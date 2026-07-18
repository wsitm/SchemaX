package org.wsitm.schemax.entity.vo;

import java.io.Serializable;

public class DdlCheckRequestVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String inputDDL;
    private String sourceDatabase;
    private String outputDatabase;

    public String getInputDDL() {
        return inputDDL;
    }

    public void setInputDDL(String inputDDL) {
        this.inputDDL = inputDDL;
    }

    public String getSourceDatabase() {
        return sourceDatabase;
    }

    public void setSourceDatabase(String sourceDatabase) {
        this.sourceDatabase = sourceDatabase;
    }

    public String getOutputDatabase() {
        return outputDatabase;
    }

    public void setOutputDatabase(String outputDatabase) {
        this.outputDatabase = outputDatabase;
    }
}
