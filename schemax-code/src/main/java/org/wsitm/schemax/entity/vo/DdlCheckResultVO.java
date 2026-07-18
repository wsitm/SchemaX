package org.wsitm.schemax.entity.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DdlCheckResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean valid;
    private Integer statementCount;
    private Integer successCount;
    private Integer tableCount;
    private Integer errorCount;
    private Integer warningCount;
    private Integer infoCount;
    private Integer successRate;
    private List<DdlIssueVO> issues = new ArrayList<>();
    private List<DdlStatementCheckVO> statements = new ArrayList<>();
    private List<DdlTableCheckVO> tableStatistics = new ArrayList<>();

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
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

    public Integer getTableCount() {
        return tableCount;
    }

    public void setTableCount(Integer tableCount) {
        this.tableCount = tableCount;
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

    public Integer getInfoCount() {
        return infoCount;
    }

    public void setInfoCount(Integer infoCount) {
        this.infoCount = infoCount;
    }

    public Integer getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(Integer successRate) {
        this.successRate = successRate;
    }

    public List<DdlIssueVO> getIssues() {
        return issues;
    }

    public void setIssues(List<DdlIssueVO> issues) {
        this.issues = issues;
    }

    public List<DdlStatementCheckVO> getStatements() {
        return statements;
    }

    public void setStatements(List<DdlStatementCheckVO> statements) {
        this.statements = statements;
    }

    public List<DdlTableCheckVO> getTableStatistics() {
        return tableStatistics;
    }

    public void setTableStatistics(List<DdlTableCheckVO> tableStatistics) {
        this.tableStatistics = tableStatistics;
    }
}
