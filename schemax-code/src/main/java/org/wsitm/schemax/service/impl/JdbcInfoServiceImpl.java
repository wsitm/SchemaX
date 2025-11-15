package org.wsitm.schemax.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;
import org.wsitm.schemax.entity.domain.JdbcInfo;
import org.wsitm.schemax.entity.vo.JdbcInfoVo;
import org.wsitm.schemax.exception.ServiceException;
import org.wsitm.schemax.service.IJdbcInfoService;
import org.wsitm.schemax.utils.CacheUtil;
import org.wsitm.schemax.utils.RdbmsUtil;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 驱动管理Service业务层处理
 *
 * @author wsitm
 * @date 2025-01-11
 */
@Service
public class JdbcInfoServiceImpl implements IJdbcInfoService {


    /**
     * 查询驱动管理
     *
     * @param jdbcId 驱动管理主键
     * @return 驱动管理
     */
    @Override
    public JdbcInfo selectJdbcInfoByJdbcId(String jdbcId) {
        return CacheUtil.getJdbcInfo(jdbcId);
    }

    /**
     * 查询驱动管理列表
     *
     * @return 驱动管理
     */
    @Override
    public List<JdbcInfoVo> selectJdbcInfoList(String jdbcName) {
        List<JdbcInfo> jdbcInfoList = CacheUtil.getJdbcInfoList();
        if (CollUtil.isEmpty(jdbcInfoList)) {
            return Collections.emptyList();
        }
        return jdbcInfoList.stream()
                .filter(jdbcInfo -> StrUtil.isEmpty(jdbcName) || jdbcInfo.getJdbcName().contains(jdbcName))
                .map(jdbcInfo -> {
                    JdbcInfoVo jdbcInfoVo = new JdbcInfoVo();
                    BeanUtil.copyProperties(jdbcInfo, jdbcInfoVo);
                    jdbcInfoVo.setIsLoaded(RdbmsUtil.isLoadJdbcJar(jdbcInfo.getJdbcId()));
                    return jdbcInfoVo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 新增驱动管理
     *
     * @param jdbcInfo 驱动管理
     * @return 结果
     */
    @Override
    public int insertJdbcInfo(JdbcInfo jdbcInfo) {
        jdbcInfo.setJdbcId(IdUtil.getSnowflakeNextIdStr());
        jdbcInfo.setCreateTime(LocalDateTime.now());
        CacheUtil.saveItemToJdbcInfo(jdbcInfo);
        RdbmsUtil.loadJdbcJar(jdbcInfo);
        return 1;
    }

    /**
     * 修改驱动管理
     *
     * @param jdbcInfo 驱动管理
     * @return 结果
     */
    @Override
    public int updateJdbcInfo(JdbcInfo jdbcInfo) {
        if (jdbcInfo.getJdbcId() == null) {
            throw new ServiceException("驱动ID不能为空");
        }
        CacheUtil.saveItemToJdbcInfo(jdbcInfo);
        RdbmsUtil.loadJdbcJar(jdbcInfo);
        return 1;
    }

    /**
     * 批量删除驱动管理
     *
     * @param jdbcIds 需要删除的驱动管理主键
     * @return 结果
     */
    @Override
    public int deleteJdbcInfoByJdbcIds(String[] jdbcIds) {
        CacheUtil.removeJdbcInfoByIds(jdbcIds);
        return 1;
    }

    /**
     * 删除驱动管理信息
     *
     * @param jdbcId 驱动管理主键
     * @return 结果
     */
    @Override
    public int deleteJdbcInfoByJdbcId(String jdbcId) {
        CacheUtil.removeJdbcInfoByIds(new String[]{jdbcId});
        return 1;
    }


    /**
     * 安装或卸载驱动
     *
     * @param jdbcId 驱动ID
     * @param action load/unload 安装/卸载
     * @return 结果
     */
    @Override
    public int load(String jdbcId, String action) {
        JdbcInfo jdbcInfo = CacheUtil.getJdbcInfo(jdbcId);
        switch (action) {
            case "load":
                RdbmsUtil.loadJdbcJar(jdbcInfo);
                break;
            case "unload":
                RdbmsUtil.unloadJdbcJar(jdbcInfo);
                break;
        }
        return 1;
    }
}
