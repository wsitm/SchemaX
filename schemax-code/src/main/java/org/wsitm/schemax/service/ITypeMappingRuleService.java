package org.wsitm.schemax.service;

import org.wsitm.schemax.entity.domain.TypeMappingRule;
import org.wsitm.schemax.entity.vo.ColumnVO;
import org.wsitm.schemax.entity.vo.TypeMappingResult;
import org.wsitm.schemax.entity.vo.TypeMappingTestVO;

import java.util.List;

public interface ITypeMappingRuleService {
    TypeMappingRule selectTypeMappingRuleByRuleId(Integer ruleId);

    List<TypeMappingRule> selectTypeMappingRuleList(TypeMappingRule rule);

    int insertTypeMappingRule(TypeMappingRule rule);

    int updateTypeMappingRule(TypeMappingRule rule);

    int deleteTypeMappingRuleByRuleIds(Integer[] ruleIds);

    TypeMappingResult map(ColumnVO columnVO, String sourceDatabase, String targetDatabase);

    TypeMappingResult test(TypeMappingTestVO testVO);
}
