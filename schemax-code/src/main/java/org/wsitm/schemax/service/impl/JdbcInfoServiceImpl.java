package org.wsitm.schemax.service.impl;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.wsitm.schemax.entity.domain.JdbcInfo;
import org.wsitm.schemax.entity.vo.JdbcInfoVo;
import org.wsitm.schemax.exception.ServiceException;
import org.wsitm.schemax.mapper.JdbcInfoMapper;
import org.wsitm.schemax.service.IJdbcInfoService;
import org.wsitm.schemax.utils.RdbmsUtil;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 驱动管理Service业务层处理
 *
 * @author wsitm
 * @date 2025-01-11
 */
@Service
public class JdbcInfoServiceImpl implements IJdbcInfoService {

    @Autowired
    private JdbcInfoMapper jdbcInfoMapper;

    /**
     * 查询驱动管理
     *
     * @param jdbcId 驱动管理主键
     * @return 驱动管理
     */
    @Override
    public JdbcInfo selectJdbcInfoByJdbcId(Integer jdbcId) {
        return jdbcInfoMapper.selectJdbcInfoByJdbcId(jdbcId);
    }

    /**
     * 查询驱动管理列表
     *
     * @return 驱动管理
     */
    @Override
    public List<JdbcInfoVo> selectJdbcInfoList(String jdbcName) {
        JdbcInfo jdbcInfo = new JdbcInfo();
        jdbcInfo.setJdbcName(jdbcName);
        List<JdbcInfoVo> jdbcInfoVoList = jdbcInfoMapper.selectJdbcInfoList(jdbcInfo);
        for (JdbcInfoVo jdbcInfoVo : jdbcInfoVoList) {
            jdbcInfoVo.setIsLoaded(RdbmsUtil.isLoadJdbcJar(jdbcInfoVo.getJdbcId()));
        }
        return jdbcInfoVoList;
    }

    /**
     * 新增驱动管理
     *
     * @param jdbcInfo 驱动管理
     * @return 结果
     */
    @Override
    public int insertJdbcInfo(JdbcInfo jdbcInfo) {
//        jdbcInfo.setJdbcId(IdUtil.getSnowflakeNextIdStr());
        jdbcInfo.setCreateTime(LocalDateTime.now());
        int insert = jdbcInfoMapper.insertJdbcInfo(jdbcInfo);
        RdbmsUtil.loadJdbcJar(jdbcInfo);
        return insert;
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
        RdbmsUtil.loadJdbcJar(jdbcInfo);
        return jdbcInfoMapper.updateJdbcInfo(jdbcInfo);
    }

    /**
     * 批量删除驱动管理
     *
     * @param jdbcIds 需要删除的驱动管理主键
     * @return 结果
     */
    @Override
    public int deleteJdbcInfoByJdbcIds(Integer[] jdbcIds) {
        return jdbcInfoMapper.deleteJdbcInfoByJdbcIds(jdbcIds);
    }

    /**
     * 删除驱动管理信息
     *
     * @param jdbcId 驱动管理主键
     * @return 结果
     */
    @Override
    public int deleteJdbcInfoByJdbcId(Integer jdbcId) {
        return jdbcInfoMapper.deleteJdbcInfoByJdbcId(jdbcId);
    }


    /**
     * 安装或卸载驱动
     *
     * @param jdbcId 驱动ID
     * @param action load/unload 安装/卸载
     * @return 结果
     */
    @Override
    public int load(Integer jdbcId, String action) {
        JdbcInfo jdbcInfo = jdbcInfoMapper.selectJdbcInfoByJdbcId(jdbcId);
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
