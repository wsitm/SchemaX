package org.wsitm.schemax.diagnosis;

import cn.hutool.core.util.StrUtil;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Order(30)
@Component
public class DialectCompatibilityDiagnosisRule implements DdlDiagnosisRule {
    @Override
    public void diagnose(DdlDiagnosisContext context) {
        if (!(context.getStatement() instanceof CreateTable)) {
            return;
        }
        String sql = context.getSegment().getSql();
        String upperSql = sql.toUpperCase(Locale.ROOT);
        String sourceFamily = databaseFamily(context.getRequest().getSourceDatabase());
        String targetFamily = databaseFamily(context.getRequest().getOutputDatabase());

        if (upperSql.contains("PARTITION BY")) {
            context.addIssueAtToken("PARTITION_SYNTAX", "WARNING", "PARTITION", null,
                    "检测到分区表语法",
                    "各数据库的分区策略、边界表达式和子分区语法差异较大，当前转换不会完整重建分区定义。",
                    "先转换基础表结构，再根据目标数据库能力单独编写和验证分区语句。", "PARTITION BY");
        }
        if (!"mysql".equals(targetFamily) && upperSql.contains("UNSIGNED")) {
            context.addIssueAtToken("UNSIGNED_SEMANTIC_DIFFERENCE", "WARNING", "DIALECT", null,
                    "目标数据库不支持 MySQL UNSIGNED 语义",
                    "移除 UNSIGNED 后可表示的正整数上限会发生变化。",
                    "建议扩大目标整数类型，或增加非负检查约束。", "UNSIGNED");
        }
        if (!"mysql".equals(targetFamily) && upperSql.contains("AUTO_INCREMENT")) {
            context.addIssueAtToken("AUTO_INCREMENT_DIFFERENCE", "WARNING", "DIALECT", null,
                    "自增字段语法需要按目标方言转换",
                    "AUTO_INCREMENT 是 MySQL 方言，其他数据库通常使用 identity、sequence 或 serial。",
                    "请核对生成结果中的自增策略及序列起始值。", "AUTO_INCREMENT");
        }
        if (!"mysql".equals(targetFamily)
                && (upperSql.contains("ENGINE=") || upperSql.contains("CHARSET=")
                || upperSql.contains("CHARACTER SET") || upperSql.contains("COLLATE"))) {
            String token = upperSql.contains("ENGINE=") ? "ENGINE="
                    : upperSql.contains("CHARSET=") ? "CHARSET="
                    : upperSql.contains("CHARACTER SET") ? "CHARACTER SET" : "COLLATE";
            context.addIssueAtToken("MYSQL_TABLE_OPTION", "WARNING", "DIALECT", null,
                    "MySQL 表选项不会直接迁移到目标数据库",
                    "存储引擎、字符集和排序规则在不同数据库中的配置位置与语义不同。",
                    "请在目标库按实例、模式或字段级规则重新配置字符集与排序规则。", token);
        }
        diagnoseDefaultFunction(context, upperSql, sourceFamily, targetFamily);
    }

    private void diagnoseDefaultFunction(DdlDiagnosisContext context, String upperSql,
                                         String sourceFamily, String targetFamily) {
        if (upperSql.contains("SYSDATE") && !"oracle".equals(targetFamily)) {
            context.addIssueAtToken("DEFAULT_FUNCTION_DIFFERENCE", "WARNING", "DEFAULT_VALUE", null,
                    "默认值函数 SYSDATE 需要转换",
                    "SYSDATE 的返回类型和求值语义是 Oracle 方言特性。",
                    "通常可改为 CURRENT_TIMESTAMP，并核对时区语义。", "SYSDATE");
        }
        if (upperSql.contains("GETDATE()") && !"sqlserver".equals(targetFamily)) {
            context.addIssueAtToken("DEFAULT_FUNCTION_DIFFERENCE", "WARNING", "DEFAULT_VALUE", null,
                    "默认值函数 GETDATE() 需要转换",
                    "GETDATE() 是 SQL Server 方言函数。",
                    "通常可改为 CURRENT_TIMESTAMP，并核对目标字段精度。", "GETDATE()");
        }
        if (upperSql.contains("NOW()") && "oracle".equals(targetFamily)) {
            context.addIssueAtToken("DEFAULT_FUNCTION_DIFFERENCE", "WARNING", "DEFAULT_VALUE", null,
                    "默认值函数 NOW() 需要转换",
                    "NOW() 不是 Oracle 的通用默认值函数。",
                    "建议改为 CURRENT_TIMESTAMP 或 SYSTIMESTAMP。", "NOW()");
        }
        if (upperSql.contains("UUID()") && "postgresql".equals(targetFamily)) {
            context.addIssueAtToken("DEFAULT_FUNCTION_DIFFERENCE", "WARNING", "DEFAULT_VALUE", null,
                    "默认值函数 UUID() 需要转换",
                    "PostgreSQL 不提供与 MySQL UUID() 完全同名的内置默认值函数。",
                    "启用对应扩展后可使用 gen_random_uuid()，并将字段类型设为 uuid。", "UUID()");
        }
        if (upperSql.contains("NEWID()") && !"sqlserver".equals(targetFamily)) {
            context.addIssueAtToken("DEFAULT_FUNCTION_DIFFERENCE", "WARNING", "DEFAULT_VALUE", null,
                    "默认值函数 NEWID() 需要转换",
                    "NEWID() 是 SQL Server 的 UUID 生成函数。",
                    "请改用目标数据库的 UUID 类型与生成函数。", "NEWID()");
        }
        if (!sourceFamily.equals(targetFamily) && upperSql.contains("ON UPDATE CURRENT_TIMESTAMP")) {
            context.addIssueAtToken("ON_UPDATE_DIFFERENCE", "WARNING", "DEFAULT_VALUE", null,
                    "ON UPDATE 自动更新时间语义需要重建",
                    "该语法并非所有目标数据库都支持，转换器不会自动创建触发器。",
                    "请按目标数据库能力使用生成列、触发器或应用层更新时间。", "ON UPDATE CURRENT_TIMESTAMP");
        }
    }

    private String databaseFamily(String database) {
        String value = StrUtil.blankToDefault(database, "").toLowerCase(Locale.ROOT);
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
