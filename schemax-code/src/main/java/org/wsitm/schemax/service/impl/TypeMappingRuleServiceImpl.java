package org.wsitm.schemax.service.impl;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.wsitm.schemax.entity.domain.TypeMappingRule;
import org.wsitm.schemax.entity.vo.ColumnVO;
import org.wsitm.schemax.entity.vo.TypeMappingResult;
import org.wsitm.schemax.entity.vo.TypeMappingTestVO;
import org.wsitm.schemax.exception.ServiceException;
import org.wsitm.schemax.mapper.TypeMappingRuleMapper;
import org.wsitm.schemax.service.ITypeMappingRuleService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class TypeMappingRuleServiceImpl implements ITypeMappingRuleService {
    public static final String WILDCARD = "*";
    public static final String KEEP = "KEEP";
    public static final String DROP = "DROP";
    public static final String FIXED = "FIXED";
    public static final String LIMIT_MAX = "LIMIT_MAX";

    @Autowired
    private TypeMappingRuleMapper typeMappingRuleMapper;

    @Override
    public TypeMappingRule selectTypeMappingRuleByRuleId(Integer ruleId) {
        return typeMappingRuleMapper.selectTypeMappingRuleByRuleId(ruleId);
    }

    @Override
    public List<TypeMappingRule> selectTypeMappingRuleList(TypeMappingRule rule) {
        return typeMappingRuleMapper.selectTypeMappingRuleList(rule);
    }

    @Override
    public int insertTypeMappingRule(TypeMappingRule rule) {
        checkRule(rule, true);
        fillDefaults(rule);
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());
        return typeMappingRuleMapper.insertTypeMappingRule(rule);
    }

    @Override
    public int updateTypeMappingRule(TypeMappingRule rule) {
        if (rule.getRuleId() == null) {
            throw new ServiceException("规则ID不能为空");
        }
        checkRule(rule, false);
        fillDefaults(rule);
        rule.setUpdateTime(LocalDateTime.now());
        return typeMappingRuleMapper.updateTypeMappingRule(rule);
    }

    @Override
    public int deleteTypeMappingRuleByRuleIds(Integer[] ruleIds) {
        return typeMappingRuleMapper.deleteTypeMappingRuleByRuleIds(ruleIds);
    }

    @Override
    public TypeMappingResult map(ColumnVO columnVO, String sourceDatabase, String targetDatabase) {
        if (columnVO == null || StrUtil.isBlank(columnVO.getTypeName()) || StrUtil.isBlank(targetDatabase)) {
            return TypeMappingResult.unmatched();
        }
        String sourceType = normalizeType(columnVO.getTypeName());
        List<TypeMappingRule> rules = typeMappingRuleMapper.selectMatchRules(
                normalizeDatabase(sourceDatabase),
                normalizeDatabase(targetDatabase),
                sourceType
        );
        if (rules == null || rules.isEmpty()) {
            return TypeMappingResult.unmatched();
        }
        return buildResult(rules.get(0), sourceType, columnVO.getSize(), columnVO.getDigit());
    }

    @Override
    public TypeMappingResult test(TypeMappingTestVO testVO) {
        ColumnVO columnVO = new ColumnVO();
        columnVO.setTypeName(testVO.getSourceType());
        columnVO.setSize(testVO.getLength() == null ? 0 : testVO.getLength());
        columnVO.setDigit(testVO.getScale());
        return map(columnVO, testVO.getSourceDatabase(), testVO.getTargetDatabase());
    }

    private void checkRule(TypeMappingRule rule, boolean add) {
        if (StrUtil.isBlank(rule.getRuleName())) {
            throw new ServiceException("规则名称不能为空");
        }
        if (StrUtil.isBlank(rule.getSourceDatabase())) {
            throw new ServiceException("源数据库不能为空");
        }
        if (StrUtil.isBlank(rule.getTargetDatabase())) {
            throw new ServiceException("目标数据库不能为空");
        }
        if (StrUtil.isBlank(rule.getSourceType())) {
            throw new ServiceException("源字段类型不能为空");
        }
        if (StrUtil.isBlank(rule.getTargetType())) {
            throw new ServiceException("目标字段类型不能为空");
        }
        checkStrategy(rule.getLengthStrategy(), "长度策略");
        checkStrategy(rule.getPrecisionStrategy(), "精度策略");
        checkStrategy(rule.getScaleStrategy(), "小数位策略");
    }

    private void checkStrategy(String strategy, String name) {
        if (StrUtil.isBlank(strategy)) {
            return;
        }
        if (!StrUtil.equalsAny(strategy, KEEP, DROP, FIXED, LIMIT_MAX)) {
            throw new ServiceException(name + "不合法");
        }
    }

    private void fillDefaults(TypeMappingRule rule) {
        rule.setSourceDatabase(normalizeDatabase(rule.getSourceDatabase()));
        rule.setTargetDatabase(normalizeDatabase(rule.getTargetDatabase()));
        rule.setSourceType(normalizeType(rule.getSourceType()));
        rule.setTargetType(rule.getTargetType().trim());
        if (StrUtil.isBlank(rule.getLengthStrategy())) {
            rule.setLengthStrategy(KEEP);
        }
        if (StrUtil.isBlank(rule.getPrecisionStrategy())) {
            rule.setPrecisionStrategy(KEEP);
        }
        if (StrUtil.isBlank(rule.getScaleStrategy())) {
            rule.setScaleStrategy(KEEP);
        }
        if (rule.getPriority() == null) {
            rule.setPriority(100);
        }
        if (rule.getEnabled() == null) {
            rule.setEnabled(1);
        }
        if (rule.getBuiltin() == null) {
            rule.setBuiltin(0);
        }
    }

    private TypeMappingResult buildResult(TypeMappingRule rule, String sourceType, long sourceLength, Integer sourceScale) {
        Integer length = applyStrategy(rule.getLengthStrategy(), sourceLength > 0 ? (int) sourceLength : null, rule.getLengthValue());
        Integer precision = applyStrategy(rule.getPrecisionStrategy(), sourceLength > 0 ? (int) sourceLength : null, rule.getPrecisionValue());
        Integer scale = applyStrategy(rule.getScaleStrategy(), sourceScale, rule.getScaleValue());

        TypeMappingResult result = new TypeMappingResult();
        result.setMatched(true);
        result.setRuleId(rule.getRuleId());
        result.setRuleName(rule.getRuleName());
        result.setSourceType(sourceType);
        result.setTargetType(rule.getTargetType());
        result.setLength(length);
        result.setPrecision(precision);
        result.setScale(scale);
        result.setTypeExpression(buildTypeExpression(rule.getTargetType(), length, precision, scale));
        return result;
    }

    private Integer applyStrategy(String strategy, Integer sourceValue, Integer ruleValue) {
        if (StrUtil.equals(strategy, DROP)) {
            return null;
        }
        if (StrUtil.equals(strategy, FIXED)) {
            return ruleValue;
        }
        if (StrUtil.equals(strategy, LIMIT_MAX)) {
            if (sourceValue == null) {
                return ruleValue;
            }
            if (ruleValue == null) {
                return sourceValue;
            }
            return Math.min(sourceValue, ruleValue);
        }
        return sourceValue;
    }

    private String buildTypeExpression(String targetType, Integer length, Integer precision, Integer scale) {
        if (precision != null && precision > 0) {
            if (scale != null && scale >= 0) {
                return targetType + "(" + precision + "," + scale + ")";
            }
            return targetType + "(" + precision + ")";
        }
        if (length != null && length > 0) {
            return targetType + "(" + length + ")";
        }
        return targetType;
    }

    private String normalizeType(String type) {
        if (StrUtil.isBlank(type)) {
            return type;
        }
        String value = type.trim();
        int idx = value.indexOf('(');
        if (idx > -1) {
            value = value.substring(0, idx);
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeDatabase(String database) {
        if (StrUtil.isBlank(database)) {
            return WILDCARD;
        }
        if (StrUtil.equals(database.trim(), WILDCARD)) {
            return WILDCARD;
        }
        return database.trim();
    }
}
