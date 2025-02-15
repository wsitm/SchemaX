package org.wsitm.rdbms.entity.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 连接配置对象 dim_connect_info
 *
 * @author wsitm
 * @date 2025-01-11
 */
public class ConnectInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 连接ID
     */
    private String connectId;

    /**
     * 连接名称
     */
    private String connectName;

    /**
     * 驱动ID
     */
    private String jdbcId;

    /**
     * JDBC URL
     */
    private String jdbcUrl;

    /**
     * 用户
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    public void setConnectId(String connectId) {
        this.connectId = connectId;
    }

    public String getConnectId() {
        return connectId;
    }

    public void setConnectName(String connectName) {
        this.connectName = connectName;
    }

    public String getConnectName() {
        return connectName;
    }

    public void setJdbcId(String jdbcId) {
        this.jdbcId = jdbcId;
    }

    public String getJdbcId() {
        return jdbcId;
    }

    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
