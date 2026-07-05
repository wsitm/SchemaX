package org.wsitm.schemax.service.impl;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.wsitm.schemax.entity.domain.MetaSnapshot;
import org.wsitm.schemax.entity.vo.ConnectInfoVO;
import org.wsitm.schemax.entity.vo.TableVO;
import org.wsitm.schemax.exception.ServiceException;
import org.wsitm.schemax.mapper.MetaSnapshotMapper;
import org.wsitm.schemax.mapper.TableMetaMapper;
import org.wsitm.schemax.service.IMetaSnapshotService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MetaSnapshotServiceImpl implements IMetaSnapshotService {
    private static final DateTimeFormatter SNAPSHOT_TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired
    private MetaSnapshotMapper metaSnapshotMapper;
    @Autowired
    private TableMetaMapper tableMetaMapper;

    @Override
    public List<MetaSnapshot> selectSnapshotList(MetaSnapshot snapshot) {
        return metaSnapshotMapper.selectList(snapshot);
    }

    @Override
    public MetaSnapshot selectSnapshotById(Long snapshotId) {
        return metaSnapshotMapper.selectBySnapshotId(snapshotId);
    }

    @Override
    public List<TableVO> selectSnapshotTableList(Long snapshotId) {
        return metaSnapshotMapper.selectTablesBySnapshotId(snapshotId);
    }

    @Override
    public MetaSnapshot createSnapshot(Integer connectId, String snapshotName, String remark) {
        if (connectId == null) {
            throw new ServiceException("连接ID不能为空");
        }
        List<TableVO> tableVOList = tableMetaMapper.findByConnectId(connectId);
        if (tableVOList == null || tableVOList.isEmpty()) {
            throw new ServiceException("未找到已缓存的表结构元数据，请先刷新连接缓存");
        }

        MetaSnapshot snapshot = new MetaSnapshot();
        snapshot.setConnectId(connectId);
        snapshot.setSnapshotName(StrUtil.blankToDefault(snapshotName, "快照-" + SNAPSHOT_TIME_FMT.format(LocalDateTime.now())));
        snapshot.setRemark(remark);
        snapshot.setTableCount(tableVOList.size());
        snapshot.setCreateTime(LocalDateTime.now());
        metaSnapshotMapper.insertSnapshot(snapshot);

        for (TableVO tableVO : tableVOList) {
            metaSnapshotMapper.insertSnapshotTable(snapshot.getSnapshotId(), tableVO);
        }
        return snapshot;
    }

    @Override
    public MetaSnapshot createAutoSnapshot(ConnectInfoVO connectInfoVO) {
        if (connectInfoVO == null || connectInfoVO.getConnectId() == null) {
            return null;
        }
        String name = StrUtil.blankToDefault(connectInfoVO.getConnectName(), "连接-" + connectInfoVO.getConnectId())
                + "-" + SNAPSHOT_TIME_FMT.format(LocalDateTime.now());
        return createSnapshot(connectInfoVO.getConnectId(), name, "刷新缓存后自动创建快照");
    }

    @Override
    public int deleteSnapshotByIds(Long[] snapshotIds) {
        metaSnapshotMapper.deleteSnapshotTablesBySnapshotIds(snapshotIds);
        return metaSnapshotMapper.deleteSnapshotByIds(snapshotIds);
    }

    @Override
    public int deleteSnapshotByConnectIds(Integer[] connectIds) {
        metaSnapshotMapper.deleteSnapshotTablesByConnectIds(connectIds);
        return metaSnapshotMapper.deleteSnapshotByConnectIds(connectIds);
    }
}
