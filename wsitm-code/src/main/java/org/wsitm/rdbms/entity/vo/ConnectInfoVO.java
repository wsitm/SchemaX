package org.wsitm.rdbms.entity.vo;

import org.wsitm.rdbms.entity.domain.ConnectInfo;

/**
 * 连接配置对象 dim_connect_info
 *
 * @author wsitm
 * @date 2025-01-11
 */
public class ConnectInfoVO extends ConnectInfo {

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
     * 加载数据到缓存情况，1、已加载，2、加载中，3、无缓存
     */
    private Integer cacheType;

    public String getJdbcName() {
        return jdbcName;
    }

    public void setJdbcName(String jdbcName) {
        this.jdbcName = jdbcName;
    }

    public String getJdbcFile() {
        return jdbcFile;
    }

    public void setJdbcFile(String jdbcFile) {
        this.jdbcFile = jdbcFile;
    }

    public String getDriverClass() {
        return driverClass;
    }

    public void setDriverClass(String driverClass) {
        this.driverClass = driverClass;
    }

    public Integer getCacheType() {
        return cacheType;
    }

    public void setCacheType(Integer cacheType) {
        this.cacheType = cacheType;
    }
}
