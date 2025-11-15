package org.wsitm.schemax.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.github.drinkjava2.jdialects.DDLFeatures;
import com.github.drinkjava2.jdialects.Dialect;
import com.github.drinkjava2.jdialects.ReservedDBWords;
import com.github.drinkjava2.jdialects.Type;
import com.github.drinkjava2.jdialects.annotation.jpa.GenerationType;
import com.github.drinkjava2.jdialects.model.ColumnModel;
import com.github.drinkjava2.jdialects.model.IndexModel;
import com.github.drinkjava2.jdialects.model.TableModel;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.SetStatement;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.comment.Comment;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.Index;
import net.sf.jsqlparser.statement.drop.Drop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wsitm.schemax.constant.DialectEnum;
import org.wsitm.schemax.constant.RdbmsConstants;
import org.wsitm.schemax.dialects.DDLCreateUtils;
import org.wsitm.schemax.entity.vo.*;
import org.wsitm.schemax.exception.ServiceException;

import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;

public abstract class DDLUtil {
    private static final Logger log = LoggerFactory.getLogger(DDLUtil.class);

    /**
     * 生成DDL语句
     *
     * @param tableVOList 表格元数据信息
     * @param database    数据源
     * @return DDL
     */
    public static Map<String, String[]> genDDL(List<TableVO> tableVOList, String database) {
        Map<String, String[]> result = new LinkedHashMap<>();
        // 获取方言
        Dialect dialect = DialectEnum.getDialectByDatabase(database).getDialect();
        for (TableVO tableVO : tableVOList) {
            try {
                if (tableVO.getExtend() != null) {
                    ExtendVO extendVO = tableVO.getExtend();
                    if (StrUtil.isNotEmpty(extendVO.getDropTable())) {
                        TableModel tableModel = new TableModel(extendVO.getDropTable());
                        String[] arrDDL = dialect.toDropDDL(tableModel);
                        result.put(tableVO.getTableName(), arrDDL);
                    }
                    if (StrUtil.isNotEmpty(extendVO.getSourceSQL())) {
                        String sourceSql = extendVO.getSourceSQL();
                        if (StrUtil.startWithAnyIgnoreCase(dialect.getName(), "Mysql", "MariaDB", "ClickHouse", "Hive")) {
                            sourceSql = sourceSql.replaceAll("\"", "`");
                        } else {
                            sourceSql = sourceSql.replaceAll("`", "\"");
                        }
                        String[] arrDDL = new String[]{sourceSql};
                        result.put(tableVO.getTableName(), arrDDL);
                    }
                    if (StrUtil.isNotEmpty(extendVO.getAbnormalDDL())) {
                        String[] arrDDL = new String[]{
                                "-- 无法解析",
                                "-- " + extendVO.getAbnormalDDL()
                        };
                        result.put(tableVO.getTableName(), arrDDL);
                    }
                    continue;
                }

                TableModel tableModel = new TableModel();
                // 表名
                tableModel.setTableName(parcelName(dialect, tableVO.getTableName()));
                // 表描述
                tableModel.setComment(tableVO.getComment());
                // 设置字段信息
                List<String> pKeyList = new ArrayList<>();
                for (ColumnVO columnVO : tableVO.getColumnList()) {
                    ColumnModel columnModel = tableModel.addColumn(parcelName(dialect, columnVO.getName()));
                    columnModel.setPkey(columnVO.isPk());
                    if (columnVO.isPk()) {
                        pKeyList.add(columnVO.getName());
                    }
                    if (columnVO.isAutoIncrement()
                            && !DDLFeatures.NOT_SUPPORT.equals(dialect.getDdlFeatures().getIdentityColumnString())) {
                        columnModel.setIdGenerationType(GenerationType.IDENTITY);
                    }

                    // 前端转义的时候需要转换为数据库类型
                    if (columnVO.getType() == null && StrUtil.isNotEmpty(columnVO.getTypeName())) {
                        columnVO.setType(dialectTypeToJavaSqlType(columnVO.getTypeName()));
                    }

                    // columnModel.setColumnType(colDef2DialectType(dialect, column.getTypeName()));
                    columnModel.setColumnType(javaSqlTypeToDialectType(dialect, columnVO.getType()));
                    if (ListUtil.of(1111, 2003).contains(columnVO.getType())) {
                        // 特殊字段类型需要自定义
                        columnModel.setColumnDefinition(String.format(" %s %s %s",
                                columnVO.getTypeName().replaceAll("\"", ""),
                                columnVO.isNullable() ? "" : "not null",
                                StrUtil.emptyToDefault(columnVO.getColumnDef(), "")
                        ));
                    }

                    if (columnVO.getSize() > 0) {
                        columnModel.setLength(Convert.toInt(columnVO.getSize()));
                        columnModel.setPrecision(Convert.toInt(columnVO.getSize()));
                    } else {
                        if (ObjectUtil.equals(Types.INTEGER, columnVO.getType())) {
                            columnModel.setLength(11);
                            columnModel.setPrecision(11);
                        } else if (ObjectUtil.equals(Types.BIGINT, columnVO.getType())) {
                            columnModel.setLength(24);
                            columnModel.setPrecision(24);
                        } else if (ObjectUtil.equals(Types.NUMERIC, columnVO.getType())) {
                            columnModel.setLength(38);
                            columnModel.setPrecision(38);
                        } else if (ObjectUtil.equals(Types.VARCHAR, columnVO.getType())) {
                            columnModel.setLength(8192);
                        } else if (ObjectUtil.equals(Types.BINARY, columnVO.getType())) {
                            columnModel.setLength(8192 * 8);
                        } else {
                            columnModel.setLength(null);
                        }
                    }
                    if (columnVO.getDigit() != null && columnVO.getDigit() > 0) {
                        columnModel.setScale(columnVO.getDigit());
                    }

                    if (StrUtil.isNotEmpty(columnVO.getColumnDef())) {
                        if (ListUtil.of(Types.VARCHAR, Types.CHAR, Types.LONGVARCHAR, Types.LONGVARBINARY, Types.NVARCHAR).contains(columnVO.getType())
                                && !StrUtil.startWith(columnVO.getColumnDef(), "'")) {
                            columnModel.setDefaultValue(String.format("'%s'", columnVO.getColumnDef()));
                        } else {
                            columnModel.setDefaultValue(columnVO.getColumnDef());
                        }
                    }
                    columnModel.setNullable(columnVO.isNullable());
                    if (StrUtil.startWithAnyIgnoreCase(dialect.getName(), "Hive")) {
                        // Hive，没有主键，没有默认值，没有不为空
                        columnModel.setPkey(false);
                        pKeyList.clear();
                        columnModel.setDefaultValue(null);
                        columnModel.setNullable(true);
                    }
                    if (StrUtil.isNotEmpty(columnVO.getComment())) {
                        columnModel.setComment(columnVO.getComment().replace("'", "\\'"));
                    }
                }
                // 设置表索引
                if (CollUtil.isNotEmpty(tableVO.getIndexList())) {
                    tableModel.setIndexConsts(
                            tableVO.getIndexList().stream()
                                    //
                                    .filter(indexVO -> !CollUtil.isEqualList(pKeyList, Arrays.asList(indexVO.getColumnList())))
                                    .map(indexVO -> {
                                        IndexModel indexModel = new IndexModel();
                                        String indexName = parcelName(dialect, indexVO.getIndexName());
                                        if (CommonUtil.containsDigit(indexName)) {
                                            indexName = "index_" + RandomUtil.randomString(RandomUtil.BASE_CHAR, 16);
                                        }
                                        indexModel.setName(indexName);
                                        indexModel.setUnique(!indexVO.isNonUnique());
                                        indexModel.setColumnList(indexVO.getColumnList());
                                        return indexModel;
                                    })
                                    .collect(Collectors.toList())
                    );
                }

                if (StrUtil.isNotEmpty(tableModel.getComment())) {
                    // TODO 补充mysql的表描述
                    if (StrUtil.startWithAnyIgnoreCase(dialect.getName(), "Mysql", "MariaDB")) {
                        tableModel.setEngineTail(
                                StrUtil.emptyToDefault(tableModel.getEngineTail(), "") + String.format(" comment='%s'", tableModel.getComment()));
                    }
                    // TODO 补充ClickHouse的表描述
                    if (StrUtil.startWithAnyIgnoreCase(dialect.getName(), "ClickHouse")) {
                        tableModel.setTableTail(
                                StrUtil.emptyToDefault(tableModel.getEngineTail(), "") + String.format(" comment '%s'", tableModel.getComment()));
                    }
                    // TODO 补充Hive的表描述
                    if (StrUtil.startWithAnyIgnoreCase(dialect.getName(), "Hive")) {
                        tableModel.setTableTail(
                                StrUtil.emptyToDefault(tableModel.getEngineTail(), "") + String.format(" COMMENT '%s'", tableModel.getComment()));
                    }
                }

                String[] arrDDL = DDLCreateUtils.toCreateDDL(dialect, tableModel);
                result.put(tableVO.getTableName(), arrDDL);
            } catch (Exception e) {
                String[] arrDDL = new String[]{"-- 解析 " + tableVO.getTableName() + " 表失败，" + e.getMessage()};
                result.put(tableVO.getTableName(), arrDDL);
                log.error("生成DDL异常: " + tableVO.getTableName(), e);
            }
        }
        return result;
    }

//    public static Dialect guessDialect(DataSource dataSource) {
//        int majorVersion = 0;
//        int minorVersion = 0;
//        try (Connection connection = dataSource.getConnection()) {
//            DatabaseMetaData meta = connection.getMetaData();
//            String driverName = meta.getDriverName();
//            String databaseName = meta.getDatabaseProductName();
//            try {
//                majorVersion = meta.getDatabaseMajorVersion();
//                minorVersion = meta.getDatabaseMinorVersion();
//            } catch (Exception e) {
//                // 达梦的jdbc有问题，可能会导致这一步异常
//                log.error("获取数据库版本失败: " + e.getMessage());
//            }
//            return GuessDialectUtils.guessDialect(driverName, databaseName, majorVersion, minorVersion);
//        } catch (Exception e) {
//            log.error("推断方言失败: ", e);
//            throw new ServiceException("推断方言失败：" + e.getMessage());
//        }
//    }

    /**
     * 判断名称是否为保留字段 或 非数字英文下划线的，则以 引号 包含
     *
     * @param dialect 方言
     * @param name    字段名
     * @return 格式字段
     */
    public static String parcelName(Dialect dialect, String name) {
        boolean reserved = ReservedDBWords.isReservedWord(name);
        if (reserved || !RdbmsConstants.RDBMS_PATTERN.matcher(name).find()) {
            String dialectName = dialect.getName();
            String strFmt = StrUtil.startWithAnyIgnoreCase(dialectName, "MySQL", "MariaDB", "ClickHouse", "Hive") ? "`%s`" : "\"%s\"";
            return String.format(strFmt, name);
        }
        return name;
    }

//    public static Type colDef2DialectType(Dialect dialect, String columnDefination) {
//        String columnDef = StrUtils.substringBefore(columnDefination, "(");
//        if ("TEXT".equalsIgnoreCase(columnDef)) {
//            return Type.VARCHAR;
//        }
//        if ("DATETIME".equalsIgnoreCase(columnDef)) {
//            //DATETIME is only DB column type, no Java type
//            return Type.TIMESTAMP;
//        }
//        return Type.getByTypeName(columnDef);
//    }


    /**
     * 将java.sql.Types.xxx类型转换为Dialect的类型
     */
    public static Type javaSqlTypeToDialectType(Dialect dialect, int javaSqlType) {
        String name = dialect.getName();
        switch (javaSqlType) {
            case java.sql.Types.BIT:
                return Type.BIT;
            case java.sql.Types.TINYINT:
                return Type.TINYINT;
            case java.sql.Types.SMALLINT:
                return Type.SMALLINT;
            case java.sql.Types.INTEGER:
                return Type.INTEGER;
            case java.sql.Types.BIGINT:
                return Type.BIGINT;
            case java.sql.Types.FLOAT:
                return Type.FLOAT;
            case java.sql.Types.REAL:
                return Type.REAL;
            case java.sql.Types.DOUBLE:
                return Type.DOUBLE;
            case java.sql.Types.NUMERIC:
                return Type.NUMERIC;
            case java.sql.Types.DECIMAL:
                return Type.DECIMAL;
            case java.sql.Types.CHAR:
                return Type.CHAR;
            case java.sql.Types.VARCHAR:
                return Type.VARCHAR;
            case java.sql.Types.LONGVARCHAR:
                return Type.LONGVARCHAR;
            case java.sql.Types.DATE:
                return Type.DATE;
            case java.sql.Types.TIME:
                return Type.TIME;
            case java.sql.Types.TIMESTAMP:
                return Type.TIMESTAMP;
            case java.sql.Types.BINARY:
                return Type.BINARY;
            case java.sql.Types.VARBINARY:
                return Type.VARBINARY;
            case java.sql.Types.LONGVARBINARY:
                return Type.LONGVARBINARY;
            // 数组
            case 2003:
            case java.sql.Types.OTHER:
                return Type.UNKNOW;
            case java.sql.Types.JAVA_OBJECT:
                return Type.JAVA_OBJECT;
            case java.sql.Types.BLOB:
                return Type.BLOB;
            case java.sql.Types.CLOB:
                return Type.CLOB;
            case java.sql.Types.BOOLEAN:
                return Type.BOOLEAN;
            case java.sql.Types.NCHAR:
                if (StrUtil.startWithAnyIgnoreCase(name, "Mysql", "Postgres", "MariaDB")) {
                    return Type.CHAR;
                }
                return Type.NCHAR;
            case java.sql.Types.NVARCHAR:
                if (StrUtil.startWithAnyIgnoreCase(name, "Mysql", "Postgres", "MariaDB")) {
                    return Type.VARCHAR;
                }
                return Type.NVARCHAR;
            case java.sql.Types.LONGNVARCHAR:
                return Type.LONGNVARCHAR;
            case java.sql.Types.NCLOB:
                if (StrUtil.startWithAnyIgnoreCase(name, "Mysql", "Postgres", "MariaDB")) {
                    return Type.CLOB;
                }
                return Type.NCLOB;

            /*- JAVA8_BEGIN */
            case java.sql.Types.REF_CURSOR:
                return Type.JAVA_OBJECT;
            case java.sql.Types.TIME_WITH_TIMEZONE:
                return Type.TIME;
            case java.sql.Types.TIMESTAMP_WITH_TIMEZONE:
                return Type.TIMESTAMP;
            /* JAVA8_END */

            // oracle
            case 100:
                if (StrUtil.startWithAnyIgnoreCase(name, "Mysql", "Postgres", "MariaDB")) {
                    return Type.FLOAT;
                }
                return Type.BINARY_FLOAT;
            case 101:
                if (StrUtil.startWithAnyIgnoreCase(name, "Mysql", "Postgres", "MariaDB")) {
                    return Type.DOUBLE;
                }
                return Type.BINARY_DOUBLE;
            default:
                return Type.UNKNOW;
//                throw new DialectException("Unsupported java.sql.Types:" + javaSqlType);
        }
    }

    public static final String POINT_TAG = "__point__";

    /**
     * 逆向解析DDL语句，基于JSqlParser
     *
     * @param convertVo 转换信息
     * @return 表元数据信息
     */
    public static List<TableVO> parserDDL(ConvertVO convertVo) {
        String[] arrDDL = Arrays.stream(convertVo.getInputDDL().split("\n"))
                // 过滤掉以 -- 或 # 开头的行以及空行
                .filter(line -> !line.trim().isEmpty() && !line.trim().startsWith("--") && !line.trim().startsWith("#"))
                // 替换注释中的 ; 成中文的 ；
                .map(line -> {
                    line = line.replaceAll("(\\s+)", " ");
                    line = CommonUtil.replaceInQuotes(line, ";", "；");
                    return line;
                })
                // 将剩余行拼接成一个字符串
                .collect(Collectors.joining(" "))
                // 按 ; 切分
                .split(";");
        // 按 ; 切分，但忽略单引号内的 ;。该方法不行，大字符串出现内存溢出
        // .split(";(?=(?:[^']*'[^']*')*[^']*$)")

        Map<String, TableVO> tableVoMap = new LinkedHashMap<>();
//        Set<String> dropSet = new HashSet<>();
        Map<String, String> commentMap = new HashMap<>();
        Map<String, List<IndexVO>> indexListMap = new HashMap<>();
        for (String ddl : arrDDL) {
            if (StrUtil.isEmpty(ddl.trim())) {
                continue;
            }
            try {
//                String marker = "geometry|geography|_geometry|st_geomfromtext|st_setsrid|st_makepoint";
//                String ddl2 = ddl.replaceAll("(\\s+)([^\\s]*\\.)(?=(?i)(" + marker + "))", " ");
                String ddl2 = ddl;
                if ((StrUtil.startWithAnyIgnoreCase(ddl2.trim(), "create table")
                        || StrUtil.startWithAnyIgnoreCase(ddl2.trim(), "create index"))
                        && StrUtil.contains(ddl, ".")) {
                    ddl2 = ddl.replace(".", POINT_TAG);
                }
                if (StrUtil.startWithAnyIgnoreCase(ddl2.trim(), "create table")
                        && StrUtil.containsIgnoreCase(ddl2, "unique index")) {
                    ddl2 = ddl.replaceAll("(?<=(?i)UNIQUE).*?(?=\\()", " ");
                }
                if (StrUtil.startWithAnyIgnoreCase(ddl2.trim(), "create index")
                        && StrUtil.containsIgnoreCase(ddl2, "on table")) {
                    ddl2 = StrUtil.replaceIgnoreCase(ddl2, "on table", "on");
                }

                // 解析SQL语句
                Statement statement = CCJSqlParserUtil.parse(ddl2);
                // 如果是建表语句
                if (statement instanceof CreateTable) {
                    CreateTable createTable = (CreateTable) statement;
                    // 提取表名
                    Table table = createTable.getTable();
                    String tableName = correctName(table.getName());

                    TableVO tableVO = ObjectUtil.defaultIfNull(tableVoMap.get(tableName), new TableVO());
                    tableVO.setTableName(tableName);

                    List<ColumnDefinition> columns = createTable.getColumnDefinitions();
                    List<ColumnVO> columnList = columns.stream()
                            .map(columnDefinition -> {
                                ColumnVO columnVO = new ColumnVO();
                                columnVO.setTableName(tableName);
                                columnVO.setName(correctName(columnDefinition.getColumnName()));

                                ColDataType colDataType = columnDefinition.getColDataType();
                                columnVO.setType(DDLUtil.dialectTypeToJavaSqlType(colDataType.getDataType()));
                                columnVO.setTypeName(correctName(colDataType.getDataType()));
                                if (StrUtil.containsAnyIgnoreCase(colDataType.getDataType(), "serial", "bigserial")) {
                                    columnVO.setAutoIncrement(true);
                                }
                                if (CollUtil.isNotEmpty(colDataType.getArgumentsStringList())) {
                                    columnVO.setSize(Convert.toInt(colDataType.getArgumentsStringList().get(0)));
                                    if (colDataType.getArgumentsStringList().size() > 1) {
                                        columnVO.setDigit(Convert.toInt(colDataType.getArgumentsStringList().get(1)));
                                    }
                                }

                                columnVO.setNullable(true);
                                List<String> columnSpecs = columnDefinition.getColumnSpecs();
                                if (CollUtil.isNotEmpty(columnSpecs)) {
                                    for (int i = 0; i < columnSpecs.size(); i++) {
                                        if (StrUtil.equalsIgnoreCase("long", columnVO.getTypeName())
                                                && i == 0 && StrUtil.equalsIgnoreCase("raw", columnSpecs.get(i))) {
                                            columnVO.setType(Types.BINARY);
                                            columnVO.setTypeName("bytea");
                                        }
                                        if (StrUtil.equalsIgnoreCase("not", columnSpecs.get(i))
                                                && columnSpecs.size() > (i + 1) && StrUtil.equalsIgnoreCase("null", columnSpecs.get(i + 1))) {
                                            columnVO.setNullable(false);
                                        }
                                        if (StrUtil.equalsIgnoreCase("comment", columnSpecs.get(i))
                                                && columnSpecs.size() > (i + 1) && StrUtil.startWith(columnSpecs.get(i + 1), "'")) {
                                            columnVO.setComment(columnSpecs.get(i + 1).replace("'", ""));
                                        }
                                        if (StrUtil.equalsAnyIgnoreCase(columnSpecs.get(i), "auto_increment", "generated")) {
                                            columnVO.setAutoIncrement(true);
                                        }
                                        if (StrUtil.equalsIgnoreCase("default", columnSpecs.get(i))
                                                && columnSpecs.size() > (i + 1) && StrUtil.contains(columnSpecs.get(i + 1), "'")) {
                                            columnVO.setColumnDef(
                                                    columnSpecs.get(i + 1)
                                                    //.replace("'", "")
                                            );
                                        }
                                    }
                                }
                                return columnVO;
                            })
                            .collect(Collectors.toList());
                    tableVO.setColumnList(columnList);

                    List<Index> indexList = createTable.getIndexes();
                    List<IndexVO> indexVOList = new ArrayList<>();
                    if (CollUtil.isNotEmpty(indexList)) {
                        for (Index index : indexList) {
                            List<String> columnsNames = index.getColumnsNames().stream()
                                    .map(DDLUtil::correctName)
                                    .collect(Collectors.toList());
                            if (StrUtil.equalsIgnoreCase(index.getType(), "primary key")) {
                                for (ColumnVO columnVO : columnList) {
                                    if (CollUtil.contains(columnsNames, columnVO.getName())) {
                                        columnVO.setPk(true);
                                    }
                                }
                                index.setName("PRIMARY");
                            }
                            fillTableIndex(columnsNames, index, tableName, indexVOList);
                        }
                    }
                    if (CollUtil.isNotEmpty(indexVOList)) {
                        tableVO.setIndexList(indexVOList);
                    }

                    List<String> options = createTable.getTableOptionsStrings();
                    if (CollUtil.isNotEmpty(options)) {
                        for (int i = 0; i < options.size(); i++) {
                            if (StrUtil.equalsIgnoreCase("comment", options.get(i))) {
                                if (options.size() > (i + 1) && StrUtil.startWith(options.get(i + 1), "'")) {
                                    tableVO.setComment(options.get(i + 1).replace("'", ""));
                                }
                                if (options.size() > (i + 2) && StrUtil.contains(options.get(i + 1), "=")
                                        && StrUtil.startWith(options.get(i + 2), "'")) {
                                    tableVO.setComment(options.get(i + 2).replace("'", ""));
                                }
                            }
                        }
                    }

                    tableVoMap.put(tableVO.getTableName(), tableVO);
                    continue;
                }
                // 如果是注释语句
                if (statement instanceof Comment) {
                    Comment comment = (Comment) statement;
                    if (comment.getTable() != null && comment.getComment() != null) {
                        Table table = comment.getTable();
                        commentMap.put(correctName(table.getFullyQualifiedName()), comment.getComment().getValue());
                    }
                    if (comment.getColumn() != null && comment.getComment() != null) {
                        Column column = comment.getColumn();
                        commentMap.put(correctName(column.getFullyQualifiedName()), comment.getComment().getValue());
                    }
                    continue;
                }
                // 如果是建索引语句
                if (statement instanceof CreateIndex) {
                    CreateIndex createIndex = (CreateIndex) statement;
                    Table table = createIndex.getTable();
                    String tableName = correctName(table.getName());
                    List<IndexVO> indexVOList = indexListMap.get(tableName);
                    if (CollUtil.isEmpty(indexVOList)) {
                        indexVOList = new ArrayList<>();
                        indexListMap.put(tableName, indexVOList);
                    }
                    Index index = createIndex.getIndex();
                    List<String> columnsNames = index.getColumns().stream()
                            .map(columnParams -> {
                                String name = columnParams.getColumnName();
                                if (CollUtil.isNotEmpty(columnParams.getParams())) {
                                    name += columnParams.getParams().get(0);
                                }
                                return correctName(name);
                            })
                            .collect(Collectors.toList());

                    fillTableIndex(columnsNames, index, tableName, indexVOList);
                    continue;
                }
                // 如果是Drop语句
                if (statement instanceof Drop) {
                    Drop drop = (Drop) statement;
                    String tableName = correctName(drop.getName().getFullyQualifiedName());
                    fillTableInfo(tableVoMap, ExtendVO.withDropTable(tableName));
                    continue;
                }
                // 如果是Set语句
                if (statement instanceof SetStatement) {
                    throw new ServiceException("不解析SET语句，避免不同数据库类型不兼容");
                }
                fillTableInfo(tableVoMap, ExtendVO.withSourceSQL(ddl));
            } catch (Exception e) {
                fillTableInfo(tableVoMap, ExtendVO.withAbnormalDDL(ddl));
                log.error("异常DDL：{}，异常信息：{}", ddl, e.getMessage());
                log.error("解析失败", e);
            }
        }
        // 回填注释内容
        if (CollUtil.isNotEmpty(commentMap)) {
            for (String tableName : tableVoMap.keySet()) {
                TableVO tableVO = tableVoMap.get(tableName);
                if (StrUtil.isNotEmpty(commentMap.get(tableName))) {
                    tableVO.setComment(commentMap.get(tableName));
                }
                if (CollUtil.isNotEmpty(tableVO.getColumnList())) {
                    for (ColumnVO columnVO : tableVO.getColumnList()) {
                        String columnKey = tableName + "." + columnVO.getName();
                        if (StrUtil.isNotEmpty(commentMap.get(columnKey))) {
                            columnVO.setComment(commentMap.get(columnKey));
                        }
                    }
                }
            }
        }
        // 回填索引
        if (CollUtil.isNotEmpty(indexListMap)) {
            for (String tableName : tableVoMap.keySet()) {
                if (CollUtil.isNotEmpty(indexListMap.get(tableName))) {
                    TableVO tableVO = tableVoMap.get(tableName);
                    List<IndexVO> innerIndexVOList = tableVO.getIndexList();
                    if (CollUtil.isEmpty(innerIndexVOList)) {
                        innerIndexVOList = new ArrayList<>();
                    }
                    tableVO.setIndexList(CollUtil.addAllIfNotContains(innerIndexVOList, indexListMap.get(tableName)));
                }
            }
        }

        return new ArrayList<>(tableVoMap.values());
    }

    /**
     * 将数据库方言类型字符串转换为对应的 Java SQL 类型（java.sql.Types 常量）。
     * <p>
     * 该方法首先对输入的方言类型进行预处理，去除可能存在的前缀或后缀标记，
     * 然后根据类型名称匹配到标准的 JDBC SQL 类型常量。
     * </p>
     *
     * @param dialectType 数据库方言中的数据类型名称，例如 "VARCHAR"、"int4" 等
     * @return 对应的 java.sql.Types 中定义的 SQL 类型常量，如果未找到匹配项则返回 {@link java.sql.Types#OTHER}
     */
    public static int dialectTypeToJavaSqlType(String dialectType) {
        // 处理包含 "__point__" 标记的类型，截取其后部分作为实际类型名
        if (StrUtil.contains(dialectType, POINT_TAG)) {
            dialectType = StrUtil.subAfter(dialectType, POINT_TAG, true);
        }

        // 若类型以 "_" 开头，则移除前缀（通常用于数组类型）
        if (dialectType.startsWith("_")) {
            dialectType = StrUtil.removePrefix(dialectType, "_");
        }

        // 字符串相关类型映射
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "CHAR")) {
            return Types.CHAR;
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "VARCHAR")) {
            return Types.VARCHAR;
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "long", "text", "String", "LONGVARCHAR")) {
            return Types.LONGVARCHAR;
        }

        // 整数类型映射
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "serial", "int4", "int2", "int", "INTEGER")) {
            return Types.INTEGER;
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "bigserial", "int8", "Int16", "Int32", "Int64", "BIGINT")) {
            return Types.BIGINT;
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "TINYINT")) {
            return Types.TINYINT;
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "SMALLINT")) {
            return Types.SMALLINT;
        }

        // 浮点与数值类型映射
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "float8", "Float64", "DOUBLE")) {
            return Types.DOUBLE;
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "Float32", "FLOAT")) {
            return Types.FLOAT;
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "number", "numeric")) {
            return Types.NUMERIC;
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "DECIMAL")) {
            return Types.DECIMAL;
        }

        // 时间日期类型映射
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "DATE")) {
            return Types.DATE;
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "TIME")) {
            return Types.TIME;
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "datetime", "DateTime64", "TIMESTAMP")) {
            return Types.TIMESTAMP;
        }

        // 二进制与大对象类型映射
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "bytea", "FixedString", "BINARY")) {
            return Types.BINARY;
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "VARBINARY")) {
            return Types.VARBINARY;
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "LONGVARBINARY")) {
            return Types.LONGVARBINARY;
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "jsonb", "JAVA_OBJECT")) {
            return Types.JAVA_OBJECT;
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "BLOB")) {
            return Types.BLOB;
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "CLOB")) {
            return Types.CLOB;
        }

        // 布尔与位类型映射
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "bool", "UInt8", "BIT")) {
            return Types.BIT;
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "BOOLEAN")) {
            return Types.BOOLEAN;
        }

        // 其他浮点类型
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "REAL")) {
            return Types.REAL;
        }

//        if (StrUtil.equalsAnyIgnoreCase(dialectType, "geometry", "geography")) {
//            return Types.OTHER;
//        }
//        if (StrUtil.equalsAnyIgnoreCase(dialectType, "uuid")) {
//            return Types.OTHER;
//        }

        // 默认返回 OTHER 表示未知或未支持的类型
        return Types.OTHER;
    }


    /**
     * 填充表信息到映射中
     *
     * @param tableVoMap 表VO映射，用于存储表信息，key为表名，value为表VO对象
     * @param extendVO   扩展VO对象，包含表的扩展信息
     */
    private static void fillTableInfo(Map<String, TableVO> tableVoMap, ExtendVO extendVO) {
        // 创建新的表VO对象并设置基本信息
        TableVO tableVO = new TableVO();
        tableVO.setTableName(IdUtil.nanoId());
        tableVO.setExtend(extendVO);
        tableVoMap.put(tableVO.getTableName(), tableVO);
    }


    /**
     * 填充表索引信息到索引VO列表中
     *
     * @param columnsNames 列名列表
     * @param index        索引对象
     * @param tableName    表名
     * @param indexVOList  索引VO列表
     */
    private static void fillTableIndex(List<String> columnsNames, Index index, String tableName, List<IndexVO> indexVOList) {
        IndexVO indexVO = new IndexVO();
        // 设置是否为非唯一索引
        indexVO.setNonUnique(!StrUtil.containsIgnoreCase(index.getType(), "unique"));
        // 设置索引名称
        if (StrUtil.isNotEmpty(index.getName())) {
            indexVO.setIndexName(correctName(index.getName()));
        } else {
            indexVO.setIndexName(
                    (indexVO.isNonUnique() ? "unique_idx_" : "idx_")
                            + tableName
                            + "_"
                            + CollUtil.join(columnsNames, "_")
            );
        }
        indexVO.setTableName(tableName);
        indexVO.setColumnList(Convert.toStrArray(columnsNames));
        indexVOList.add(indexVO);
    }


    /**
     * 修正名称字符串，移除特殊字符并替换特定标记
     *
     * @param name 需要修正的原始名称字符串
     * @return 修正后的名称字符串
     */
    private static String correctName(String name) {
        // 移除反引号和双引号，然后将 "__point__" 替换为 "."
        return name.replaceAll("[`\"]", "").replace(POINT_TAG, ".");
    }

}
