package org.wsitm.schemax.entity.vo;

import org.wsitm.schemax.entity.domain.ConnectTemplateLink;

/**
 * 连接模板关联视图对象
 */
public class ConnectTemplateLinkVO extends ConnectTemplateLink {
    private String tpName;
    private Integer tpType;
    private String tpContent;

    public String getTpName() {
        return tpName;
    }

    public void setTpName(String tpName) {
        this.tpName = tpName;
    }

    public Integer getTpType() {
        return tpType;
    }

    public void setTpType(Integer tpType) {
        this.tpType = tpType;
    }

    public String getTpContent() {
        return tpContent;
    }

    public void setTpContent(String tpContent) {
        this.tpContent = tpContent;
    }
}
