package org.wsitm.schemax.utils;

import cn.hutool.core.lang.JarClassLoader;
import cn.hutool.core.util.ClassLoaderUtil;
import cn.hutool.db.ds.simple.AbstractDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wsitm.schemax.constant.RdbmsConstants;
import org.wsitm.schemax.entity.domain.JdbcInfo;
import org.wsitm.schemax.entity.vo.ConnectInfoVO;
import org.wsitm.schemax.exception.ServiceException;
import org.wsitm.schemax.mapper.ConnectInfoMapper;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public abstract class RdbmsUtil {
    private static final Logger log = LoggerFactory.getLogger(RdbmsUtil.class);
    public static final Map<String, ShimDriver> DRIVER_SHIM_MAP = new ConcurrentHashMap<>();
    private static final Map<String, JarClassLoader> CLASS_LOADER_MAP = new ConcurrentHashMap<>();

    /**
     * 加载JdbcJar
     *
     * @param jdbcInfo 驱动信息
     */
    public static void loadJdbcJar(JdbcInfo jdbcInfo) {

        // 先卸载
        unloadJdbcJar(jdbcInfo);

        try {
            File jarFile = new File(RdbmsConstants.LIB_PATH, jdbcInfo.getJdbcFile());
            JarClassLoader jarClassLoader = ClassLoaderUtil.getJarClassLoader(jarFile);
            CLASS_LOADER_MAP.put(jdbcInfo.getJdbcId(), jarClassLoader);
            Class<?> clazz = jarClassLoader.loadClass(jdbcInfo.getDriverClass());
            Driver driver = (Driver) clazz.getDeclaredConstructor().newInstance();

            // 注册驱动
            ShimDriver shimDriver = new ShimDriver(driver);
            DriverManager.registerDriver(shimDriver);
            DRIVER_SHIM_MAP.put(jdbcInfo.getJdbcId(), shimDriver);
        } catch (ClassNotFoundException e) {
            log.error("驱动异常", e);
            throw new ServiceException("找不到驱动类：" + e.getMessage());
        } catch (Exception e) {
            log.error("加载异常", e);
            throw new ServiceException(e.getMessage());
        }
    }

    /**
     * 卸载JdbcJar
     *
     * @param jdbcInfo 驱动信息
     */
    public static void unloadJdbcJar(JdbcInfo jdbcInfo) {
        try {
            ShimDriver shimDriver = DRIVER_SHIM_MAP.get(jdbcInfo.getJdbcId());
            if (shimDriver != null) {
                DriverManager.deregisterDriver(shimDriver);
                DRIVER_SHIM_MAP.remove(jdbcInfo.getJdbcId());
            }
        } catch (SQLException ignored) {
        }
        try {
            JarClassLoader jarClassLoader = CLASS_LOADER_MAP.get(jdbcInfo.getJdbcId());
            if (jarClassLoader != null) {
                jarClassLoader.close();
                CLASS_LOADER_MAP.remove(jdbcInfo.getJdbcId());
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * 是否加载JdbcJar
     *
     * @param jdbcId 驱动ID
     * @return 结果
     */
    public static boolean isLoadJdbcJar(String jdbcId) {
        return DRIVER_SHIM_MAP.get(jdbcId) != null;
    }


    /**
     * 基于连接ID获取数据源
     *
     * @param connectId 连接ID
     * @return 数据源
     */
    public static ShimDataSource getDataSource(String connectId) {
//        ConnectInfoVO connectInfoVO = CacheUtil.getConnectInfo(connectId);
        ConnectInfoMapper connectInfoMapper = SpringUtils.getBean(ConnectInfoMapper.class);
        ConnectInfoVO connectInfoVO = connectInfoMapper.selectConnectInfoByConnectId(connectId);

        String url = connectInfoVO.getJdbcUrl();
        String username = connectInfoVO.getUsername();
        String password = connectInfoVO.getPassword();
//        String driverClass = connectInfoVO.getDriverClass();
        return new ShimDataSource(url, username, password);
    }


    /**
     * 由于 DriverManager 无法直接使用动态加载的驱动，
     * 因此需要创建一个包装类 ShimDriver，将动态加载的驱动注册到 DriverManager
     */
    public static class ShimDriver implements Driver {
        private final Driver driver;

        public ShimDriver(Driver driver) {
            this.driver = driver;
        }

        @Override
        public boolean acceptsURL(String url) throws SQLException {
            return driver.acceptsURL(url);
        }

        @Override
        public Connection connect(String url, Properties info) {
            try {
                return driver.connect(url, info);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public int getMajorVersion() {
            return driver.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return driver.getMinorVersion();
        }

        @Override
        public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return driver.getParentLogger();
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
            return driver.getPropertyInfo(url, info);
        }

        @Override
        public boolean jdbcCompliant() {
            return driver.jdbcCompliant();
        }
    }

    /**
     * DataSource 包装类
     */
    public static class ShimDataSource extends AbstractDataSource {

        private String url; // jdbc url
        private String user; // 用户名
        private String pass; // 密码

        /**
         * 构造
         *
         * @param url  jdbc url
         * @param user 用户名
         * @param pass 密码
         */
        public ShimDataSource(String url, String user, String pass) {
            this.url = url;
            this.user = user;
            this.pass = pass;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }

        public String getPass() {
            return pass;
        }

        public void setPass(String pass) {
            this.pass = pass;
        }

        // -------------------------------------------------------------------- Getters and Setters end

        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(this.url, this.user, this.pass);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return DriverManager.getConnection(this.url, username, password);
        }

        @Override
        public void close() {
            // Not need to close;
        }
    }
}
