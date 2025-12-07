package org.wsitm.schemax.metainfo.impl;

import cn.hutool.db.meta.MetaUtil;
import cn.hutool.db.meta.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wsitm.schemax.constant.RdbmsConstants;
import org.wsitm.schemax.entity.vo.TableVO;
import org.wsitm.schemax.metainfo.AbsMetaInfoHandler;
import org.wsitm.schemax.metainfo.anno.JdbcType;
import org.wsitm.schemax.utils.RdbmsUtil;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 通用元数据信息处理器
 */
@Component
@JdbcType(RdbmsConstants.JDBC_RDBMS)
public class RdbmsMetaInfoHandler extends AbsMetaInfoHandler {
    private static final Logger log = LoggerFactory.getLogger(RdbmsMetaInfoHandler.class);

    /**
     * 刷新数据
     *
     * @param connectId     连接ID
     * @param checkNameFunc 校验名称函数
     * @param consumer      消费者
     */
    @Override
    public void flushData(Integer connectId, Function<String, Boolean> checkNameFunc, Consumer<TableVO> consumer) {
        log.info("Rdbms读取表信息");
        try (RdbmsUtil.ShimDataSource dataSource = RdbmsUtil.getDataSource(connectId)) {
            List<String> tableNames = MetaUtil.getTables(dataSource);
            for (String tableName : tableNames) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                if (!checkNameFunc.apply(tableName)) {
                    continue;
                }
                try {
                    log.info("读取表 {} 的信息", tableName);
                    Table table = MetaUtil.getTableMeta(dataSource, tableName);
                    TableVO tableVO = new TableVO(table);
                    consumer.accept(tableVO);
                } catch (Exception e) {
                    log.error("获取表信息失败", e);
                }
            }
        }
    }

}
