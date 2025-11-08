package org.wsitm.schemax.entity.vo;

import java.io.Serializable;

/**
 * DDL转换其余信息
 *
 * @author wsitm
 * @date 2025-02-11
 */
public class ExtendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    public ExtendVO() {
    }

    public static ExtendVO withDropTable(String dropTable) {
        ExtendVO extendVO = new ExtendVO();
        extendVO.setDropTable(dropTable);
        return extendVO;
    }

    public static ExtendVO withAbnormalDDL(String abnormalDDL) {
        ExtendVO extendVO = new ExtendVO();
        extendVO.setAbnormalDDL(abnormalDDL);
        return extendVO;
    }

    public static ExtendVO withSourceSQL(String sourceSQL) {
        ExtendVO extendVO = new ExtendVO();
        extendVO.setSourceSQL(sourceSQL);
        return extendVO;
    }


    /**
     * 移除表
     */
    private String dropTable;

    /**
     * 异常语句，用于承接解析异常的DDL
     */
    private String abnormalDDL;

    /**
     * 原始语句，解析正常，但可能不是DDL语句
     */
    private String sourceSQL;

    public String getDropTable() {
        return dropTable;
    }

    public void setDropTable(String dropTable) {
        this.dropTable = dropTable;
    }

    public String getAbnormalDDL() {
        return abnormalDDL;
    }

    public void setAbnormalDDL(String abnormalDDL) {
        this.abnormalDDL = abnormalDDL;
    }

    public String getSourceSQL() {
        return sourceSQL;
    }

    public void setSourceSQL(String sourceSQL) {
        this.sourceSQL = sourceSQL;
    }
}
