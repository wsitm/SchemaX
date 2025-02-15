package org.wsitm.rdbms.entity.vo;

import java.io.Serializable;
import java.util.List;

public class ConvertVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 输出类型，1、DDL, 2、原始信息
     */
    private Integer inputType;

    /**
     * 输入DDL语句
     */
    private String inputDDL;

    /**
     * 表格元信息列表
     */
    private List<TableVO> tableVOList;


    /**
     * 输出类型，1、DDL, 2、原始信息
     */
    private Integer outputType;

    /**
     * 输出数据库
     */
    private String outputDatabase;

    public Integer getInputType() {
        return inputType;
    }

    public void setInputType(Integer inputType) {
        this.inputType = inputType;
    }

    public String getInputDDL() {
        return inputDDL;
    }

    public void setInputDDL(String inputDDL) {
        this.inputDDL = inputDDL;
    }


    public List<TableVO> getTableVOList() {
        return tableVOList;
    }

    public void setTableVOList(List<TableVO> tableVOList) {
        this.tableVOList = tableVOList;
    }


    public Integer getOutputType() {
        return outputType;
    }

    public void setOutputType(Integer outputType) {
        this.outputType = outputType;
    }


    public String getOutputDatabase() {
        return outputDatabase;
    }

    public void setOutputDatabase(String outputDatabase) {
        this.outputDatabase = outputDatabase;
    }

}
