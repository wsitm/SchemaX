package org.wsitm.schemax.service.impl;

import cn.hutool.core.util.StrUtil;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.SetStatement;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.comment.Comment;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.drop.Drop;
import org.springframework.stereotype.Service;
import org.wsitm.schemax.diagnosis.DdlDiagnosisContext;
import org.wsitm.schemax.diagnosis.DdlDiagnosisRule;
import org.wsitm.schemax.entity.vo.DdlCheckRequestVO;
import org.wsitm.schemax.entity.vo.DdlCheckResultVO;
import org.wsitm.schemax.entity.vo.DdlIssueVO;
import org.wsitm.schemax.entity.vo.DdlStatementCheckVO;
import org.wsitm.schemax.entity.vo.DdlTableCheckVO;
import org.wsitm.schemax.exception.ServiceException;
import org.wsitm.schemax.service.IDdlDiagnosisService;
import org.wsitm.schemax.utils.DdlParserSupport;
import org.wsitm.schemax.utils.DdlStatementSplitter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DdlDiagnosisServiceImpl implements IDdlDiagnosisService {
    private static final Pattern PARSER_POSITION = Pattern.compile(
            "(?i)line\\s+(\\d+)\\s*,\\s*column\\s+(\\d+)");
    private static final Pattern TABLE_NAME_FALLBACK = Pattern.compile(
            "(?is)\\b(?:create\\s+table(?:\\s+if\\s+not\\s+exists)?|alter\\s+table|"
                    + "drop\\s+table(?:\\s+if\\s+exists)?)\\s+([`\"\\[\\]A-Za-z0-9_.$]+)");
    private static final Pattern INDEX_TABLE_FALLBACK = Pattern.compile(
            "(?is)\\bcreate(?:\\s+unique)?\\s+index\\b.*?\\bon\\s+(?:table\\s+)?"
                    + "([`\"\\[\\]A-Za-z0-9_.$]+)");

    private final List<DdlDiagnosisRule> diagnosisRules;

    public DdlDiagnosisServiceImpl(List<DdlDiagnosisRule> diagnosisRules) {
        this.diagnosisRules = diagnosisRules;
    }

    @Override
    public DdlCheckResultVO precheck(DdlCheckRequestVO request) {
        validateRequest(request);
        List<DdlStatementSplitter.Segment> segments = DdlStatementSplitter.split(request.getInputDDL());
        if (segments.isEmpty()) {
            throw new ServiceException("未检测到可预检的DDL语句");
        }

        List<DdlIssueVO> issues = new ArrayList<>();
        List<DdlStatementCheckVO> statementChecks = new ArrayList<>();
        for (DdlStatementSplitter.Segment segment : segments) {
            diagnoseStatement(request, segment, issues, statementChecks);
        }
        assignIssueIds(issues);
        return buildResult(issues, statementChecks);
    }

    private void validateRequest(DdlCheckRequestVO request) {
        if (request == null || StrUtil.isBlank(request.getInputDDL())) {
            throw new ServiceException("DDL语句不能为空");
        }
        if (StrUtil.isBlank(request.getSourceDatabase())) {
            throw new ServiceException("源数据库方言不能为空");
        }
        if (StrUtil.isBlank(request.getOutputDatabase())) {
            throw new ServiceException("目标数据库方言不能为空");
        }
    }

    private void diagnoseStatement(DdlCheckRequestVO request, DdlStatementSplitter.Segment segment,
                                   List<DdlIssueVO> allIssues,
                                   List<DdlStatementCheckVO> statementChecks) {
        Statement statement = null;
        Exception originalException = null;
        boolean compatibilityPrepared = false;
        try {
            statement = CCJSqlParserUtil.parse(segment.getSql());
        } catch (Exception exception) {
            originalException = exception;
            String prepared = DdlParserSupport.prepareForCompatibility(segment.getSql());
            if (!StrUtil.equals(prepared, segment.getSql())) {
                try {
                    statement = CCJSqlParserUtil.parse(prepared);
                    compatibilityPrepared = true;
                } catch (Exception ignored) {
                    // 使用原始解析异常报告准确位置。
                }
            }
        }

        String tableName = statement == null
                ? extractTableNameFallback(segment.getSql()) : extractTableName(statement);
        DdlDiagnosisContext context = new DdlDiagnosisContext(request, segment, statement, tableName);

        if (statement == null) {
            int[] position = extractParserPosition(originalException);
            context.addIssueAtLocalPosition("SYNTAX_ERROR", "ERROR", "SYNTAX", null,
                    "DDL语法解析失败",
                    "解析器无法识别该位置附近的语法，可能存在括号、逗号、字段定义错误或未支持的方言扩展。",
                    "检查当前语句的括号、逗号和字段定义；若属于数据库扩展语法，请拆分或改写后再转换。",
                    position[0], position[1]);
        } else {
            if (compatibilityPrepared) {
                context.addIssue("PARSER_COMPATIBILITY", "WARNING", "UNSUPPORTED_SYNTAX", null,
                        "该语句需要兼容处理后才能解析",
                        "原始语法超出了当前解析器的直接支持范围，预检使用了兼容转换。",
                        "请重点核对转换结果，必要时将数据库扩展语法拆成独立语句。");
            }
            if (isSupportedStatement(statement)) {
                for (DdlDiagnosisRule diagnosisRule : diagnosisRules) {
                    diagnosisRule.diagnose(context);
                }
            } else {
                String reason = statement instanceof SetStatement
                        ? "SET会改变会话状态，不同数据库之间没有稳定的一一对应关系。"
                        : "当前转换器只转换建表、索引、注释和删除表语句，其他语句会被原样保留。";
                String suggestion = statement instanceof SetStatement
                        ? "从迁移DDL中移除SET语句，并在目标数据库部署脚本中单独配置会话参数。"
                        : "将该对象改为目标方言语法，或从结构转换任务中拆出后单独迁移。";
                context.addIssue("UNSUPPORTED_STATEMENT", "ERROR", "UNSUPPORTED_SYNTAX", null,
                        "当前不支持转换“" + statementType(statement) + "”语句", reason, suggestion);
            }
        }

        allIssues.addAll(context.getIssues());
        statementChecks.add(buildStatementCheck(segment, statement, tableName, context.getIssues()));
    }

    private boolean isSupportedStatement(Statement statement) {
        if (statement instanceof CreateTable || statement instanceof CreateIndex || statement instanceof Comment) {
            return true;
        }
        return statement instanceof Drop
                && statement.toString().trim().toUpperCase(Locale.ROOT).startsWith("DROP TABLE");
    }

    private String statementType(Statement statement) {
        if (statement instanceof CreateTable) {
            return "建表";
        }
        if (statement instanceof CreateIndex) {
            return "建索引";
        }
        if (statement instanceof Comment) {
            return "注释";
        }
        if (statement instanceof Drop) {
            return "删除对象";
        }
        if (statement instanceof SetStatement) {
            return "会话设置";
        }
        return "其他";
    }

    private String extractTableName(Statement statement) {
        if (statement instanceof CreateTable createTable && createTable.getTable() != null) {
            return normalizeIdentifier(createTable.getTable().getFullyQualifiedName());
        }
        if (statement instanceof CreateIndex createIndex && createIndex.getTable() != null) {
            return normalizeIdentifier(createIndex.getTable().getFullyQualifiedName());
        }
        if (statement instanceof Comment comment) {
            if (comment.getTable() != null) {
                return normalizeIdentifier(comment.getTable().getFullyQualifiedName());
            }
            Column column = comment.getColumn();
            if (column != null && column.getTable() != null) {
                return normalizeIdentifier(column.getTable().getFullyQualifiedName());
            }
        }
        if (statement instanceof Drop drop && drop.getName() != null) {
            return normalizeIdentifier(drop.getName().getFullyQualifiedName());
        }
        return extractTableNameFallback(statement.toString());
    }

    private String extractTableNameFallback(String sql) {
        Matcher matcher = TABLE_NAME_FALLBACK.matcher(sql);
        if (matcher.find()) {
            return normalizeIdentifier(matcher.group(1));
        }
        matcher = INDEX_TABLE_FALLBACK.matcher(sql);
        return matcher.find() ? normalizeIdentifier(matcher.group(1)) : null;
    }

    private String normalizeIdentifier(String value) {
        return DdlDiagnosisContext.normalizeIdentifier(value);
    }

    private int[] extractParserPosition(Exception exception) {
        Throwable current = exception;
        while (current != null) {
            Matcher matcher = PARSER_POSITION.matcher(StrUtil.blankToDefault(current.getMessage(), ""));
            if (matcher.find()) {
                return new int[]{Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2))};
            }
            current = current.getCause();
        }
        return new int[]{1, 1};
    }

    private DdlStatementCheckVO buildStatementCheck(DdlStatementSplitter.Segment segment,
                                                     Statement statement, String tableName,
                                                     List<DdlIssueVO> issues) {
        int errorCount = countLevel(issues, "ERROR");
        int warningCount = countLevel(issues, "WARNING");
        DdlStatementCheckVO result = new DdlStatementCheckVO();
        result.setStatementIndex(segment.getIndex());
        result.setStatementType(statement == null ? "无法解析" : statementType(statement));
        result.setTableName(tableName);
        result.setStartLine(segment.getStartLine());
        result.setEndLine(segment.getEndLine());
        result.setParseSuccess(statement != null);
        result.setConvertible(statement != null && errorCount == 0);
        result.setErrorCount(errorCount);
        result.setWarningCount(warningCount);
        return result;
    }

    private DdlCheckResultVO buildResult(List<DdlIssueVO> issues,
                                         List<DdlStatementCheckVO> statements) {
        int errors = countLevel(issues, "ERROR");
        int warnings = countLevel(issues, "WARNING");
        int infos = countLevel(issues, "INFO");
        int successCount = (int) statements.stream().filter(DdlStatementCheckVO::isConvertible).count();

        DdlCheckResultVO result = new DdlCheckResultVO();
        result.setValid(errors == 0);
        result.setStatementCount(statements.size());
        result.setSuccessCount(successCount);
        result.setErrorCount(errors);
        result.setWarningCount(warnings);
        result.setInfoCount(infos);
        result.setSuccessRate(calculateRate(successCount, statements.size()));
        result.setIssues(issues);
        result.setStatements(statements);
        result.setTableStatistics(buildTableStatistics(statements));

        Set<String> tableNames = new LinkedHashSet<>();
        for (DdlStatementCheckVO statement : statements) {
            if (StrUtil.isNotBlank(statement.getTableName())) {
                tableNames.add(statement.getTableName());
            }
        }
        result.setTableCount(tableNames.size());
        return result;
    }

    private List<DdlTableCheckVO> buildTableStatistics(List<DdlStatementCheckVO> statements) {
        Map<String, List<DdlStatementCheckVO>> grouped = new LinkedHashMap<>();
        for (DdlStatementCheckVO statement : statements) {
            String tableName = StrUtil.blankToDefault(statement.getTableName(), "未识别语句");
            grouped.computeIfAbsent(tableName, key -> new ArrayList<>()).add(statement);
        }

        List<DdlTableCheckVO> result = new ArrayList<>();
        for (Map.Entry<String, List<DdlStatementCheckVO>> entry : grouped.entrySet()) {
            List<DdlStatementCheckVO> tableStatements = entry.getValue();
            int success = (int) tableStatements.stream().filter(DdlStatementCheckVO::isConvertible).count();
            int errors = tableStatements.stream()
                    .mapToInt(item -> item.getErrorCount() == null ? 0 : item.getErrorCount()).sum();
            int warnings = tableStatements.stream()
                    .mapToInt(item -> item.getWarningCount() == null ? 0 : item.getWarningCount()).sum();

            DdlTableCheckVO table = new DdlTableCheckVO();
            table.setTableName(entry.getKey());
            table.setStatementCount(tableStatements.size());
            table.setSuccessCount(success);
            table.setErrorCount(errors);
            table.setWarningCount(warnings);
            table.setSuccessRate(calculateRate(success, tableStatements.size()));
            table.setStatus(errors > 0 ? "失败" : warnings > 0 ? "有风险" : "通过");
            result.add(table);
        }
        return result;
    }

    private int countLevel(List<DdlIssueVO> issues, String level) {
        return (int) issues.stream().filter(issue -> level.equals(issue.getLevel())).count();
    }

    private int calculateRate(int success, int total) {
        return total == 0 ? 0 : (int) Math.round(success * 100.0 / total);
    }

    private void assignIssueIds(List<DdlIssueVO> issues) {
        for (int i = 0; i < issues.size(); i++) {
            issues.get(i).setIssueId(String.format("ISSUE-%03d", i + 1));
        }
    }
}
