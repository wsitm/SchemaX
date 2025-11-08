package org.wsitm.schemax.entity.vo;

import cn.hutool.db.meta.Table;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 表信息
 *
 * @author wsitm
 * @date 2025-01-11
 */
public class TableVO implements Serializable {

    private static final long serialVersionUID = 1L;

    public TableVO() {
    }

    public TableVO(String tableName) {
        this.tableName = tableName;
    }

    public TableVO(Table table) {
        this.schema = table.getSchema();
        this.catalog = table.getCatalog();
        this.tableName = table.getTableName();
        this.comment = table.getComment();
        this.columnList = table.getColumns().stream()
                .map(ColumnVO::new)
                .collect(Collectors.toList());
        this.indexList = table.getIndexInfoList().stream()
                .map(IndexVO::new)
                .collect(Collectors.toList());
    }

    public TableVO(String schema, String catalog, String tableName, String comment, Integer numRows, List<ColumnVO> columnList, List<IndexVO> indexList) {
        this.schema = schema;
        this.catalog = catalog;
        this.tableName = tableName;
        this.comment = comment;
        this.numRows = numRows;
        this.columnList = columnList;
        this.indexList = indexList;
    }

    private String schema;
    private String catalog;
    private String tableName;
    private String comment;
    private Integer numRows;

    private List<ColumnVO> columnList;

    private List<IndexVO> indexList;

    /**
     * DDL转换其余信息
     */
    private ExtendVO extend;

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

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }


    public Integer getNumRows() {
        return numRows;
    }

    public void setNumRows(Integer numRows) {
        this.numRows = numRows;
    }

    public List<ColumnVO> getColumnList() {
        return columnList;
    }

    public void setColumnList(List<ColumnVO> columnList) {
        this.columnList = columnList;
    }

    public List<IndexVO> getIndexList() {
        return indexList;
    }

    public void setIndexList(List<IndexVO> indexList) {
        this.indexList = indexList;
    }

    public ExtendVO getExtend() {
        return extend;
    }

    public void setExtend(ExtendVO extend) {
        this.extend = extend;
    }
}
