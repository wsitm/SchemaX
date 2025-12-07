package org.wsitm.schemax.entity.vo;

import cn.hutool.db.meta.Table;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 表信息 VO 类
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

    private Long id;
    private Integer connectId;
    private String schema;
    private String catalog;
    private String tableName;
    private String comment;
    private Integer numRows;

    private List<ColumnVO> columnList;
    private String columnListJson;

    private List<IndexVO> indexList;
    private String indexListJson;

    /**
     * DDL转换其余信息
     */
    private ExtendVO extend;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getConnectId() {
        return connectId;
    }

    public void setConnectId(Integer connectId) {
        this.connectId = connectId;
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
        if (columnList == null && columnListJson != null) {
            columnList = JSON.parseObject(columnListJson, new TypeReference<List<ColumnVO>>() {
            });
        }
        return columnList;
    }

    public void setColumnList(List<ColumnVO> columnList) {
        this.columnList = columnList;
        this.columnListJson = JSON.toJSONString(columnList);
    }

    public String getColumnListJson() {
        if (columnListJson == null && columnList != null) {
            columnListJson = JSON.toJSONString(columnList);
        }
        return columnListJson;
    }

    public void setColumnListJson(String columnListJson) {
        this.columnListJson = columnListJson;
        this.columnList = JSON.parseObject(columnListJson, new TypeReference<List<ColumnVO>>() {
        });
    }

    public List<IndexVO> getIndexList() {
        if (indexList == null && indexListJson != null) {
            indexList = JSON.parseObject(indexListJson, new TypeReference<List<IndexVO>>() {
            });
        }
        return indexList;
    }

    public void setIndexList(List<IndexVO> indexList) {
        this.indexList = indexList;
        this.indexListJson = JSON.toJSONString(indexList);
    }

    public String getIndexListJson() {
        if (indexListJson == null && indexList != null) {
            indexListJson = JSON.toJSONString(indexList);
        }
        return indexListJson;
    }

    public void setIndexListJson(String indexListJson) {
        this.indexListJson = indexListJson;
        this.indexList = JSON.parseObject(indexListJson, new TypeReference<List<IndexVO>>() {
        });
    }

    public ExtendVO getExtend() {
        return extend;
    }

    public void setExtend(ExtendVO extend) {
        this.extend = extend;
    }
}
