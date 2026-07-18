package org.wsitm.schemax.entity.vo;

import java.io.Serializable;

public class DdlStatementCheckVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer statementIndex;
    private String statementType;
    private String tableName;
    private Integer startLine;
    private Integer endLine;
    private boolean parseSuccess;
    private boolean convertible;
    private Integer errorCount;
    private Integer warningCount;

    public Integer getStatementIndex() {
        return statementIndex;
    }

    public void setStatementIndex(Integer statementIndex) {
        this.statementIndex = statementIndex;
    }

    public String getStatementType() {
        return statementType;
    }

    public void setStatementType(String statementType) {
        this.statementType = statementType;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Integer getStartLine() {
        return startLine;
    }

    public void setStartLine(Integer startLine) {
        this.startLine = startLine;
    }

    public Integer getEndLine() {
        return endLine;
    }

    public void setEndLine(Integer endLine) {
        this.endLine = endLine;
    }

    public boolean isParseSuccess() {
        return parseSuccess;
    }

    public void setParseSuccess(boolean parseSuccess) {
        this.parseSuccess = parseSuccess;
    }

    public boolean isConvertible() {
        return convertible;
    }

    public void setConvertible(boolean convertible) {
        this.convertible = convertible;
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
}
