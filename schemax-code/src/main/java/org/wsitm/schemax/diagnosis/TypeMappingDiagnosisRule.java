package org.wsitm.schemax.diagnosis;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.wsitm.schemax.entity.vo.ColumnVO;
import org.wsitm.schemax.entity.vo.TypeMappingResult;
import org.wsitm.schemax.service.ITypeMappingRuleService;
import org.wsitm.schemax.utils.DDLUtil;

import java.sql.Types;

@Order(20)
@Component
public class TypeMappingDiagnosisRule implements DdlDiagnosisRule {
    private final ITypeMappingRuleService typeMappingRuleService;

    public TypeMappingDiagnosisRule(ITypeMappingRuleService typeMappingRuleService) {
        this.typeMappingRuleService = typeMappingRuleService;
    }

    @Override
    public void diagnose(DdlDiagnosisContext context) {
        if (!(context.getStatement() instanceof CreateTable createTable)
                || CollUtil.isEmpty(createTable.getColumnDefinitions())) {
            return;
        }
        for (ColumnDefinition definition : createTable.getColumnDefinitions()) {
            diagnoseColumn(context, definition);
        }
    }

    private void diagnoseColumn(DdlDiagnosisContext context, ColumnDefinition definition) {
        String columnName = DdlDiagnosisContext.normalizeIdentifier(definition.getColumnName());
        ColDataType dataType = definition.getColDataType();
        String typeName = DdlDiagnosisContext.normalizeIdentifier(dataType.getDataType());

        ColumnVO column = new ColumnVO();
        column.setTableName(context.getTableName());
        column.setName(columnName);
        column.setTypeName(typeName);
        column.setType(DDLUtil.dialectTypeToJavaSqlType(typeName));
        if (CollUtil.isNotEmpty(dataType.getArgumentsStringList())) {
            column.setSize(Convert.toLong(dataType.getArgumentsStringList().get(0), 0L));
            if (dataType.getArgumentsStringList().size() > 1) {
                column.setDigit(Convert.toInt(dataType.getArgumentsStringList().get(1), null));
            }
        }

        TypeMappingResult mapping = typeMappingRuleService.map(column,
                context.getRequest().getSourceDatabase(), context.getRequest().getOutputDatabase());
        if (mapping.isMatched()) {
            String reason = "已命中类型映射规则“" + mapping.getRuleName() + "”。";
            String suggestion = "目标字段将使用 " + mapping.getTypeExpression() + "，请核对长度、精度和小数位。";
            context.addIssueAtColumn("TYPE_MAPPING_APPLIED", "INFO", "TYPE", columnName,
                    "字段类型 " + typeName + " 将映射为 " + mapping.getTypeExpression(), reason, suggestion);
            return;
        }

        boolean sameFamily = databaseFamily(context.getRequest().getSourceDatabase())
                .equals(databaseFamily(context.getRequest().getOutputDatabase()));
        if (column.getType() == Types.OTHER) {
            String level = sameFamily ? "WARNING" : "ERROR";
            String reason = sameFamily
                    ? "当前解析器无法确认该数据库扩展类型的转换能力。"
                    : "该类型没有标准 JDBC 映射，也没有命中已配置的类型映射规则。";
            String suggestion = sameFamily
                    ? "请核对目标数据库是否支持该类型，或新增明确的类型映射规则。"
                    : "请在类型映射规则中配置 " + typeName + " 到目标数据库类型的映射。";
            context.addIssueAtColumn("UNSUPPORTED_DATA_TYPE", level, "TYPE", columnName,
                    "字段类型 " + typeName + " 无法确认转换结果", reason, suggestion);
        }

        String sourceFamily = databaseFamily(context.getRequest().getSourceDatabase());
        String targetFamily = databaseFamily(context.getRequest().getOutputDatabase());
        if ("oracle".equals(sourceFamily) && "postgresql".equals(targetFamily)
                && StrUtil.equalsIgnoreCase(typeName, "NUMBER")) {
            context.addIssueAtColumn("NUMBER_SEMANTIC_DIFFERENCE", "WARNING", "TYPE", columnName,
                    "Oracle NUMBER 转 PostgreSQL 存在精度语义差异",
                    "未声明精度的 NUMBER 可同时承载整数和小数，目标端自动推断可能与原库不同。",
                    "建议显式映射为 numeric(p,s)；整型字段可按实际范围改为 integer 或 bigint。" );
        }
        if ("oracle".equals(sourceFamily) && !"oracle".equals(targetFamily)
                && StrUtil.equalsIgnoreCase(typeName, "DATE")) {
            String targetType = "mysql".equals(targetFamily) ? "datetime" : "timestamp";
            context.addIssueAtColumn("ORACLE_DATE_SEMANTIC_DIFFERENCE", "WARNING", "TYPE", columnName,
                    "Oracle DATE 包含时间信息",
                    "目标数据库的 DATE 通常只保存日期，直接转换可能丢失时分秒。",
                    "如原字段包含时间，请映射为 " + targetType + "；仅保存日期时再使用 date。" );
        }
    }

    private String databaseFamily(String database) {
        String value = StrUtil.blankToDefault(database, "").toLowerCase();
        if (value.contains("mariadb") || value.contains("mysql")) {
            return "mysql";
        }
        if (value.contains("postgres")) {
            return "postgresql";
        }
        if (value.contains("oracle")) {
            return "oracle";
        }
        if (value.contains("sqlserver")) {
            return "sqlserver";
        }
        return value;
    }
}
