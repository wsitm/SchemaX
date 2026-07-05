package org.wsitm.schemax.service;

import org.wsitm.schemax.entity.domain.MetaSnapshot;
import org.wsitm.schemax.entity.vo.ConnectInfoVO;
import org.wsitm.schemax.entity.vo.TableVO;

import java.util.List;

public interface IMetaSnapshotService {
    List<MetaSnapshot> selectSnapshotList(MetaSnapshot snapshot);

    MetaSnapshot selectSnapshotById(Long snapshotId);

    List<TableVO> selectSnapshotTableList(Long snapshotId);

    MetaSnapshot createSnapshot(Integer connectId, String snapshotName, String remark);

    MetaSnapshot createAutoSnapshot(ConnectInfoVO connectInfoVO);

    int deleteSnapshotByIds(Long[] snapshotIds);

    int deleteSnapshotByConnectIds(Integer[] connectIds);
}
