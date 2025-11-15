package org.wsitm.schemax.service;

import org.wsitm.schemax.entity.domain.JdbcInfo;
import org.wsitm.schemax.entity.vo.JdbcInfoVo;

import java.util.List;

/**
 * 驱动管理Service接口
 *
 * @author wsitm
 * @date 2025-01-11
 */
public interface IJdbcInfoService {
    /**
     * 查询驱动管理
     *
     * @param jdbcId 驱动管理主键
     * @return 驱动管理
     */
    public JdbcInfo selectJdbcInfoByJdbcId(String jdbcId);

    /**
     * 查询驱动管理列表
     *
     * @return 驱动管理集合
     */
    public List<JdbcInfoVo> selectJdbcInfoList(String jdbcName);

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
     * 批量删除驱动管理
     *
     * @param jdbcIds 需要删除的驱动管理主键集合
     * @return 结果
     */
    public int deleteJdbcInfoByJdbcIds(String[] jdbcIds);

    /**
     * 删除驱动管理信息
     *
     * @param jdbcId 驱动管理主键
     * @return 结果
     */
    public int deleteJdbcInfoByJdbcId(String jdbcId);

    /**
     * 安装或卸载驱动
     *
     * @param jdbcId 驱动ID
     * @param action load/unload 安装/卸载
     * @return 结果
     */
    int load(String jdbcId, String action);
}
