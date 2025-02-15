package org.wsitm.rdbms.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import org.wsitm.rdbms.constant.RdbmsConstants;
import org.wsitm.rdbms.ehcache.CacheKit;
import org.wsitm.rdbms.entity.domain.JdbcInfo;
import org.wsitm.rdbms.entity.vo.ConnectInfoVO;
import org.wsitm.rdbms.entity.vo.TableVO;
import org.wsitm.rdbms.exception.ServiceException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 缓存工具类
 *
 * @author wsitm
 */
public abstract class CacheUtil {

    public static final String INFO_KEY = "info";
    public static final String JDBC_JSON_PATH = RdbmsConstants.INF_PATH + File.separator + "jdbc-info.json";
    public static final String CONNECT_JSON_PATH = RdbmsConstants.INF_PATH + File.separator + "connect-info.json";

    /**
     * 获取驱动管理列表
     *
     * @return 驱动列表
     */
    public static List<JdbcInfo> getJdbcInfoList() {
        return CacheKit.get(INFO_KEY, "jdbc-info", () -> {
            File file = new File(JDBC_JSON_PATH);
            if (!FileUtil.exist(file)) {
                return new ArrayList<>();
            }
            String jdbcJsonStr = FileUtil.readUtf8String(file);
            if (StrUtil.isEmpty(jdbcJsonStr)) {
                return new ArrayList<>();
            }
            return JSON.parseArray(jdbcJsonStr, JdbcInfo.class);
        });
    }

    /**
     * 保存驱动列表到缓存
     *
     * @param jdbcInfoList 驱动列表
     */
    public static void saveJdbcInfoList(List<JdbcInfo> jdbcInfoList) {
        CacheKit.put(INFO_KEY, "jdbc-info", jdbcInfoList);
        FileUtil.writeUtf8String(JSON.toJSONString(jdbcInfoList), new File(JDBC_JSON_PATH));
    }

    /**
     * 通过驱动ID获取驱动信息
     *
     * @param jdbcId 驱动ID
     * @return 驱动信息
     */
    public static JdbcInfo getJdbcInfo(String jdbcId) {
        List<JdbcInfo> jdbcInfoList = getJdbcInfoList();
        for (JdbcInfo jdbcInfo : jdbcInfoList) {
            if (jdbcInfo.getJdbcId().equals(jdbcId)) {
                return jdbcInfo;
            }
        }
        throw new ServiceException("驱动信息不存在");
    }

    /**
     * 保存一个驱动信息到缓存
     *
     * @param jdbcInfo 驱动信息
     */
    public static void saveItemToJdbcInfo(JdbcInfo jdbcInfo) {
        List<JdbcInfo> jdbcInfoList = getJdbcInfoList();
        Optional<JdbcInfo> optional = jdbcInfoList.stream()
                .filter(info -> info.getJdbcId().equals(jdbcInfo.getJdbcId()))
                .findFirst();
        if (optional.isPresent()) {
            ListUtil.setOrPadding(jdbcInfoList, jdbcInfoList.indexOf(optional.get()), jdbcInfo);
        } else {
            jdbcInfoList.add(jdbcInfo);
        }
        saveJdbcInfoList(jdbcInfoList);
    }

    /**
     * 通过 驱动ID组 移除缓存
     *
     * @param jdbcIds 驱动ID组
     */
    public static void removeJdbcInfoByIds(String[] jdbcIds) {
        List<JdbcInfo> jdbcInfoList = getJdbcInfoList();
        for (String jdbcId : jdbcIds) {
            Optional<JdbcInfo> optional = jdbcInfoList.stream()
                    .filter(info -> info.getJdbcId().equals(jdbcId))
                    .findFirst();
            optional.ifPresent(jdbcInfoList::remove);
        }
        saveJdbcInfoList(jdbcInfoList);
    }

    /**
     * 获取所有连接配置列表
     *
     * @return 连接配置列表
     */
    public static List<ConnectInfoVO> getConnectInfoList() {
        return CacheKit.get(INFO_KEY, "connect-info", () -> {
            File file = new File(CONNECT_JSON_PATH);
            if (!FileUtil.exist(file)) {
                return new ArrayList<>();
            }
            String jdbcJsonStr = FileUtil.readUtf8String(file);
            if (StrUtil.isEmpty(jdbcJsonStr)) {
                return new ArrayList<>();
            }
            return JSON.parseArray(jdbcJsonStr, ConnectInfoVO.class);
        });
    }

    /**
     * 保存连接配置列表到缓存
     *
     * @param connectInfoList 连接配置列表
     */
    public static void saveConnectInfoList(List<ConnectInfoVO> connectInfoList) {
        CacheKit.put(INFO_KEY, "connect-info", connectInfoList);
        FileUtil.writeUtf8String(JSON.toJSONString(connectInfoList), new File(CONNECT_JSON_PATH));
    }

    /**
     * 保存一个连接配置到缓存
     *
     * @param connectInfoVO 连接配置
     */
    public static void saveItemToConnectInfo(ConnectInfoVO connectInfoVO) {
        List<ConnectInfoVO> connectInfoList = getConnectInfoList();
        Optional<ConnectInfoVO> optional = connectInfoList.stream()
                .filter(info -> info.getConnectId().equals(connectInfoVO.getConnectId()))
                .findFirst();
        if (optional.isPresent()) {
            ListUtil.setOrPadding(connectInfoList, connectInfoList.indexOf(optional.get()), connectInfoVO);
        } else {
            connectInfoList.add(connectInfoVO);
        }
        saveConnectInfoList(connectInfoList);
    }

    /**
     * 通过 连接ID 获取连接配置
     *
     * @param connectId 连接ID
     * @return 连接配置
     */
    public static ConnectInfoVO getConnectInfo(String connectId) {
        List<ConnectInfoVO> connectInfoList = getConnectInfoList();
        for (ConnectInfoVO connectInfoVO : connectInfoList) {
            if (connectInfoVO.getConnectId().equals(connectId)) {
                return connectInfoVO;
            }
        }
        throw new ServiceException("配置信息不存在");
    }

    /**
     * 通过 连接ID组 移除缓存
     *
     * @param connectIds 连接ID组
     */
    public static void removeConnectInfoByIds(String[] connectIds) {
        List<ConnectInfoVO> connectInfoList = getConnectInfoList();
        for (String connectId : connectIds) {
            Optional<ConnectInfoVO> optional = connectInfoList.stream()
                    .filter(info -> info.getConnectId().equals(connectId))
                    .findFirst();
            optional.ifPresent(connectInfoList::remove);
        }
        saveConnectInfoList(connectInfoList);
    }

    // ----------------------------------------------------------------------------------------------------------------

    public static final String DATA_KEY = "data";

    /**
     * 获取缓存 Loading key
     *
     * @param connectId 连接ID
     * @return key
     */
    public static String getLoadingKey(String connectId) {
        return String.format(RdbmsConstants.CACHE_LOADING_KEY, connectId);
    }

    /**
     * 获取缓存 History key
     *
     * @param connectId 连接ID
     * @return key
     */
    public static String getHistoryKey(String connectId) {
        return String.format(RdbmsConstants.CACHE_HISTORY_KEY, connectId);
    }

    /**
     * 获取缓存 Metainfo key
     *
     * @param connectId 连接ID
     * @param nanoId    历史标记，nanoId
     * @return key
     */
    public static String getMetainfoKey(String connectId, String nanoId) {
        return String.format(RdbmsConstants.CACHE_METAINFO_KEY, connectId, nanoId);
    }

    /**
     * 是否正在加载数据到缓存中
     *
     * @param connectId 连接ID
     * @return 布尔
     */
    public static Boolean isLoading(String connectId) {
        String loadingKey = getLoadingKey(connectId);
        Boolean loading = CacheKit.get(DATA_KEY, loadingKey);
        return Boolean.TRUE.equals(loading);
    }

    /**
     * 加载数据到缓存情况，1、已加载，2、加载中，3、无缓存
     *
     * @param connectId 连接ID
     * @return 类型
     */
    public static Integer cacheType(String connectId) {
        Boolean isLoading = isLoading(connectId);
        if (Boolean.TRUE.equals(isLoading)) {
            return 2;
        }

        String historyKey = getHistoryKey(connectId);
        List<String> keyList = CacheKit.get(DATA_KEY, historyKey);
        if (CollUtil.isEmpty(keyList)) {
            return 3;
        }
        String nanoId = keyList.get(0);
        if (Boolean.TRUE.equals(isLoading) && keyList.size() > 1) {
            // 如果正在加载中，临时先使用旧数据
            nanoId = keyList.get(1);
        }

        String realKey = getMetainfoKey(connectId, nanoId);
        List<TableVO> cacheList = CacheKit.get(DATA_KEY, realKey);
        if (CollUtil.isEmpty(cacheList)) {
            return 3;
        }

        return 1;
    }

    /**
     * 获取表元数据列表
     *
     * @param connectId 连接ID
     * @return 表元数据列表
     */
    public static List<TableVO> getTableMetaList(String connectId) {
        String loadingKey = getLoadingKey(connectId);
        String historyKey = getHistoryKey(connectId);

        Boolean isLoading = CacheKit.get(DATA_KEY, loadingKey);
        List<String> keyList = CacheKit.get(DATA_KEY, historyKey);
        if (CollUtil.isEmpty(keyList)) {
            return new ArrayList<>();
        }
        String nanoId = keyList.get(0);
        if (Boolean.TRUE.equals(isLoading) && keyList.size() > 1) {
            // 如果正在加载中，临时先使用旧数据
            nanoId = keyList.get(1);
        }

        String realKey = getMetainfoKey(connectId, nanoId);
        List<TableVO> cacheList = CacheKit.get(DATA_KEY, realKey);
        if (CollUtil.isEmpty(cacheList)) {
            return new ArrayList<>();
        }
        return cacheList;
    }

}
