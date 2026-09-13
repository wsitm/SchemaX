package org.wsitm.schemax.metainfo;

import org.wsitm.schemax.entity.vo.ConnectInfoVO;

import java.util.function.BooleanSupplier;

public interface IMetaInfoHandler {

    /**
     * 加载数据到缓存
     *
     * @param connectInfoVO 连接信息对象
     * @param aliveCheck    任务存活检查，返回 false 表示已被更新的刷新任务取代，应尽快安全停止
     * @return 是否完整完成（未被取代）
     */
    boolean loadDataToCache(ConnectInfoVO connectInfoVO, BooleanSupplier aliveCheck);

}
