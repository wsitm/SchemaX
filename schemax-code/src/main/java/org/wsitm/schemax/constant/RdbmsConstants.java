package org.wsitm.schemax.constant;

import java.io.File;
import java.util.regex.Pattern;

/**
 * 缓存的key 常量
 *
 * @author wsitm
 */
public class RdbmsConstants {

    /**
     * 文件和日志所在路径
     */
    public static final String FILE_PATH = "files";

    /**
     * 配置所在路径
     */
    public static final String CFG_PATH = "config";

    /**
     * 驱动包所在路径
     */
    public static final String INF_PATH = CFG_PATH + File.separator + "info";


    /**
     * 缓存Key-配置信息
     */
    public static final String INFO_KEY = "info";
    public static final String JDBC_INFO = "jdbc-info";
    public static final String CONNECT_INFO = "connect-info";
    public static final String SUFFIX = ".json";

    /**
     * 配置文件路径
     */
    public static final String JDBC_JSON_PATH = INF_PATH + File.separator + JDBC_INFO + SUFFIX;
    public static final String CONNECT_JSON_PATH = INF_PATH + File.separator + CONNECT_INFO + SUFFIX;


    /**
     * 驱动包所在路径
     */
    public static final String LIB_PATH = CFG_PATH + File.separator + "jdbc-lib";

    /**
     * 缓存Key-数据信息
     */
    public static final String DATA_LOADING_KEY = "data-loading";
    public static final String DATA_METAINFO_KEY = "data-metainfo";

    public static final String CACHE_LOADING_KEY = "rdbms:loading:%s";
    public static final String CACHE_METAINFO_KEY = "rdbms:metainfo:%s:%s";

    public static final String CACHE_METAINFO_SUB_KEY_MAIN = "main";
    public static final String CACHE_METAINFO_SUB_KEY_TEMP = "temp";

    /**
     * 正则表达式：只允许英文、数字和下划线
     */
    public static final Pattern RDBMS_PATTERN = Pattern.compile("^[a-zA-Z0-9_.]+$");


    /**
     * 通用驱动标识
     */
    public static final String JDBC_RDBMS = "rdbms";

}
