package org.wsitm.schemax.entity.domain;

import java.io.Serializable;

/**
 * 连接模板关联对象 dim_connect_template_link
 */
public class ConnectTemplateLink implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer connectId;
    private Integer tpId;
    /**
     * 是否默认，1=是，0=否
     */
    private Integer isDef;

    public Integer getConnectId() {
        return connectId;
    }

    public void setConnectId(Integer connectId) {
        this.connectId = connectId;
    }

    public Integer getTpId() {
        return tpId;
    }

    public void setTpId(Integer tpId) {
        this.tpId = tpId;
    }

    public Integer getIsDef() {
        return isDef;
    }

    public void setIsDef(Integer isDef) {
        this.isDef = isDef;
    }
}
