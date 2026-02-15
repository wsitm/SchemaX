package org.wsitm.schemax.service.impl;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.wsitm.schemax.entity.domain.TemplateInfo;
import org.wsitm.schemax.entity.vo.TemplateInfoVO;
import org.wsitm.schemax.exception.ServiceException;
import org.wsitm.schemax.mapper.ConnectTemplateLinkMapper;
import org.wsitm.schemax.mapper.TemplateInfoMapper;
import org.wsitm.schemax.service.ITemplateInfoService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 模板管理Service业务层处理
 *
 * @author wsitm
 * @date 2025-12-27
 */
@Service
public class TemplateInfoServiceImpl implements ITemplateInfoService {

    @Autowired
    private TemplateInfoMapper templateInfoMapper;
    @Autowired
    private ConnectTemplateLinkMapper connectTemplateLinkMapper;

    @Override
    public TemplateInfoVO selectTemplateInfoByTpId(Integer tpId) {
        return templateInfoMapper.selectTemplateInfoByTpId(tpId);
    }

    @Override
    public List<TemplateInfoVO> selectTemplateInfoList(TemplateInfo templateInfo) {
        return templateInfoMapper.selectTemplateInfoList(templateInfo);
    }

    @Override
    public int insertTemplateInfo(TemplateInfo templateInfo) {
        checkTemplate(templateInfo, true);
        templateInfo.setCreateTime(LocalDateTime.now());
        return templateInfoMapper.insertTemplateInfo(templateInfo);
    }

    @Override
    public int updateTemplateInfo(TemplateInfo templateInfo) {
        if (templateInfo.getTpId() == null) {
            throw new ServiceException("模板ID不能为空");
        }
        checkTemplate(templateInfo, false);
        return templateInfoMapper.updateTemplateInfo(templateInfo);
    }

    private void checkTemplate(TemplateInfo templateInfo, boolean isAdd) {
        if (StrUtil.isBlank(templateInfo.getTpName())) {
            throw new ServiceException("模板名称不能为空");
        }
        if (templateInfo.getTpType() == null) {
            throw new ServiceException("模板类型不能为空");
        }
        if (!StrUtil.equalsAny(String.valueOf(templateInfo.getTpType()), "1", "2", "3")) {
            throw new ServiceException("模板类型不合法");
        }
        if (templateInfo.getTpContent() == null) {
            throw new ServiceException("模板内容不能为空");
        }
    }

    @Override
    public int deleteTemplateInfoByTpIds(Integer[] tpIds) {
        connectTemplateLinkMapper.deleteByTpIds(tpIds);
        return templateInfoMapper.deleteTemplateInfoByTpIds(tpIds);
    }

    @Override
    public int deleteTemplateInfoByTpId(Integer tpId) {
        connectTemplateLinkMapper.deleteByTpId(tpId);
        return templateInfoMapper.deleteTemplateInfoByTpId(tpId);
    }
}

