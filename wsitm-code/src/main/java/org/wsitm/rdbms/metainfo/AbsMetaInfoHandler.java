package org.wsitm.rdbms.metainfo;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wsitm.rdbms.ehcache.EhcacheKit;
import org.wsitm.rdbms.entity.vo.TableVO;
import org.wsitm.rdbms.utils.CacheUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.wsitm.rdbms.constant.RdbmsConstants.*;


public abstract class AbsMetaInfoHandler implements IMetaInfoHandler {
    private static final Logger log = LoggerFactory.getLogger(AbsMetaInfoHandler.class);

    /**
     * 加载数据到缓存
     *
     * @param connectId 连接ID
     */
    public void loadDataToCache(String connectId) {

        // 判断是否正在加载数据到缓存中
        String loadingKey = CacheUtil.getLoadingKey(connectId);
        Boolean isLoading = EhcacheKit.get(DATA_LOADING_KEY, loadingKey);
        if (Boolean.TRUE.equals(isLoading)) {
            log.warn("正在加载表信息到缓存中...");
            return;
        }
        EhcacheKit.put(DATA_LOADING_KEY, loadingKey, true);

        String nanoId = IdUtil.nanoId();
        String historyKey = CacheUtil.getHistoryKey(connectId);

        List<String> hisKeyList = EhcacheKit.get(DATA_HISTORY_KEY, historyKey, ArrayList::new);
        hisKeyList.add(0, nanoId);
        EhcacheKit.put(DATA_HISTORY_KEY, historyKey, hisKeyList);

        if (CollUtil.isNotEmpty(hisKeyList) && hisKeyList.size() > 2) {
            // 只保留两份数据
            for (int i = hisKeyList.size() - 1; i >= 2; i--) {
                String oldRealKey = CacheUtil.getMetainfoKey(connectId, hisKeyList.get(i));
                EhcacheKit.remove(DATA_METAINFO_KEY, oldRealKey);
            }
            hisKeyList = CollUtil.sub(hisKeyList, 0, 2);
            EhcacheKit.put(DATA_HISTORY_KEY, historyKey, hisKeyList);
        }

        String realKey = CacheUtil.getMetainfoKey(connectId, nanoId);
        try {
            List<TableVO> tableVOList = EhcacheKit.get(DATA_METAINFO_KEY, realKey, ArrayList::new);
            flushData(connectId, tableVOList::add);
            EhcacheKit.put(DATA_METAINFO_KEY, realKey, tableVOList);
        } finally {
            EhcacheKit.put(DATA_LOADING_KEY, loadingKey, false);
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
