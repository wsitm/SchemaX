package org.wsitm.schemax.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.wsitm.schemax.entity.domain.JdbcInfo;
import org.wsitm.schemax.entity.vo.JdbcInfoVo;

import java.util.List;

@Mapper
public interface JdbcInfoMapper {

    List<JdbcInfo> findAll();

    JdbcInfo findById(String jdbcId);

    int insert(JdbcInfo jdbcInfo);

    int update(JdbcInfo jdbcInfo);

    int deleteById(String jdbcId);

    int deleteByIds(@Param("ids") String[] ids);

    /**
     * 查询驱动管理
     *
     * @param jdbcId 驱动管理主键
     * @return 驱动管理
     */
    JdbcInfoVo selectJdbcInfoByJdbcId(String jdbcId);

    /**
     * 查询驱动管理列表
     *
     * @param jdbcInfo 驱动管理
     * @return 驱动管理集合
     */
    List<JdbcInfoVo> selectJdbcInfoList(JdbcInfo jdbcInfo);

}
