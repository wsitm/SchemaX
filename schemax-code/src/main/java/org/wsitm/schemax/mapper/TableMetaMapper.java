package org.wsitm.schemax.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.wsitm.schemax.entity.vo.TableVO;

import java.util.List;

/**
 * 表格元数据映射器接口
 *
 * @author wsitm
 * @since 2025-12-06
 */
@Mapper
public interface TableMetaMapper {

    /**
     * 插入表格元数据
     *
     * @param tableVO 表格元数据对象
     * @return 影响的行数
     */
    int insert(TableVO tableVO);

    /**
     * 更新表格元数据
     *
     * @param tableVO 表格元数据对象
     * @return 影响的行数
     */
    int update(TableVO tableVO);

    /**
     * 根据连接ID删除表格元数据
     *
     * @param connectId 连接ID
     * @return 影响的行数
     */
    int deleteByConnectId(@Param("connectId") Integer connectId);

    /**
     * 根据连接ID查询所有表格元数据
     *
     * @param connectId 连接ID
     * @return 表格元数据列表
     */
    List<TableVO> findByConnectId(@Param("connectId") Integer connectId);

    /**
     * 根据连接ID和表名查询表格元数据
     *
     * @param connectId 连接ID
     * @param tableName 表名
     * @return 表格元数据对象
     */
    TableVO findByConnectIdAndTableName(@Param("connectId") Integer connectId,
                                        @Param("tableName") String tableName);

}

