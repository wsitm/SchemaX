package org.wsitm.schemax.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.wsitm.schemax.entity.domain.TypeMappingRule;

import java.util.List;

@Mapper
public interface TypeMappingRuleMapper {
    TypeMappingRule selectTypeMappingRuleByRuleId(Integer ruleId);

    List<TypeMappingRule> selectTypeMappingRuleList(TypeMappingRule rule);

    List<TypeMappingRule> selectMatchRules(@Param("sourceDatabase") String sourceDatabase,
                                           @Param("targetDatabase") String targetDatabase,
                                           @Param("sourceType") String sourceType);

    int insertTypeMappingRule(TypeMappingRule rule);

    int updateTypeMappingRule(TypeMappingRule rule);

    int deleteTypeMappingRuleByRuleId(Integer ruleId);

    int deleteTypeMappingRuleByRuleIds(Integer[] ruleIds);
}
