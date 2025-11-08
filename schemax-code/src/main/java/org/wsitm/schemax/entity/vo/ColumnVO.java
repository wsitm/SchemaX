package org.wsitm.schemax.entity.vo;

import cn.hutool.db.meta.Column;

import java.io.Serializable;

public class ColumnVO implements Serializable {

    private static final long serialVersionUID = 1L;

    public ColumnVO() {
    }

    public ColumnVO(Column column) {
        this.tableName = column.getTableName();
        this.name = column.getName();
        this.type = column.getType();
        this.typeName = column.getTypeName();
        this.size = column.getSize();
        this.digit = column.getDigit();
        this.isNullable = column.isNullable();
        this.comment = column.getComment();
        this.autoIncrement = column.isAutoIncrement();
        this.columnDef = column.getColumnDef();
        this.isPk = column.isPk();
    }


    private String tableName;
    private String name;
    private Integer type;
    private String typeName;
    private long size;
    private Integer digit;
    private boolean isNullable;
    private String comment;
    private boolean autoIncrement;
    private String columnDef;
    private boolean isPk;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Integer getDigit() {
        return digit;
    }

    public void setDigit(Integer digit) {
        this.digit = digit;
    }

    public boolean isNullable() {
        return isNullable;
    }

    public void setNullable(boolean nullable) {
        isNullable = nullable;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isAutoIncrement() {
        return autoIncrement;
    }

    public void setAutoIncrement(boolean autoIncrement) {
        this.autoIncrement = autoIncrement;
    }

    public String getColumnDef() {
        return columnDef;
    }

    public void setColumnDef(String columnDef) {
        this.columnDef = columnDef;
    }

    public boolean isPk() {
        return isPk;
    }

    public void setPk(boolean pk) {
        isPk = pk;
    }
}
