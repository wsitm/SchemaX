package org.wsitm.rdbms.metainfo;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wsitm.rdbms.ehcache.CacheKit;
import org.wsitm.rdbms.entity.vo.TableVO;
import org.wsitm.rdbms.utils.CacheUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class AbsMetaInfoHandler implements IMetaInfoHandler {
    private static final Logger log = LoggerFactory.getLogger(AbsMetaInfoHandler.class);

    /**
     * 加载数据到缓存
     *
     * @param connectId 连接ID
     */
    public void loadDataToCache(String connectId) {

        String loadingKey = CacheUtil.getLoadingKey(connectId);
        Boolean loading = CacheUtil.isLoading(connectId);
        if (Boolean.TRUE.equals(loading)) {
            log.warn("正在加载表信息到缓存中...");
            return;
        }
//        redisCache.setCacheObject(loadingKey, true);
        CacheKit.put(CacheUtil.DATA_KEY, loadingKey, true);

        String nanoId = IdUtil.nanoId();
        String historyKey = CacheUtil.getHistoryKey(connectId);

        List<String> hisKeyList = CacheKit.get(CacheUtil.DATA_KEY, historyKey, ArrayList::new);
        hisKeyList.add(nanoId);
        CacheKit.put(CacheUtil.DATA_KEY, historyKey, hisKeyList);
//        redisCache.leftPushToCacheList(historyKey, nanoId);

//        List<String> hisKeyList = redisCache.getCacheList(historyKey);
        if (CollUtil.isNotEmpty(hisKeyList) && hisKeyList.size() > 2) {
            // 只保留两份数据
            for (int i = hisKeyList.size() - 1; i >= 2; i--) {
//                redisCache.deleteObject(CacheUtil.getMetainfoKey(connectId, hisKeyList.get(i)));
                CacheKit.remove(CacheUtil.DATA_KEY, CacheUtil.getMetainfoKey(connectId, hisKeyList.get(i)));
            }
//            redisCache.trimList(historyKey, 0, 1);
            hisKeyList = CollUtil.sub(hisKeyList, 0, 1);
            CacheKit.put(CacheUtil.DATA_KEY, historyKey, hisKeyList);
        }

        String realKey = CacheUtil.getMetainfoKey(connectId, nanoId);
        try {
            flushData(connectId, (t) -> {
                List<TableVO> tableVOList = CacheKit.get(CacheUtil.DATA_KEY, realKey, ArrayList::new);
                tableVOList.add(t);
                CacheKit.put(CacheUtil.DATA_KEY, realKey, tableVOList);
//                redisCache.rightPushToCacheList(realKey, t);
            });
        } finally {
//            redisCache.setCacheObject(loadingKey, false);
            CacheKit.put(CacheUtil.DATA_KEY, loadingKey, false);
        }
    }


    /**
     * 刷新数据
     *
     * @param connectId 连接ID
     * @param consumer  消费者
     */
    public abstract void flushData(String connectId, Consumer<TableVO> consumer);

}
