package org.wsitm.schemax.metainfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wsitm.schemax.entity.vo.ConnectInfoVO;
import org.wsitm.schemax.entity.vo.TableVO;
import org.wsitm.schemax.mapper.TableMetaMapper;
import org.wsitm.schemax.utils.SpringUtils;

import java.util.function.Consumer;
import java.util.function.Function;


public abstract class AbsMetaInfoHandler implements IMetaInfoHandler {
    private static final Logger log = LoggerFactory.getLogger(AbsMetaInfoHandler.class);

    /**
     * 加载数据到缓存
     *
     * @param connectInfoVO 连接信息对象
     */
    public void loadDataToCache(ConnectInfoVO connectInfoVO) {
        // 获取连接ID
        Integer connectId = connectInfoVO.getConnectId();

        TableMetaMapper tableMetaMapper = SpringUtils.getBean(TableMetaMapper.class);
        try {
            // 开始加载表信息数据到缓存
            log.info("开始加载表信息数据到缓存……");
            // 删除表信息数据
            tableMetaMapper.deleteByConnectId(connectId);
            // 刷新数据到缓存，根据表名模式过滤并添加到缓存中
            flushData(connectId,
                    MetaInfoUtil.createTableNameChecker(connectInfoVO),
                    this.createTableConsumer(connectId, tableMetaMapper));
            // 完成加载表信息数据到缓存
            log.info("加载表信息数据到缓存完成 ^v^ ");
        } finally {
            // 标记数据加载完成

        }
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
     */
    public abstract void flushData(Integer connectId, Function<String, Boolean> checkNameFunc, Consumer<TableVO> consumer);

}
