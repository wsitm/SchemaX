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
        CacheKit.put(CacheUtil.DATA_KEY, loadingKey, true);

        String nanoId = IdUtil.nanoId();
        String historyKey = CacheUtil.getHistoryKey(connectId);

        List<String> hisKeyList = CacheKit.get(CacheUtil.DATA_KEY, historyKey, ArrayList::new);
        hisKeyList.add(0, nanoId);
        CacheKit.put(CacheUtil.DATA_KEY, historyKey, hisKeyList);

        if (CollUtil.isNotEmpty(hisKeyList) && hisKeyList.size() > 2) {
            // 只保留两份数据
            for (int i = hisKeyList.size() - 1; i >= 2; i--) {
                CacheKit.remove(CacheUtil.DATA_KEY, CacheUtil.getMetainfoKey(connectId, hisKeyList.get(i)));
            }
            hisKeyList = CollUtil.sub(hisKeyList, 0, 2);
            CacheKit.put(CacheUtil.DATA_KEY, historyKey, hisKeyList);
        }

        String realKey = CacheUtil.getMetainfoKey(connectId, nanoId);
        try {
            flushData(connectId, (t) -> {
                List<TableVO> tableVOList = CacheKit.get(CacheUtil.DATA_KEY, realKey, ArrayList::new);
                tableVOList.add(t);
                CacheKit.put(CacheUtil.DATA_KEY, realKey, tableVOList);
            });
        } finally {
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
