package org.wsitm.schemax.entity.vo;

import org.wsitm.schemax.entity.domain.ConnectInfo;

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
     * 加载的表结构数量
     */
    private Integer tableCount;


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

    public Integer getTableCount() {
        return tableCount;
    }

    public void setTableCount(Integer tableCount) {
        this.tableCount = tableCount;
    }

}
