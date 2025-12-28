package org.wsitm.schemax.service;

import org.wsitm.schemax.entity.domain.TemplateInfo;
import org.wsitm.schemax.entity.vo.TemplateInfoVO;

import java.util.List;

public interface ITemplateInfoService {
    TemplateInfoVO selectTemplateInfoByTpId(Integer tpId);

    List<TemplateInfoVO> selectTemplateInfoList(TemplateInfo templateInfo);

    int insertTemplateInfo(TemplateInfo templateInfo);

    int updateTemplateInfo(TemplateInfo templateInfo);

    int deleteTemplateInfoByTpIds(Integer[] tpIds);

    int deleteTemplateInfoByTpId(Integer tpId);
}

