//package org.wsitm.schemax.utils;
//
//import cn.hutool.core.collection.CollUtil;
//import cn.hutool.core.collection.ListUtil;
//import cn.hutool.core.io.FileUtil;
//import cn.hutool.core.util.StrUtil;
//import com.alibaba.fastjson2.JSON;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.jdbc.core.RowMapper;
//import org.wsitm.schemax.entity.domain.JdbcInfo;
//import org.wsitm.schemax.entity.vo.ConnectInfoVO;
//import org.wsitm.schemax.entity.vo.TableVO;
//import org.wsitm.schemax.exception.ServiceException;
//
//import java.io.File;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Optional;
//import java.util.function.Supplier;
//
//import static org.wsitm.schemax.constant.RdbmsConstants.*;
//
//
///**
// * 缓存工具类
// *
// * @author wsitm
// */
//public abstract class CacheUtil {
//    private static final Logger log = LoggerFactory.getLogger(CacheUtil.class);
//
//    private static JdbcTemplate jdbcTemplate;
//
//    public static void setJdbcTemplate(JdbcTemplate jdbcTemplate) {
//        CacheUtil.jdbcTemplate = jdbcTemplate;
//    }
//
//    // --------------   配置信息和数据加载    -----------------------------------------------------------------------------
//
//    /**
//     * 获取驱动管理列表
//     *
//     * @return 驱动列表
//     */
//    public static List<JdbcInfo> getJdbcInfoList() {
//        return get(INFO_KEY, JDBC_INFO, () -> {
//            File file = new File(JDBC_JSON_PATH);
//            if (!FileUtil.exist(file)) {
//                return new ArrayList<>();
//            }
//            String jdbcJsonStr = FileUtil.readUtf8String(file);
//            if (StrUtil.isEmpty(jdbcJsonStr)) {
//                return new ArrayList<>();
//            }
//            return JSON.parseArray(jdbcJsonStr, JdbcInfo.class);
//        });
//    }
//
//    /**
//     * 保存驱动列表到缓存
//     *
//     * @param jdbcInfoList 驱动列表
//     */
//    public static void saveJdbcInfoList(List<JdbcInfo> jdbcInfoList) {
//        put(INFO_KEY, JDBC_INFO, jdbcInfoList);
//        FileUtil.writeUtf8String(JSON.toJSONString(jdbcInfoList), new File(JDBC_JSON_PATH));
//    }
//
//    /**
//     * 通过驱动ID获取驱动信息
//     *
//     * @param jdbcId 驱动ID
//     * @return 驱动信息
//     */
//    public static JdbcInfo getJdbcInfo(String jdbcId) {
//        List<JdbcInfo> jdbcInfoList = getJdbcInfoList();
//        for (JdbcInfo jdbcInfo : jdbcInfoList) {
//            if (jdbcInfo.getJdbcId().equals(jdbcId)) {
//                return jdbcInfo;
//            }
//        }
//        throw new ServiceException("驱动信息不存在");
//    }
//
//    /**
//     * 保存一个驱动信息到缓存
//     *
//     * @param jdbcInfo 驱动信息
//     */
//    public static void saveItemToJdbcInfo(JdbcInfo jdbcInfo) {
//        List<JdbcInfo> jdbcInfoList = getJdbcInfoList();
//        Optional<JdbcInfo> optional = jdbcInfoList.stream()
//                .filter(info -> info.getJdbcId().equals(jdbcInfo.getJdbcId())).findFirst();
//        if (optional.isPresent()) {
//            ListUtil.setOrPadding(jdbcInfoList, jdbcInfoList.indexOf(optional.get()), jdbcInfo);
//        } else {
//            jdbcInfoList.add(jdbcInfo);
//        }
//        saveJdbcInfoList(jdbcInfoList);
//    }
//
//    /**
//     * 通过 驱动ID组 移除缓存
//     *
//     * @param jdbcIds 驱动ID组
//     */
//    public static void removeJdbcInfoByIds(String[] jdbcIds) {
//        List<JdbcInfo> jdbcInfoList = getJdbcInfoList();
//        for (String jdbcId : jdbcIds) {
//            Optional<JdbcInfo> optional = jdbcInfoList.stream()
//                    .filter(info -> info.getJdbcId().equals(jdbcId)).findFirst();
//            optional.ifPresent(jdbcInfoList::remove);
//        }
//        saveJdbcInfoList(jdbcInfoList);
//    }
//
//    /**
//     * 获取所有连接配置列表
//     *
//     * @return 连接配置列表
//     */
//    public static List<ConnectInfoVO> getConnectInfoList() {
//        return get(INFO_KEY, CONNECT_INFO, () -> {
//            File file = new File(CONNECT_JSON_PATH);
//            if (!FileUtil.exist(file)) {
//                return new ArrayList<>();
//            }
//            String jdbcJsonStr = FileUtil.readUtf8String(file);
//            if (StrUtil.isEmpty(jdbcJsonStr)) {
//                return new ArrayList<>();
//            }
//            return JSON.parseArray(jdbcJsonStr, ConnectInfoVO.class);
//        });
//    }
//
//    /**
//     * 保存连接配置列表到缓存
//     *
//     * @param connectInfoList 连接配置列表
//     */
//    public static void saveConnectInfoList(List<ConnectInfoVO> connectInfoList) {
//        put(INFO_KEY, CONNECT_INFO, connectInfoList);
//        FileUtil.writeUtf8String(JSON.toJSONString(connectInfoList), new File(CONNECT_JSON_PATH));
//    }
//
//    /**
//     * 保存一个连接配置到缓存
//     *
//     * @param connectInfoVO 连接配置
//     */
//    public static void saveItemToConnectInfo(ConnectInfoVO connectInfoVO) {
//        List<ConnectInfoVO> connectInfoList = getConnectInfoList();
//        Optional<ConnectInfoVO> optional = connectInfoList.stream()
//                .filter(info -> info.getConnectId().equals(connectInfoVO.getConnectId())).findFirst();
//        if (optional.isPresent()) {
//            ListUtil.setOrPadding(connectInfoList, connectInfoList.indexOf(optional.get()), connectInfoVO);
//        } else {
//            connectInfoList.add(connectInfoVO);
//        }
//        saveConnectInfoList(connectInfoList);
//    }
//
//    /**
//     * 通过 连接ID 获取连接配置
//     *
//     * @param connectId 连接ID
//     * @return 连接配置
//     */
//    public static ConnectInfoVO getConnectInfo(String connectId) {
//        List<ConnectInfoVO> connectInfoList = getConnectInfoList();
//        for (ConnectInfoVO connectInfoVO : connectInfoList) {
//            if (connectInfoVO.getConnectId().equals(connectId)) {
//                return connectInfoVO;
//            }
//        }
//        throw new ServiceException("配置信息不存在");
//    }
//
//    /**
//     * 通过 连接ID组 移除缓存
//     *
//     * @param connectIds 连接ID组
//     */
//    public static void removeConnectInfoByIds(String[] connectIds) {
//        List<ConnectInfoVO> connectInfoList = getConnectInfoList();
//        for (String connectId : connectIds) {
//            Optional<ConnectInfoVO> optional = connectInfoList.stream()
//                    .filter(info -> info.getConnectId().equals(connectId)).findFirst();
//            optional.ifPresent(connectInfoList::remove);
//        }
//        saveConnectInfoList(connectInfoList);
//    }
//
//    // ----------------------------------------------------------------------------------------------------------------
//
//    /**
//     * 获取缓存 Loading key
//     *
//     * @param connectId 连接ID
//     * @return key
//     */
//    public static String getLoadingKey(String connectId) {
//        return String.format(CACHE_LOADING_KEY, connectId);
//    }
//
//
//    /**
//     * 获取缓存 Metainfo key
//     *
//     * @param connectId 连接ID
//     * @param subKey    子标记
//     * @return key
//     */
//    public static String getMetainfoKey(String connectId, String subKey) {
//        return String.format(CACHE_METAINFO_KEY, connectId, subKey);
//    }
//
//    /**
//     * 加载数据到缓存情况，1、已加载，2、加载中，3、无缓存
//     *
//     * @param connectId 连接ID
//     * @return 类型
//     */
//    public static Integer cacheType(String connectId) {
//        String loadingKey = getLoadingKey(connectId);
//        Boolean isLoading = get(DATA_LOADING_KEY, loadingKey);
//        if (Boolean.TRUE.equals(isLoading)) {
//            return 2;
//        }
//        String realKey = getMetainfoKey(connectId, CACHE_METAINFO_SUB_KEY_MAIN);
//        boolean exist = exist(DATA_METAINFO_KEY, realKey);
//        return exist ? 1 : 3;
//    }
//
//    /**
//     * 获取表元数据列表
//     *
//     * @param connectId 连接ID
//     * @return 表元数据列表
//     */
//    public static List<TableVO> getTableMetaList(String connectId) {
//        String mainKey = getMetainfoKey(connectId, CACHE_METAINFO_SUB_KEY_MAIN);
//        List<TableVO> cacheList = get(DATA_METAINFO_KEY, mainKey);
//        if (CollUtil.isEmpty(cacheList)) {
//            return new ArrayList<>();
//        }
//        return cacheList;
//    }
//
//    // ----------------------------------------------------------------------------------------------------------------
//
//    @SuppressWarnings("unchecked")
//    public static <T> T get(String cacheName, String key) {
//        try {
//            String sql = "SELECT cache_value FROM cache_entries WHERE cache_name = ? AND cache_key = ?";
//            byte[] valueBytes = jdbcTemplate.queryForObject(sql, new Object[]{cacheName, key}, byte[].class);
//            if (valueBytes != null) {
//                return (T) JSON.parseObject(new String(valueBytes), Object.class);
//            }
//        } catch (Exception e) {
//            log.debug("Cache miss for {}:{} - {}", cacheName, key, e.getMessage());
//        }
//        return null;
//    }
//
//    @SuppressWarnings("unchecked")
//    public static <T> T get(String cacheName, String key, Supplier<T> dataLoader) {
//        Object data = get(cacheName, key);
//        if (data == null) {
//            data = dataLoader.get();
//            put(cacheName, key, data);
//        }
//        return (T) data;
//    }
//
//    public static void put(String cacheName, String key, Object value) {
//        try {
//            String jsonValue = JSON.toJSONString(value);
//            String sql = "MERGE INTO cache_entries (cache_name, cache_key, cache_value) VALUES (?, ?, ?)";
//            jdbcTemplate.update(sql, cacheName, key, jsonValue.getBytes());
//        } catch (Exception e) {
//            log.error("Failed to put cache entry {}:{}", cacheName, key, e);
//        }
//    }
//
//    public static boolean exist(String cacheName, String key) {
//        try {
//            String sql = "SELECT COUNT(*) FROM cache_entries WHERE cache_name = ? AND cache_key = ?";
//            Integer count = jdbcTemplate.queryForObject(sql, new Object[]{cacheName, key}, Integer.class);
//            return count != null && count > 0;
//        } catch (Exception e) {
//            log.error("Failed to check existence of cache entry {}:{}", cacheName, key, e);
//            return false;
//        }
//    }
//}
