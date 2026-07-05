package org.wsitm.schemax.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.wsitm.schemax.entity.core.R;
import org.wsitm.schemax.entity.core.TableDataInfo;
import org.wsitm.schemax.entity.domain.MetaSnapshot;
import org.wsitm.schemax.entity.vo.MetaSnapshotCreateVO;
import org.wsitm.schemax.entity.vo.TableVO;
import org.wsitm.schemax.service.IMetaSnapshotService;
import org.wsitm.schemax.utils.PageUtils;

import java.util.List;

@RestController
@RequestMapping("/rdbms/snapshot")
public class MetaSnapshotController {
    @Autowired
    private IMetaSnapshotService metaSnapshotService;

    @GetMapping("/list")
    public TableDataInfo<MetaSnapshot> list(MetaSnapshot snapshot) {
        PageUtils.startPage();
        return TableDataInfo.getDataTable(metaSnapshotService.selectSnapshotList(snapshot));
    }

    @GetMapping("/{snapshotId}")
    public R<MetaSnapshot> getInfo(@PathVariable("snapshotId") Long snapshotId) {
        return R.ok(metaSnapshotService.selectSnapshotById(snapshotId));
    }

    @GetMapping("/{snapshotId}/tables")
    public R<List<TableVO>> tables(@PathVariable("snapshotId") Long snapshotId) {
        return R.ok(metaSnapshotService.selectSnapshotTableList(snapshotId));
    }

    @PostMapping("/connect/{connectId}")
    public R<MetaSnapshot> create(@PathVariable("connectId") Integer connectId,
                                  @RequestBody(required = false) MetaSnapshotCreateVO createVO) {
        String snapshotName = createVO == null ? null : createVO.getSnapshotName();
        String remark = createVO == null ? null : createVO.getRemark();
        return R.ok(metaSnapshotService.createSnapshot(connectId, snapshotName, remark));
    }

    @DeleteMapping("/{snapshotIds}")
    public R<Integer> remove(@PathVariable Long[] snapshotIds) {
        return R.ok(metaSnapshotService.deleteSnapshotByIds(snapshotIds));
    }
}
