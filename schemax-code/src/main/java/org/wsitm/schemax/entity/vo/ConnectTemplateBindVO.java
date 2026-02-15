package org.wsitm.schemax.entity.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 连接模板绑定参数
 */
public class ConnectTemplateBindVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Integer> tpIdList;
    private Integer defTpId;

    public List<Integer> getTpIdList() {
        return tpIdList;
    }

    public void setTpIdList(List<Integer> tpIdList) {
        this.tpIdList = tpIdList;
    }

    public Integer getDefTpId() {
        return defTpId;
    }

    public void setDefTpId(Integer defTpId) {
        this.defTpId = defTpId;
    }
}
