package org.wsitm.schemax.entity.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

public class TypeMappingRule implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer ruleId;
    private String ruleName;
    private String sourceDatabase;
    private String targetDatabase;
    private String sourceType;
    private String targetType;
    private String lengthStrategy;
    private Integer lengthValue;
    private String precisionStrategy;
    private Integer precisionValue;
    private String scaleStrategy;
    private Integer scaleValue;
    private Integer priority;
    private Integer enabled;
    private Integer builtin;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

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

    public String getSourceDatabase() {
        return sourceDatabase;
    }

    public void setSourceDatabase(String sourceDatabase) {
        this.sourceDatabase = sourceDatabase;
    }

    public String getTargetDatabase() {
        return targetDatabase;
    }

    public void setTargetDatabase(String targetDatabase) {
        this.targetDatabase = targetDatabase;
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

    public String getLengthStrategy() {
        return lengthStrategy;
    }

    public void setLengthStrategy(String lengthStrategy) {
        this.lengthStrategy = lengthStrategy;
    }

    public Integer getLengthValue() {
        return lengthValue;
    }

    public void setLengthValue(Integer lengthValue) {
        this.lengthValue = lengthValue;
    }

    public String getPrecisionStrategy() {
        return precisionStrategy;
    }

    public void setPrecisionStrategy(String precisionStrategy) {
        this.precisionStrategy = precisionStrategy;
    }

    public Integer getPrecisionValue() {
        return precisionValue;
    }

    public void setPrecisionValue(Integer precisionValue) {
        this.precisionValue = precisionValue;
    }

    public String getScaleStrategy() {
        return scaleStrategy;
    }

    public void setScaleStrategy(String scaleStrategy) {
        this.scaleStrategy = scaleStrategy;
    }

    public Integer getScaleValue() {
        return scaleValue;
    }

    public void setScaleValue(Integer scaleValue) {
        this.scaleValue = scaleValue;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }

    public Integer getBuiltin() {
        return builtin;
    }

    public void setBuiltin(Integer builtin) {
        this.builtin = builtin;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
