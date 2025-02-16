package org.wsitm.rdbms.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
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
import org.wsitm.rdbms.constant.DialectEnum;
import org.wsitm.rdbms.constant.RdbmsConstants;
import org.wsitm.rdbms.dialects.DDLCreateUtils;
import org.wsitm.rdbms.entity.vo.*;
import org.wsitm.rdbms.exception.ServiceException;

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
                        if (StrUtil.startWithAnyIgnoreCase(dialect.getName(), "Mysql", "MariaDB", "ClickHouse")) {
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
                    if (columnVO.isAutoIncrement()) {
                        columnModel.setIdGenerationType(GenerationType.IDENTITY);
                    }

                    // columnModel.setColumnType(colDef2DialectType(dialect, column.getTypeName()));
                    columnModel.setColumnType(javaSqlTypeToDialectType(dialect, columnVO.getType()));
                    if (ListUtil.of(1111, 2003).contains(columnVO.getType())) {
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
                        } else if (ObjectUtil.equals(Types.BIGINT, columnVO.getType())) {
                            columnModel.setLength(22);
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
                                        indexModel.setName(parcelName(dialect, indexVO.getIndexName()));
                                        indexModel.setUnique(indexVO.isNonUnique());
                                        indexModel.setColumnList(indexVO.getColumnList());
                                        return indexModel;
                                    })
                                    .collect(Collectors.toList())
                    );
                }

                // TODO 补充mysql的表描述
                if (StrUtil.isNotEmpty(tableModel.getComment())) {
                    if (StrUtil.startWithAnyIgnoreCase(dialect.getName(), "Mysql", "MariaDB")) {
                        tableModel.setEngineTail(
                                StrUtil.emptyToDefault(tableModel.getEngineTail(), "") + String.format(" comment='%s'", tableModel.getComment()));
                    }
                    if (StrUtil.startWithAnyIgnoreCase(dialect.getName(), "ClickHouse")) {
                        tableModel.setTableTail(
                                StrUtil.emptyToDefault(tableModel.getEngineTail(), "") + String.format(" comment '%s'", tableModel.getComment()));
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
     * 判断名称是否为保留字段，如果是保留字段则以 引号 包含
     *
     * @param dialect 方言
     * @param name    字段名
     * @return 格式字段
     */
    public static String parcelName(Dialect dialect, String name) {
        boolean reserved = ReservedDBWords.isReservedWord(name);
        if (reserved || !RdbmsConstants.RDBMS_PATTERN.matcher(name).find()) {
            String dialectName = dialect.getName();
            String strFmt = StrUtil.startWithAnyIgnoreCase(dialectName, "MySQL", "MariaDB", "ClickHouse") ? "`%s`" : "\"%s\"";
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
     * Convert java.sql.Types.xxx type to Dialect's Type
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
        Set<String> dropSet = new HashSet<>();
        Map<String, String> commentMap = new HashMap<>();
        Map<String, List<IndexVO>> indexListMap = new HashMap<>();
        for (String ddl : arrDDL) {
            if (StrUtil.isEmpty(ddl.trim())) {
                continue;
            }
            try {
                String marker = "geometry|geography|_geometry|st_geomfromtext|st_setsrid|st_makepoint";
                String ddl2 = ddl.replaceAll("(\\s+)([^\\s]*\\.)(?=(?i)(" + marker + "))", " ");

                if (StrUtil.startWithAnyIgnoreCase(ddl2.trim(), "create table")
                        && StrUtil.containsIgnoreCase(ddl2, "unique index")) {
                    ddl2 = ddl.replaceAll("(?<=(?i)UNIQUE).*?(?=\\()", " ");
                }

                // 解析SQL语句
                Statement statement = CCJSqlParserUtil.parse(ddl2);
                // 如果是建表语句
                if (statement instanceof CreateTable) {
                    CreateTable createTable = (CreateTable) statement;
                    // 提取表名
                    Table table = createTable.getTable();
                    String tableName = table.getName().replaceAll("[`\"]", "");

                    TableVO tableVO = ObjectUtil.defaultIfNull(tableVoMap.get(tableName), new TableVO());
                    tableVO.setTableName(tableName);

                    List<ColumnDefinition> columns = createTable.getColumnDefinitions();
                    List<ColumnVO> columnList = columns.stream()
                            .map(columnDefinition -> {
                                ColumnVO columnVO = new ColumnVO();
                                columnVO.setTableName(tableName);
                                columnVO.setName(columnDefinition.getColumnName().replaceAll("[`\"]", ""));

                                ColDataType colDataType = columnDefinition.getColDataType();
                                columnVO.setType(DDLUtil.dialectTypeToJavaSqlType(colDataType.getDataType()));
                                columnVO.setTypeName(colDataType.getDataType());
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
                                                && columnSpecs.size() > (i + 1) && StrUtil.contains(columnSpecs.get(i + 1), "'")) {
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
                                    .map(column -> column.replaceAll("[`\"]", ""))
                                    .collect(Collectors.toList());
                            if (StrUtil.equalsIgnoreCase(index.getType(), "primary key")) {
                                for (ColumnVO columnVO : columnList) {
                                    if (CollUtil.contains(columnsNames, columnVO.getName())) {
                                        columnVO.setPk(true);
                                    }
                                }
                            } else {
                                IndexVO indexVO = new IndexVO();
                                indexVO.setNonUnique(StrUtil.containsIgnoreCase(index.getType(), "unique"));
                                if (StrUtil.isNotEmpty(index.getName())) {
                                    indexVO.setIndexName(index.getName().replaceAll("[`\"]", ""));
                                } else {
                                    indexVO.setIndexName((indexVO.isNonUnique() ? "unique_idx_" : "idx_") + tableName
                                            + "_" + CollUtil.join(columnsNames, "_"));
                                }
                                indexVO.setTableName(tableName);
                                indexVO.setColumnList(Convert.toStrArray(columnsNames));
                                indexVOList.add(indexVO);
                            }
                        }
                    }
                    if (CollUtil.isNotEmpty(indexVOList)) {
                        tableVO.setIndexList(indexVOList);
                    }

                    List<String> options = createTable.getTableOptionsStrings();
                    if (CollUtil.isNotEmpty(options)) {
                        for (int i = 0; i < options.size(); i++) {
                            if (StrUtil.equalsIgnoreCase("comment", options.get(i)) && options.size() > (i + 2)
                                    && StrUtil.contains(options.get(i + 1), "=")
                                    && StrUtil.contains(options.get(i + 2), "'")) {
                                tableVO.setComment(options.get(i + 2).replace("'", ""));
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
                        commentMap.put(table.getFullyQualifiedName().replaceAll("[`\"]", ""), comment.getComment().getValue());
                    }
                    if (comment.getColumn() != null && comment.getComment() != null) {
                        Column column = comment.getColumn();
                        commentMap.put(column.getFullyQualifiedName().replaceAll("[`\"]", ""), comment.getComment().getValue());
                    }
                    continue;
                }
                // 如果是建索引语句
                if (statement instanceof CreateIndex) {
                    CreateIndex createIndex = (CreateIndex) statement;
                    Table table = createIndex.getTable();
                    String tableName = table.getName().replaceAll("[`\"]", "");
                    List<IndexVO> indexVOList = indexListMap.get(tableName);
                    if (CollUtil.isEmpty(indexVOList)) {
                        indexVOList = new ArrayList<>();
                        indexListMap.put(tableName, indexVOList);
                    }
                    Index index = createIndex.getIndex();
                    List<String> columnsNames = index.getColumnsNames().stream()
                            .map(column -> column.replaceAll("[`\"]", ""))
                            .collect(Collectors.toList());

                    IndexVO indexVO = new IndexVO();
                    indexVO.setNonUnique(StrUtil.containsIgnoreCase(index.getType(), "unique"));
                    if (StrUtil.isNotEmpty(index.getName())) {
                        indexVO.setIndexName(index.getName().replaceAll("[`\"]", ""));
                    } else {
                        indexVO.setIndexName((indexVO.isNonUnique() ? "unique_idx_" : "idx_") + tableName
                                + "_" + CollUtil.join(columnsNames, "_"));
                    }
                    indexVO.setTableName(tableName);
                    indexVO.setColumnList(Convert.toStrArray(columnsNames));
                    indexVOList.add(indexVO);
                    continue;
                }
                // 如果是Drop语句
                if (statement instanceof Drop) {
                    Drop drop = (Drop) statement;
                    String tableName = drop.getName().getFullyQualifiedName().replaceAll("[`\"]", "");
                    fillTableInfo(tableVoMap, ExtendVO.withDropTable(tableName));
                    continue;
                }
                // 如果是Set语句
                if (statement instanceof SetStatement) {
                    throw new ServiceException("不解析SET语句，避免不同数据库类型不兼容");
                }
                fillTableInfo(tableVoMap, ExtendVO.withSourceSQL(ddl));
            } catch (Exception exception) {
                fillTableInfo(tableVoMap, ExtendVO.withAbnormalDDL(ddl));
                log.error("异常DDL: " + ddl);
                log.error("解析失败", exception);
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

    public static int dialectTypeToJavaSqlType(String dialectType) {
        if (dialectType.startsWith("_")) {
            dialectType = StrUtil.removePrefix(dialectType, "_");
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "serial", "int4", "int2", "int")) {
            return Types.INTEGER;
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "bigserial", "int8")) {
            return Types.BIGINT;
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "float8")) {
            return Types.DOUBLE;
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "number", "numeric")) {
            return Types.NUMERIC;
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "datetime")) {
            return Types.TIMESTAMP;
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "long", "text")) {
            return Types.LONGVARCHAR;
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "jsonb")) {
            return Types.JAVA_OBJECT;
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "geometry", "geography")) {
            return Types.OTHER;
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "bytea")) {
            return Types.BINARY;
        }
        if (StrUtil.equalsAnyIgnoreCase(dialectType, "bool")) {
            return Types.BIT;
        }
        Type type = Type.getByTypeName(dialectType);
        switch (type) {
            case BIT:
                return Types.BIT;
            case TINYINT:
                return Types.TINYINT;
            case SMALLINT:
                return Types.SMALLINT;
            case INTEGER:
                return Types.INTEGER;
            case BIGINT:
                return Types.BIGINT;
            case FLOAT:
                return Types.FLOAT;
            case REAL:
                return Types.REAL;
            case DOUBLE:
                return Types.DOUBLE;
            case NUMERIC:
                return Types.NUMERIC;
            case DECIMAL:
                return Types.DECIMAL;
            case CHAR:
                return Types.CHAR;
            case VARCHAR:
                return Types.VARCHAR;
            case LONGVARCHAR:
                return Types.LONGVARCHAR;
            case DATE:
                return Types.DATE;
            case TIME:
                return Types.TIME;
            case TIMESTAMP:
                return Types.TIMESTAMP;
            case BINARY:
                return Types.BINARY;
            case VARBINARY:
                return Types.VARBINARY;
            case LONGVARBINARY:
                return Types.LONGVARBINARY;
            case JAVA_OBJECT:
                return Types.JAVA_OBJECT;
            case BLOB:
                return Types.BLOB;
            case CLOB:
                return Types.CLOB;
            case BOOLEAN:
                return Types.BOOLEAN;

            case UNKNOW:
                return Types.OTHER;
            /* JAVA8_END */
            default:
                return Types.OTHER;
//                throw new DialectException("Unsupported Types:" + dialectType);
        }
    }


    private static void fillTableInfo(Map<String, TableVO> tableVoMap, ExtendVO extendVO) {
        TableVO tableVO = new TableVO();
        tableVO.setTableName(IdUtil.nanoId());
        tableVO.setExtend(extendVO);
        tableVoMap.put(tableVO.getTableName(), tableVO);
    }
}
