package org.wsitm.schemax.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.wsitm.schemax.entity.domain.TemplateInfo;
import org.wsitm.schemax.entity.vo.TemplateInfoVO;

import java.util.List;

@Mapper
public interface TemplateInfoMapper {
    /**
     * 查询模板管理
     *
     * @param tpId 模板ID
     * @return 模板管理
     */
    TemplateInfoVO selectTemplateInfoByTpId(Integer tpId);

    /**
     * 查询模板管理列表
     *
     * @param templateInfo 模板管理
     * @return 模板管理集合
     */
    List<TemplateInfoVO> selectTemplateInfoList(TemplateInfo templateInfo);

    /**
     * 新增模板管理
     *
     * @param templateInfo 模板管理
     * @return 结果
     */
    int insertTemplateInfo(TemplateInfo templateInfo);

    /**
     * 修改模板管理
     *
     * @param templateInfo 模板管理
     * @return 结果
     */
    int updateTemplateInfo(TemplateInfo templateInfo);

    /**
     * 删除模板管理
     *
     * @param tpId 模板ID
     * @return 结果
     */
    int deleteTemplateInfoByTpId(Integer tpId);

    /**
     * 批量删除模板管理
     *
     * @param tpIds 需要删除的数据主键集合
     * @return 结果
     */
    int deleteTemplateInfoByTpIds(Integer[] tpIds);
}

