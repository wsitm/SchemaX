package org.wsitm.schemax.entity.vo;

import java.io.Serializable;

public class MetaSnapshotCreateVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String snapshotName;
    private String remark;

    public String getSnapshotName() {
        return snapshotName;
    }

    public void setSnapshotName(String snapshotName) {
        this.snapshotName = snapshotName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
