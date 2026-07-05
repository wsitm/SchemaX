package org.wsitm.schemax.entity.vo;

import java.io.Serializable;

public class TypeMappingResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean matched;
    private Integer ruleId;
    private String ruleName;
    private String sourceType;
    private String targetType;
    private Integer length;
    private Integer precision;
    private Integer scale;
    private String typeExpression;

    public static TypeMappingResult unmatched() {
        TypeMappingResult result = new TypeMappingResult();
        result.setMatched(false);
        return result;
    }

    public boolean isMatched() {
        return matched;
    }

    public void setMatched(boolean matched) {
        this.matched = matched;
    }

    public Integer getRuleId() {
        return ruleId;
    }

    public void setRuleId(Integer ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Integer getLength() {
        return length;
    }

    public void setLength(Integer length) {
        this.length = length;
    }

    public Integer getPrecision() {
        return precision;
    }

    public void setPrecision(Integer precision) {
        this.precision = precision;
    }

    public Integer getScale() {
        return scale;
    }

    public void setScale(Integer scale) {
        this.scale = scale;
    }

    public String getTypeExpression() {
        return typeExpression;
    }

    public void setTypeExpression(String typeExpression) {
        this.typeExpression = typeExpression;
    }
}
