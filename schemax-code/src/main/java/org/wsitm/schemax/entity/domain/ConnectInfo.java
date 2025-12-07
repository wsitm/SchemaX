package org.wsitm.schemax.entity.domain;

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
    private Integer connectId;

    /**
     * 连接名称
     */
    private String connectName;

    /**
     * 驱动ID
     */
    private Integer jdbcId;

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
     * 通配符，用于过滤表
     * <strong>注</strong>：通配符匹配，匹配包含，
     * <strong>?</strong> 表示匹配任何单个，
     * <strong>*</strong> 表示匹配任何多个，
     * <strong>!</strong> 表示剔除，
     * <strong>,</strong> 逗号分隔多个通配符
     * <br/>
     * <strong>例</strong>："sys_*,!tb_*"，表示以 sys_ 开头，和不以 tb_ 开头的表
     */
    private String wildcard;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    public void setConnectId(Integer connectId) {
        this.connectId = connectId;
    }

    public Integer getConnectId() {
        return connectId;
    }

    public void setConnectName(String connectName) {
        this.connectName = connectName;
    }

    public String getConnectName() {
        return connectName;
    }

    public void setJdbcId(Integer jdbcId) {
        this.jdbcId = jdbcId;
    }

    public Integer getJdbcId() {
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

    public String getWildcard() {
        return wildcard;
    }

    public void setWildcard(String wildcard) {
        this.wildcard = wildcard;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
