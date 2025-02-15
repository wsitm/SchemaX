package org.wsitm.rdbms.entity.vo;

import org.wsitm.rdbms.entity.domain.JdbcInfo;

/**
 * 驱动管理对象 dim_jdbc_info
 *
 * @author wsitm
 * @date 2025-01-11
 */
public class JdbcInfoVo extends JdbcInfo {
    private static final long serialVersionUID = 1L;

    /**
     * 是否安装
     */
    private Boolean isLoaded;


    public Boolean getIsLoaded() {
        return isLoaded;
    }

    public void setIsLoaded(Boolean isLoaded) {
        this.isLoaded = isLoaded;
    }
}
