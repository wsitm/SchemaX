package org.wsitm.rdbms.metainfo;

import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wsitm.rdbms.entity.vo.ConnectInfoVO;
import org.wsitm.rdbms.entity.vo.TableVO;
import org.wsitm.rdbms.utils.CacheUtil;
import org.wsitm.rdbms.utils.CommonUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.wsitm.rdbms.constant.RdbmsConstants.*;


public abstract class AbsMetaInfoHandler implements IMetaInfoHandler {
    private static final Logger log = LoggerFactory.getLogger(AbsMetaInfoHandler.class);

    /**
     * 加载数据到缓存
     *
     * @param connectInfoVO 连接信息对象
     */
    public void loadDataToCache(ConnectInfoVO connectInfoVO) {
        // 获取连接ID
        String connectId = connectInfoVO.getConnectId();
        // 标记正在加载数据到缓存中
        String loadingKey = CacheUtil.getLoadingKey(connectId);
        CacheUtil.put(DATA_LOADING_KEY, loadingKey, true);
        try {
            // 开始加载表信息数据到临时缓存
            log.info("开始加载表信息数据到临时缓存……");
            // 获取临时缓存的键
            String tempKey = CacheUtil.getMetainfoKey(connectId, CACHE_METAINFO_SUB_KEY_TEMP);
            // 在临时缓存中存储一个空的表信息列表
            CacheUtil.put(DATA_METAINFO_KEY, tempKey, new ArrayList<>());
            // 从临时缓存中获取表信息列表
            List<TableVO> tableVOList = CacheUtil.get(DATA_METAINFO_KEY, tempKey, ArrayList::new);

            // 处理忽略的表名
            String[] skipStrArr = StrUtil.isEmpty(connectInfoVO.getWildcard()) ?
                    new String[]{"*"} : CommonUtil.dealStipStrArr(connectInfoVO.getWildcard().split(","));

            // 刷新数据到缓存，根据表名模式过滤并添加到临时缓存中
            flushData(connectId,
                    (tableName) -> CommonUtil.matchAnyIgnoreCase(tableName, skipStrArr),
                    (item) -> {
                        CacheUtil.put(DATA_LOADING_KEY, loadingKey, true);
                        tableVOList.add(item);
                    });
            // 完成加载表信息数据到临时缓存
            log.info("加载表信息数据到临时缓存完成 ^v^ ");

            // 复制临时缓存表信息数据到正式缓存
            log.info("复制临时缓存表信息数据到正式缓存");
            // 获取正式缓存的键
            String mainKey = CacheUtil.getMetainfoKey(connectId, CACHE_METAINFO_SUB_KEY_MAIN);
            // 将表信息列表存储到正式缓存中
            CacheUtil.put(DATA_METAINFO_KEY, mainKey, tableVOList);
        } finally {
            // 标记数据加载完成
            CacheUtil.put(DATA_LOADING_KEY, loadingKey, false);
        }
    }


    /**
     * 刷新数据
     *
     * @param connectId     连接ID
     * @param checkNameFunc 校验名称函数
     * @param consumer      消费者
     */
    public abstract void flushData(String connectId, Function<String, Boolean> checkNameFunc, Consumer<TableVO> consumer);

}
