package org.wsitm.schemax.metainfo.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import cn.hutool.db.dialect.DriverNamePool;
import cn.hutool.db.handler.EntityListHandler;
import cn.hutool.db.sql.SqlBuilder;
import cn.hutool.db.sql.SqlExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wsitm.schemax.entity.vo.ColumnVO;
import org.wsitm.schemax.entity.vo.IndexVO;
import org.wsitm.schemax.entity.vo.TableVO;
import org.wsitm.schemax.metainfo.AbsMetaInfoHandler;
import org.wsitm.schemax.metainfo.anno.JdbcType;
import org.wsitm.schemax.utils.RdbmsUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@JdbcType({DriverNamePool.DRIVER_ORACLE, DriverNamePool.DRIVER_ORACLE_OLD})
public class OracleMetaInfoHandler extends AbsMetaInfoHandler {
    private static final Logger log = LoggerFactory.getLogger(OracleMetaInfoHandler.class);

    public static final String TABLE_SQL = "SELECT " +
            "       T.TABLESPACE_NAME,\n" +
            "       T.TABLE_NAME,\n" +
            "       C.COMMENTS,\n" +
            "       T.NUM_ROWS\n" +
            "FROM USER_TABLES T\n" +
            "LEFT JOIN USER_TAB_COMMENTS C ON T.TABLE_NAME = C.TABLE_NAME\n" +
            "ORDER BY T.TABLE_NAME";
    public static final String COLUMN_SQL = "SELECT \n" +
            "       D.TABLE_NAME,\n" +
            "       D.COLUMN_NAME,\n" +
            "       DECODE(SUBSTR(D.DATA_TYPE, 1, 9), 'TIMESTAMP', DECODE(SUBSTR(D.DATA_TYPE, 10, 1), '(', DECODE(SUBSTR(D.DATA_TYPE, 19, 5), 'LOCAL', -102, 'TIME ', -101, 93), DECODE(SUBSTR(D.DATA_TYPE, 16, 5), 'LOCAL', -102, 'TIME ', -101, 93)), 'INTERVAL ', DECODE(SUBSTR(D.DATA_TYPE, 10, 3), 'DAY', -104, 'YEA', -103),DECODE(D.DATA_TYPE, 'BINARY_DOUBLE', 101, 'BINARY_FLOAT', 100, 'BFILE', -13, 'BLOB', 2004, 'CHAR', 1, 'CLOB', 2005, 'COLLECTION', 2003, 'DATE', 93, 'FLOAT', 6, 'LONG', -1, 'LONG RAW', -4, 'NCHAR', -15, 'NCLOB', 2011, 'NUMBER', 2, 'NVARCHAR', -9, 'NVARCHAR2', -9, 'OBJECT', 2002, 'OPAQUE/XMLTYPE', 2009, 'RAW', -3, 'REF', 2006, 'ROWID', -8, 'SQLXML', 2009, 'UROWID', -8, 'VARCHAR2', 12, 'VARRAY', 2003, 'XMLTYPE', 2009, DECODE((SELECT A.TYPECODE FROM ALL_TYPES A WHERE A.TYPE_NAME = D.DATA_TYPE AND ((A.OWNER IS NULL AND D.DATA_TYPE_OWNER IS NULL) OR (A.OWNER = D.DATA_TYPE_OWNER))), 'OBJECT', 2002, 'COLLECTION', 2003, 1111))) AS DATA_TYPE,\n" +
            "       D.DATA_TYPE AS TYPE_NAME,\n" +
            "       DECODE(D.DATA_PRECISION, NULL, DECODE(D.DATA_TYPE, 'NUMBER', DECODE(D.DATA_SCALE, NULL, 0 , 38), DECODE(D.DATA_TYPE, 'CHAR', D.CHAR_LENGTH, 'VARCHAR', D.CHAR_LENGTH, 'VARCHAR2', D.CHAR_LENGTH, 'NVARCHAR2', D.CHAR_LENGTH, 'NCHAR', D.CHAR_LENGTH, 'NUMBER', 0, D.DATA_LENGTH)), D.DATA_PRECISION) AS COLUMN_SIZE,\n" +
            "       DECODE(D.DATA_TYPE, 'NUMBER', DECODE(D.DATA_PRECISION, NULL, DECODE(D.DATA_SCALE, NULL, -127 , D.DATA_SCALE), D.DATA_SCALE), D.DATA_SCALE) AS DIGIT,\n" +
            "       DECODE(D.NULLABLE, 'N', 0, 1) AS NULLABLE,\n" +
            "       COMM.COMMENTS as COLUMN_COMMENTS,\n" +
            "       D.DATA_DEFAULT,\n" +
            "       CASE\n" +
            "           WHEN CONS.CONSTRAINT_NAME IS NOT NULL AND CONS.CONSTRAINT_TYPE = 'P' THEN 1\n" +
            "           ELSE 0\n" +
            "       END AS IS_PK\n" +
            "FROM USER_TAB_COLUMNS D\n" +
            "LEFT JOIN\n" +
            "  ( SELECT UCC.TABLE_NAME,\n" +
            "           UCC.COLUMN_NAME,\n" +
            "           UC.CONSTRAINT_NAME,\n" +
            "           UC.CONSTRAINT_TYPE\n" +
            "   FROM USER_CONS_COLUMNS UCC\n" +
            "   JOIN USER_CONSTRAINTS UC ON UCC.CONSTRAINT_NAME = UC.CONSTRAINT_NAME\n" +
            "   AND UC.CONSTRAINT_TYPE IN ('P', 'U')) CONS ON D.TABLE_NAME = CONS.TABLE_NAME\n" +
            "AND D.COLUMN_NAME = CONS.COLUMN_NAME\n" +
            "LEFT JOIN USER_COL_COMMENTS COMM ON D.TABLE_NAME = COMM.TABLE_NAME AND D.COLUMN_NAME = COMM.COLUMN_NAME\n" +
            "WHERE D.TABLE_NAME IN (%s) \n" +
            "ORDER BY D.TABLE_NAME,D.COLUMN_ID\n";
    public static final String INDEX_SQL = "SELECT \n" +
            "    CASE WHEN UI.UNIQUENESS='UNIQUE' THEN 1 ELSE 0 END AS NON_UNIQUE,\n" +
            "    UI.INDEX_NAME,\n" +
            "    UI.TABLE_NAME,\n" +
            "    UI.INDEX_TYPE,\n" +
            "    UI.STATUS,\n" +
            "    UIC.COLUMN_NAMES\n" +
            "FROM USER_INDEXES UI\n" +
            "JOIN (\n" +
            "       SELECT INDEX_NAME, \n" +
            "              LISTAGG(COLUMN_NAME, ',') WITHIN GROUP (ORDER BY COLUMN_POSITION) AS COLUMN_NAMES\n" +
            "       FROM USER_IND_COLUMNS\n" +
            "       GROUP BY INDEX_NAME\n" +
            ") UIC ON UI.INDEX_NAME = UIC.INDEX_NAME\n" +
            "WHERE UI.TABLE_NAME IN (%s) \n" +
            "ORDER BY UI.TABLE_NAME, UI.INDEX_NAME\n";

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
            String schema = connection.getSchema();
            String catalog = connection.getCatalog();
            log.info("Oracle表查询：\n" + TABLE_SQL);
            List<Entity> tableEntityList = SqlExecutor.query(connection, SqlBuilder.of(TABLE_SQL), EntityListHandler.create());

            for (List<Entity> entityList : ListUtil.partition(tableEntityList, 100)) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                String tableNames = entityList.stream()
                        .map(e -> String.format("'%s'", e.getStr("TABLE_NAME")))
                        .collect(Collectors.joining(","));
                // 查询字段信息
                String columnSql = String.format(COLUMN_SQL, tableNames);
                log.info("Oracle字段查询：\n" + columnSql);
                List<Entity> columnEntityList = SqlExecutor.query(connection, SqlBuilder.of(columnSql), EntityListHandler.create());
                Map<String, List<Entity>> columnEntityGroup = columnEntityList.stream().collect(Collectors.groupingBy(e -> e.getStr("TABLE_NAME")));
                // 查询索引信息
                String indexSql = String.format(INDEX_SQL, tableNames);
                log.info("Oracle索引查询：\n" + indexSql);
                List<Entity> indexEntityList = SqlExecutor.query(connection, SqlBuilder.of(indexSql), EntityListHandler.create());
                Map<String, List<Entity>> indexEntityGroup = indexEntityList.stream().collect(Collectors.groupingBy(e -> e.getStr("TABLE_NAME")));
                // 查询表信息
                for (Entity entity : entityList) {
                    String tableName = entity.getStr("TABLE_NAME");
                    if (!checkNameFunc.apply(tableName)) {
                        continue;
                    }
                    log.info("读取表 {} 的信息", tableName);
                    TableVO tableVO = new TableVO();
                    tableVO.setSchema(schema);
                    tableVO.setCatalog(catalog);
                    tableVO.setTableName(tableName);
                    tableVO.setComment(entity.getStr("COMMENTS"));
                    tableVO.setNumRows(entity.getInt("NUM_ROWS"));
                    tableVO.setColumnList(
                            columnEntityGroup.get(tableName).stream()
                                    .map(c -> {
                                        ColumnVO columnVO = new ColumnVO();
                                        columnVO.setTableName(c.getStr("TABLE_NAME"));
                                        columnVO.setName(c.getStr("COLUMN_NAME"));
                                        columnVO.setType(c.getInt("DATA_TYPE"));
                                        columnVO.setTypeName(c.getStr("TYPE_NAME"));
                                        columnVO.setSize(c.getInt("COLUMN_SIZE"));
                                        columnVO.setDigit(c.getInt("DIGIT"));
                                        columnVO.setNullable(c.getBool("NULLABLE"));
                                        columnVO.setComment(c.getStr("COLUMN_COMMENTS"));
                                        columnVO.setAutoIncrement(false);
                                        columnVO.setColumnDef(c.getStr("DATA_DEFAULT"));
                                        columnVO.setPk(c.getBool("IS_PK"));
                                        return columnVO;
                                    })
                                    .collect(Collectors.toList())
                    );
                    List<Entity> indexs = indexEntityGroup.get(entity.getStr("TABLE_NAME"));
                    if (CollUtil.isNotEmpty(indexs)) {
                        tableVO.setIndexList(
                                indexs.stream()
                                        .map(i -> {
                                            IndexVO indexVO = new IndexVO();
                                            indexVO.setNonUnique(i.getBool("NON_UNIQUE"));
                                            indexVO.setIndexName(i.getStr("INDEX_NAME"));
                                            indexVO.setTableName(i.getStr("TABLE_NAME"));
                                            indexVO.setColumnList(StrUtil.splitToArray(i.getStr("COLUMN_NAMES"), ","));
                                            return indexVO;
                                        })
                                        .collect(Collectors.toList())
                        );
                    }
                    consumer.accept(tableVO);
                }
            }
        } catch (SQLException sqlException) {
            log.error("获取表格信息异常", sqlException);
        }
    }
}
