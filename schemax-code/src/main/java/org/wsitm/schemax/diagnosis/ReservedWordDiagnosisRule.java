package org.wsitm.schemax.diagnosis;

import com.github.drinkjava2.jdialects.ReservedDBWords;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(10)
@Component
public class ReservedWordDiagnosisRule implements DdlDiagnosisRule {
    @Override
    public void diagnose(DdlDiagnosisContext context) {
        checkIdentifier(context, context.getTableName(), null);
        if (context.getStatement() instanceof CreateTable createTable
                && createTable.getColumnDefinitions() != null) {
            for (ColumnDefinition column : createTable.getColumnDefinitions()) {
                String columnName = DdlDiagnosisContext.normalizeIdentifier(column.getColumnName());
                checkIdentifier(context, columnName, columnName);
            }
        }
    }

    private void checkIdentifier(DdlDiagnosisContext context, String identifier, String columnName) {
        if (identifier == null || identifier.isBlank()) {
            return;
        }
        String simpleName = identifier.contains(".")
                ? identifier.substring(identifier.lastIndexOf('.') + 1) : identifier;
        if (!ReservedDBWords.isReservedWord(simpleName)) {
            return;
        }
        String objectName = columnName == null ? "表名" : "字段名";
        String message = objectName + "“" + simpleName + "”是数据库保留字";
        String reason = "不同数据库对该标识符的解释可能不同，未引用时可能导致建表失败。";
        String suggestion = "转换结果会按目标方言自动引用，仍建议将其改为非保留字名称。";
        if (columnName == null) {
            context.addIssueAtToken("RESERVED_WORD", "WARNING", "RESERVED_WORD", null,
                    message, reason, suggestion, simpleName);
        } else {
            context.addIssueAtColumn("RESERVED_WORD", "WARNING", "RESERVED_WORD", columnName,
                    message, reason, suggestion);
        }
    }
}
