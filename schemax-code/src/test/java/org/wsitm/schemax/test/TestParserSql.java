package org.wsitm.schemax.test;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.comment.Comment;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.Index;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wsitm.schemax.entity.vo.ColumnVO;
import org.wsitm.schemax.entity.vo.IndexVO;
import org.wsitm.schemax.entity.vo.TableVO;
import org.wsitm.schemax.utils.DDLUtil;
import org.wsitm.schemax.utils.JsonUtil;

import java.util.*;
import java.util.stream.Collectors;

public class TestParserSql {
    private static final Logger log = LoggerFactory.getLogger(TestParserSql.class);

    public static final String DDL1 = "CREATE TABLE `d_jk_scene` (\n" +
            "  `intId` bigint(20) NOT NULL AUTO_INCREMENT,\n" +
            "  `vcSceneName` varchar(200) DEFAULT '0' COMMENT '场景名称',\n" +
            "  `intNetCityId` bigint(20) DEFAULT '0' COMMENT '业务发生市编码，86020',\n" +
            "  `intStatus` int(11) DEFAULT '0' COMMENT '场景状态，0表示场景审核中,-1：删除，3，已跑数',\n" +
            "  `dtUpdateTime` datetime DEFAULT NULL COMMENT '更新场景时间',\n" +
            "  `dtcreatetime` datetime DEFAULT NULL COMMENT '创建场景时间',\n" +
            "  `vccreateuser` varchar(200) DEFAULT NULL COMMENT '创建用户',\n" +
            "  PRIMARY KEY (`intId`),\n" +
            "  UNIQUE KEY `d_jk_scene_vcSceneName_IDX` (`vcSceneName`) USING BTREE,\n" +
            "  KEY `d_jk_scene_intNetCityId_IDX` (`intNetCityId`) USING BTREE\n" +
            ") ENGINE=InnoDB AUTO_INCREMENT=176 DEFAULT CHARSET=utf8mb4 COMMENT='场景定义表';";

    @Test
    public void test1() {
        try {
            // 解析SQL语句
            Statement statement = CCJSqlParserUtil.parse(DDL1);

            // 检查语句类型并处理
            if (statement instanceof CreateTable) {
                CreateTable createTable = (CreateTable) statement;

                // 提取表名
                Table table = createTable.getTable();
                System.out.println(JsonUtil.toJSONString(createTable));
//                System.out.println("Table Name: " + table.getName() + ", " + table.getAlias());

                // 提取列定义
//                List<ColumnDefinition> columns = createTable.getColumnDefinitions();
//                for (ColumnDefinition column : columns) {
//                    System.out.printf("Column: %s; %s; %s%n",
//                            column.getColumnName(),
//                            JsonUtil.toJSONString(column.getColDataType()),
//                            JsonUtil.toJSONString(column.getColumnSpecs())
//                    );
//                }
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }


    public static final String DDL2 = "create table\n" +
            "  dim_connect_info (\n" +
            "    connect_id serial not null,\n" +
            "    connect_name varchar(200) not null,\n" +
            "    jdbc_id int4 not null,\n" +
            "    jdbc_url varchar(2048) not null,\n" +
            "    username varchar(256) not null,\n" +
            "    password varchar(256) not null,\n" +
            "    create_by varchar(100),\n" +
            "    create_time timestamp,\n" +
            "    update_by varchar(100),\n" +
            "    update_time timestamp,\n" +
            "    primary key (connect_id)\n" +
            "  );\n" +
            "comment on table dim_connect_info is '连接配置表';\n" +
            "comment on column dim_connect_info.connect_id is '连接ID';\n" +
            "comment on column dim_connect_info.connect_name is '连接名称';\n" +
            "comment on column dim_connect_info.jdbc_id is '驱动ID';\n" +
            "comment on column dim_connect_info.jdbc_url is 'JDBC URL';\n" +
            "comment on column dim_connect_info.username is '用户';\n" +
            "comment on column dim_connect_info.password is '密码';\n" +
            "comment on column dim_connect_info.create_by is '创建用户';\n" +
            "comment on column dim_connect_info.create_time is '创建时间';\n" +
            "comment on column dim_connect_info.update_by is '修改用户';\n" +
            "comment on column dim_connect_info.update_time is '修改时间';";

    @Test
    public void test2() {
        String[] arrDDL = StrUtil.splitToArray(DDL1, ";");
        Map<String, TableVO> tableVoMap = new LinkedHashMap<>();
        Map<String, String> commentMap = new HashMap<>();

        for (String ddl : arrDDL) {
            if (StrUtil.isEmpty(ddl)) {
                continue;
            }
            try {
                // 解析SQL语句
                Statement statement = CCJSqlParserUtil.parse(ddl);

                if (statement instanceof CreateTable) {
                    CreateTable createTable = (CreateTable) statement;
                    // 提取表名
                    Table table = createTable.getTable();

                    TableVO tableVO = ObjectUtil.defaultIfNull(tableVoMap.get(table.getName()), new TableVO());
                    tableVO.setTableName(table.getName().replaceAll("[`\"]", ""));

                    List<ColumnDefinition> columns = createTable.getColumnDefinitions();
                    List<ColumnVO> columnList = columns.stream()
                            .map(columnDefinition -> {
                                ColumnVO columnVO = new ColumnVO();
                                columnVO.setTableName(table.getName().replaceAll("[`\"]", ""));
                                columnVO.setName(columnDefinition.getColumnName().replaceAll("[`\"]", ""));

                                ColDataType colDataType = columnDefinition.getColDataType();
                                columnVO.setType(DDLUtil.dialectTypeToJavaSqlType(colDataType.getDataType()));
                                columnVO.setTypeName(colDataType.getDataType());
                                if (StrUtil.containsIgnoreCase(colDataType.getDataType(), "serial")) {
                                    columnVO.setAutoIncrement(true);
                                }
                                if (CollUtil.isNotEmpty(colDataType.getArgumentsStringList())) {
                                    columnVO.setSize(Convert.toInt(colDataType.getArgumentsStringList().get(0)));
                                    if (colDataType.getArgumentsStringList().size() > 1) {
                                        columnVO.setDigit(Convert.toInt(colDataType.getArgumentsStringList().get(1)));
                                    }
                                }

                                List<String> definitionList = columnDefinition.getColumnSpecs();
                                if (CollUtil.isNotEmpty(definitionList)) {
                                    for (int i = 0; i < definitionList.size(); i++) {
                                        if (StrUtil.equalsIgnoreCase("not", definitionList.get(i))
                                                && definitionList.size() > (i + 1) && StrUtil.equalsIgnoreCase("null", definitionList.get(i + 1))) {
                                            columnVO.setNullable(true);
                                        }
                                        if (StrUtil.equalsIgnoreCase("comment", definitionList.get(i))
                                                && definitionList.size() > (i + 1) && StrUtil.contains(definitionList.get(i + 1), "'")) {
                                            columnVO.setComment(definitionList.get(i + 1).replace("'", ""));
                                        }
                                        if (StrUtil.equalsIgnoreCase("auto_increment", definitionList.get(i))) {
                                            columnVO.setAutoIncrement(true);
                                        }
                                        if (StrUtil.equalsIgnoreCase("default", definitionList.get(i))
                                                && definitionList.size() > (i + 1) && StrUtil.contains(definitionList.get(i + 1), "'")) {
                                            columnVO.setColumnDef(definitionList.get(i + 1).replace("'", ""));
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
                            if (StrUtil.equalsIgnoreCase(index.getType(), "primary key")) {
                                for (ColumnVO columnVO : columnList) {
                                    if (CollUtil.contains(index.getColumnsNames(), columnVO.getName())) {
                                        columnVO.setPk(true);
                                    }
                                }
                            } else {
                                IndexVO indexVO = new IndexVO();
                                indexVO.setNonUnique(
                                        StrUtil.containsIgnoreCase(index.getType(), "unique"));
                                indexVO.setIndexName(index.getName().replaceAll("[`\"]", ""));
                                indexVO.setTableName(table.getName().replaceAll("[`\"]", ""));
                                indexVO.setColumnList(Convert.toStrArray(index.getColumnsNames()));
                                indexVOList.add(indexVO);
                            }
                        }
                    }
                    if (CollUtil.isNotEmpty(indexVOList)) {
                        tableVO.setIndexList(indexVOList);
                    }

                    List<String> options = createTable.getTableOptionsStrings();
                    for (int i = 0; i < options.size(); i++) {
                        if (StrUtil.equalsIgnoreCase("comment", options.get(i)) && options.size() > (i + 2)
                                && StrUtil.contains(options.get(i + 1), "=")
                                && StrUtil.contains(options.get(i + 2), "'")) {
                            tableVO.setComment(options.get(i + 2).replace("'", ""));
                        }
                    }

                    tableVoMap.put(tableVO.getTableName(), tableVO);
                }

                if (statement instanceof Comment) {
                    Comment comment = (Comment) statement;
                    if (comment.getTable() != null && comment.getComment() != null) {
                        Table table = comment.getTable();
                        commentMap.put(table.getFullyQualifiedName(), comment.getComment().getValue());
                    }
                    if (comment.getColumn() != null && comment.getComment() != null) {
                        Column column = comment.getColumn();
                        commentMap.put(column.getFullyQualifiedName(), comment.getComment().getValue());
                    }
                }
            } catch (JSQLParserException jsqlParserException) {
                log.error("", jsqlParserException);
            }
        }
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

        System.out.println(JsonUtil.toJSONString(tableVoMap));
    }

}
