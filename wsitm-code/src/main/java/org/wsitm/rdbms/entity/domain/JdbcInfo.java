package org.wsitm.rdbms.entity.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 驱动管理对象 dim_jdbc_info
 *
 * @author wsitm
 * @date 2025-01-11
 */
public class JdbcInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 驱动ID
     */
    private String jdbcId;

    /**
     * 驱动名称
     */
    private String jdbcName;

    /**
     * 驱动文件
     */
    private String jdbcFile;

    /**
     * 驱动类名称
     */
    private String driverClass;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    public void setJdbcId(String jdbcId) {
        this.jdbcId = jdbcId;
    }

    public String getJdbcId() {
        return jdbcId;
    }

    public void setJdbcName(String jdbcName) {
        this.jdbcName = jdbcName;
    }

    public String getJdbcName() {
        return jdbcName;
    }

    public void setJdbcFile(String jdbcFile) {
        this.jdbcFile = jdbcFile;
    }

    public String getJdbcFile() {
        return jdbcFile;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
