package org.wsitm.rdbms.constant;

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
     * 驱动包所在路径
     */
    public static final String LIB_PATH = CFG_PATH + File.separator + "jdbc-lib";

    /**
     * 通用驱动标识
     */
    public static final String JDBC_RDBMS = "rdbms";

    /**
     * 缓存Key
     */
    public static final String CACHE_KEY = "rdbms:";

    public static final String CACHE_LOADING_KEY = "rdbms:loading:%s";
    public static final String CACHE_HISTORY_KEY = "rdbms:history:%s";
    public static final String CACHE_METAINFO_KEY = "rdbms:metainfo:%s:%s";

    /**
     * 正则表达式：只允许英文、数字和下划线
     */
    public static final Pattern RDBMS_PATTERN = Pattern.compile("^[a-zA-Z0-9_]+$");


}
