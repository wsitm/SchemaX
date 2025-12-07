package org.wsitm.schemax.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.wsitm.schemax.entity.vo.TableVO;

import java.util.List;

@Mapper
public interface TableMetaMapper {

    List<TableVO> findByConnectId(@Param("connectId") Integer connectId);

    TableVO findByConnectIdAndTableName(@Param("connectId") Integer connectId, @Param("tableName") String tableName);

    int insert(TableVO tableVO);

    int update(TableVO tableVO);

    int deleteByConnectId(String connectId);
}
