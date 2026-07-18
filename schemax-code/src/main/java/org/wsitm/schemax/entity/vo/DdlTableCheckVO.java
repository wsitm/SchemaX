package org.wsitm.schemax.entity.vo;

import java.io.Serializable;

public class DdlTableCheckVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tableName;
    private Integer statementCount;
    private Integer successCount;
    private Integer errorCount;
    private Integer warningCount;
    private Integer successRate;
    private String status;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Integer getStatementCount() {
        return statementCount;
    }

    public void setStatementCount(Integer statementCount) {
        this.statementCount = statementCount;
    }

    public Integer getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Integer successCount) {
        this.successCount = successCount;
    }

    public Integer getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(Integer errorCount) {
        this.errorCount = errorCount;
    }

    public Integer getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(Integer warningCount) {
        this.warningCount = warningCount;
    }

    public Integer getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(Integer successRate) {
        this.successRate = successRate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
