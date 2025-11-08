package org.wsitm.schemax.entity.vo;

import cn.hutool.db.meta.ColumnIndexInfo;
import cn.hutool.db.meta.IndexInfo;

import java.io.Serializable;

public class IndexVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean nonUnique;
    private String indexName;
    private String tableName;
    private String schema;
    private String catalog;
    private String[] columnList;

    public IndexVO() {
    }

    public IndexVO(IndexInfo indexInfo) {
        this.nonUnique = indexInfo.isNonUnique();
        this.indexName = indexInfo.getIndexName();
        this.tableName = indexInfo.getTableName();
        this.schema = indexInfo.getSchema();
        this.catalog = indexInfo.getCatalog();
        this.columnList = indexInfo.getColumnIndexInfoList()
                .stream()
                .map(ColumnIndexInfo::getColumnName)
                .toArray(String[]::new);
    }

    public boolean isNonUnique() {
        return nonUnique;
    }

    public void setNonUnique(boolean nonUnique) {
        this.nonUnique = nonUnique;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public String[] getColumnList() {
        return columnList;
    }

    public void setColumnList(String[] columnList) {
        this.columnList = columnList;
    }
}
