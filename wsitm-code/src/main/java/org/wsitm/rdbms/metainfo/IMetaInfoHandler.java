package org.wsitm.rdbms.metainfo;

import org.wsitm.rdbms.entity.vo.ConnectInfoVO;

public interface IMetaInfoHandler {

    /**
     * 加载数据到缓存
     *
     * @param connectInfoVO 连接信息对象
     */
    void loadDataToCache(ConnectInfoVO connectInfoVO);

}
