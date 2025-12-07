package org.wsitm.schemax.metainfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wsitm.schemax.entity.vo.ConnectInfoVO;
import org.wsitm.schemax.mapper.ConnectInfoMapper;
import org.wsitm.schemax.utils.SpringUtils;

public class MetaInfoTask implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(MetaInfoTask.class);

    private final Integer connectId;

    public MetaInfoTask(Integer connectId) {
        this.connectId = connectId;
    }

    @Override
    public void run() {
        log.info("连接ID: {}， 正在线程 {} 上运行.", connectId, Thread.currentThread().getName());
        // 获取数据库连接信息
        ConnectInfoMapper connectInfoMapper = SpringUtils.getBean(ConnectInfoMapper.class);
//        ConnectInfoVO connectInfoVO = CacheUtil.getConnectInfo(connectId);
        ConnectInfoVO connectInfoVO = connectInfoMapper.selectConnectInfoByConnectId(connectId);
        IMetaInfoHandler metaInfoHandler = MetaInfoFactory.getInstance(connectInfoVO.getDriverClass());
        metaInfoHandler.loadDataToCache(connectInfoVO);
        log.info("连接ID: {} 处理完成。", connectId);
    }

}
