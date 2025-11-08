package org.wsitm.schemax.metainfo.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.dialect.DriverNamePool;
import cn.hutool.db.handler.EntityListHandler;
import cn.hutool.db.meta.MetaUtil;
import cn.hutool.db.sql.SqlBuilder;
import cn.hutool.db.sql.SqlExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wsitm.schemax.entity.vo.TableVO;
import org.wsitm.schemax.metainfo.AbsMetaInfoHandler;
import org.wsitm.schemax.metainfo.anno.JdbcType;
import org.wsitm.schemax.utils.RdbmsUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Component
@JdbcType({DriverNamePool.DRIVER_MYSQL, DriverNamePool.DRIVER_MYSQL_V6, DriverNamePool.DRIVER_MARIADB})
public class MysqlMetaInfoHandler extends AbsMetaInfoHandler {
    private static final Logger log = LoggerFactory.getLogger(MysqlMetaInfoHandler.class);

    public static final String TABLE_SQL = "select \n" +
            "       table_catalog,\n" +
            "       table_schema,\n" +
            "       table_name,\n" +
            "       table_comment,\n" +
            "       table_rows\n" +
            "from information_schema.tables \n" +
            "where table_schema = '%s'\n" +
            "and table_type = 'BASE TABLE'\n" +
            "order by table_name";


    /**
     * 刷新数据
     *
     * @param connectId     连接ID
     * @param checkNameFunc 校验名称函数
     * @param consumer      消费者
     */
    @Override
    public void flushData(String connectId, Function<String, Boolean> checkNameFunc, Consumer<TableVO> consumer) {
        try (
                RdbmsUtil.ShimDataSource dataSource = RdbmsUtil.getDataSource(connectId);
                Connection connection = dataSource.getConnection()
        ) {
            String tableSql = String.format(TABLE_SQL, connection.getCatalog());
            log.info("MySql表查询：\n" + tableSql);
            // jdbc 获取表信息漏缺，临时重写
            List<Entity> tableEntityList = SqlExecutor.query(connection, SqlBuilder.of(tableSql), new EntityListHandler(true));
            for (Entity entity : tableEntityList) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                String tableName = entity.getStr("table_name");
                if (!checkNameFunc.apply(tableName)) {
                    continue;
                }
                log.info("读取表 {} 的信息", tableName);
                TableVO tableVO = new TableVO(MetaUtil.getTableMeta(dataSource, tableName));
                if (StrUtil.isEmpty(tableVO.getSchema())) {
                    tableVO.setSchema(entity.getStr("table_schema"));
                }
                if (StrUtil.isEmpty(tableVO.getComment())) {
                    tableVO.setComment(entity.getStr("table_comment"));
                }
                consumer.accept(tableVO);
            }
        } catch (SQLException sqlException) {
            log.error("获取表格信息异常", sqlException);
        }
    }
}
