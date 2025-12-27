package org.wsitm.schemax.entity.vo;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 模板管理对象 dim_template_info
 *
 * @author wsitm
 * @date 2025-12-27
 */
public class TemplateInfoVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer tpId;

    private String tpName;

    /**
     * 模板类型(1=excel,2=word,3=markdown)
     */
    private Integer tpType;

    /**
     * 模板内容
     */
    private String tpContent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    public Integer getTpId() {
        return tpId;
    }

    public void setTpId(Integer tpId) {
        this.tpId = tpId;
    }

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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}

