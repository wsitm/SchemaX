package org.wsitm.schemax.metainfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wsitm.schemax.entity.vo.ConnectInfoVO;
import org.wsitm.schemax.mapper.ConnectInfoMapper;
import org.wsitm.schemax.service.IMetaSnapshotService;
import org.wsitm.schemax.utils.SpringUtils;

import java.util.function.BooleanSupplier;

public class MetaInfoTask implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(MetaInfoTask.class);

    private final Integer connectId;

    /**
     * 任务存活检查，返回 false 表示当前任务已被更新的刷新任务取代
     */
    private final BooleanSupplier aliveCheck;

    public MetaInfoTask(Integer connectId, BooleanSupplier aliveCheck) {
        this.connectId = connectId;
        this.aliveCheck = aliveCheck;
    }

    @Override
    public void run() {
        log.info("连接ID: {}， 正在线程 {} 上运行.", connectId, Thread.currentThread().getName());
        // 获取数据库连接信息
        ConnectInfoMapper connectInfoMapper = SpringUtils.getBean(ConnectInfoMapper.class);
        ConnectInfoVO connectInfoVO = connectInfoMapper.selectConnectInfoByConnectId(connectId);
        if (connectInfoVO == null) {
            log.warn("连接ID: {} 不存在或已被删除，跳过元数据加载", connectId);
            return;
        }
        IMetaInfoHandler metaInfoHandler = MetaInfoFactory.getInstance(connectInfoVO.getDriverClass());
        boolean completed = metaInfoHandler.loadDataToCache(connectInfoVO, aliveCheck);
        if (!completed) {
            log.info("连接ID: {} 的刷新任务已被新任务取代，不再创建自动快照", connectId);
            return;
        }
        try {
            SpringUtils.getBean(IMetaSnapshotService.class).createAutoSnapshot(connectInfoVO);
        } catch (Exception e) {
            log.error("创建数据库结构快照失败，连接ID: {}", connectId, e);
        }
        log.info("连接ID: {} 处理完成。", connectId);
    }

}
