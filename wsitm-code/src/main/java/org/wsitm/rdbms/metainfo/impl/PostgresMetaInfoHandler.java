package org.wsitm.rdbms.metainfo.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.dialect.DriverNamePool;
import cn.hutool.db.handler.EntityListHandler;
import cn.hutool.db.meta.MetaUtil;
import cn.hutool.db.sql.SqlBuilder;
import cn.hutool.db.sql.SqlExecutor;
import org.wsitm.rdbms.entity.vo.TableVO;
import org.wsitm.rdbms.metainfo.AbsMetaInfoHandler;
import org.wsitm.rdbms.metainfo.anno.JdbcType;
import org.wsitm.rdbms.utils.RdbmsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

@Component
@JdbcType({DriverNamePool.DRIVER_POSTGRESQL})
public class PostgresMetaInfoHandler extends AbsMetaInfoHandler {
    private static final Logger log = LoggerFactory.getLogger(PostgresMetaInfoHandler.class);

    public static final String TABLE_SQL = "select \n" +
            "   distinct \n" +
            "   case when b.relname is not null then b.relname else a.relname end as table_name, \n" +
            "   case when d2.description is not null then d2.description else d.description end AS table_comment, \n" +
            "   n.nspname as table_schema \n" +
            "from pg_class a \n" +
            "left join pg_inherits p on p.inhrelid=a.oid \n" +
            "left join pg_class b on b.oid=p.inhparent \n" +
            "left join pg_namespace n on n.oid = a.relnamespace \n" +
            "left join pg_description d on (d.objoid = a.oid and d.objsubid = 0) \n" +
            "left join pg_description d2 on (d2.objoid = b.oid and d2.objsubid = 0) \n" +
            "where a.relkind in ('r', 'v') \n" +
            "   and n.nspname = '%s' \n" +
            "order by table_name";


    /**
     * 刷新数据
     *
     * @param connectId 连接ID
     * @param consumer  消费者
     */
    @Override
    public void flushData(String connectId, Consumer<TableVO> consumer) {
        try (
                RdbmsUtil.ShimDataSource dataSource = RdbmsUtil.getDataSource(connectId);
                Connection connection = dataSource.getConnection()
        ) {
            String tableSql = String.format(TABLE_SQL, connection.getSchema());
            log.info("Postgres表查询：\n" + tableSql);
            List<Entity> tableEntityList = SqlExecutor.query(connection, SqlBuilder.of(tableSql), EntityListHandler.create());
            for (Entity entity : tableEntityList) {
                // jdbc 获取表信息错误，分区表也查询出来了，临时重写
                String tableName = entity.getStr("table_name");
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
