package org.wsitm.schemax.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.wsitm.schemax.entity.domain.JdbcInfo;
import org.wsitm.schemax.entity.vo.JdbcInfoVo;

import java.util.List;

@Mapper
public interface JdbcInfoMapper {
    /**
     * 查询驱动管理
     *
     * @param jdbcId 驱动管理主键
     * @return 驱动管理
     */
    public JdbcInfoVo selectJdbcInfoByJdbcId(Integer jdbcId);

    /**
     * 查询驱动管理列表
     *
     * @param jdbcInfo 驱动管理
     * @return 驱动管理集合
     */
    public List<JdbcInfoVo> selectJdbcInfoList(JdbcInfo jdbcInfo);

    /**
     * 新增驱动管理
     *
     * @param jdbcInfo 驱动管理
     * @return 结果
     */
    public int insertJdbcInfo(JdbcInfo jdbcInfo);

    /**
     * 修改驱动管理
     *
     * @param jdbcInfo 驱动管理
     * @return 结果
     */
    public int updateJdbcInfo(JdbcInfo jdbcInfo);

    /**
     * 删除驱动管理
     *
     * @param jdbcId 驱动管理主键
     * @return 结果
     */
    public int deleteJdbcInfoByJdbcId(Integer jdbcId);

    /**
     * 批量删除驱动管理
     *
     * @param jdbcIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteJdbcInfoByJdbcIds(Integer[] jdbcIds);
}
