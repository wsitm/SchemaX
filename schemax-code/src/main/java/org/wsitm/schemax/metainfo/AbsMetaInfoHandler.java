package org.wsitm.schemax.metainfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wsitm.schemax.entity.vo.ConnectInfoVO;
import org.wsitm.schemax.entity.vo.TableVO;
import org.wsitm.schemax.mapper.TableMetaMapper;
import org.wsitm.schemax.utils.SpringUtils;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;


public abstract class AbsMetaInfoHandler implements IMetaInfoHandler {
    private static final Logger log = LoggerFactory.getLogger(AbsMetaInfoHandler.class);

    /**
     * 加载数据到缓存
     *
     * @param connectInfoVO 连接信息对象
     * @param aliveCheck    任务存活检查，返回 false 表示已被更新的刷新任务取代，应尽快安全停止
     * @return 是否完整完成（未被取代）
     */
    public boolean loadDataToCache(ConnectInfoVO connectInfoVO, BooleanSupplier aliveCheck) {
        // 获取连接ID
        Integer connectId = connectInfoVO.getConnectId();

        TableMetaMapper tableMetaMapper = SpringUtils.getBean(TableMetaMapper.class);
        // 任务已被新任务取代，直接放弃执行（残留数据由新任务开头的 deleteByConnectId 清理）
        if (!aliveCheck.getAsBoolean()) {
            log.info("连接ID: {} 的刷新任务已被新任务取代，放弃执行", connectId);
            return false;
        }
        // 开始加载表信息数据到缓存
        log.info("开始加载表信息数据到缓存……");
        // 删除表信息数据
        tableMetaMapper.deleteByConnectId(connectId);
        // 刷新数据到缓存，根据表名模式过滤并添加到缓存中
        boolean completed = flushData(connectId,
                MetaInfoUtil.createTableNameChecker(connectInfoVO),
                this.createTableConsumer(connectId, tableMetaMapper),
                aliveCheck);
        if (completed) {
            // 完成加载表信息数据到缓存
            log.info("加载表信息数据到缓存完成 ^v^ ");
        }
        return completed;
    }


    /**
     * 创建表格消费者函数
     *
     * @param connectId 连接ID，用于设置TableVO对象的连接标识
     * @param mapper    表格元数据映射器，用于执行插入操作
     * @return 返回一个Consumer接口实现，用于消费TableVO对象并进行持久化处理
     */
    private Consumer<TableVO> createTableConsumer(Integer connectId, TableMetaMapper mapper) {
        return tableVO -> {
            // 设置连接ID
            tableVO.setConnectId(connectId);
            // 执行插入操作
            mapper.insert(tableVO);
        };
    }


    /**
     * 刷新数据
     *
     * @param connectId     连接ID
     * @param checkNameFunc 校验名称函数
     * @param consumer      消费者
     * @param aliveCheck    任务存活检查，返回 false 表示已被更新的刷新任务取代，应尽快安全停止
     * @return 是否完整完成（未被取代）
     */
    public abstract boolean flushData(Integer connectId, Function<String, Boolean> checkNameFunc,
                                      Consumer<TableVO> consumer, BooleanSupplier aliveCheck);

}
