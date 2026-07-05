package org.wsitm.schemax.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.wsitm.schemax.entity.domain.MetaSnapshot;
import org.wsitm.schemax.entity.vo.TableVO;

import java.util.List;

@Mapper
public interface MetaSnapshotMapper {
    MetaSnapshot selectBySnapshotId(Long snapshotId);

    List<MetaSnapshot> selectList(MetaSnapshot snapshot);

    int insertSnapshot(MetaSnapshot snapshot);

    int insertSnapshotTable(@Param("snapshotId") Long snapshotId, @Param("table") TableVO tableVO);

    List<TableVO> selectTablesBySnapshotId(Long snapshotId);

    int deleteSnapshotByIds(Long[] snapshotIds);

    int deleteSnapshotTablesBySnapshotIds(Long[] snapshotIds);

    int deleteSnapshotByConnectIds(Integer[] connectIds);

    int deleteSnapshotTablesByConnectIds(Integer[] connectIds);
}
