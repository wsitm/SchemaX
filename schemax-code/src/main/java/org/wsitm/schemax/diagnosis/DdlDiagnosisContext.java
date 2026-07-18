package org.wsitm.schemax.diagnosis;

import net.sf.jsqlparser.statement.Statement;
import org.wsitm.schemax.entity.vo.DdlCheckRequestVO;
import org.wsitm.schemax.entity.vo.DdlIssueVO;
import org.wsitm.schemax.utils.DdlStatementSplitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DdlDiagnosisContext {
    private final DdlCheckRequestVO request;
    private final DdlStatementSplitter.Segment segment;
    private final Statement statement;
    private final String tableName;
    private final List<DdlIssueVO> issues = new ArrayList<>();

    public DdlDiagnosisContext(DdlCheckRequestVO request, DdlStatementSplitter.Segment segment,
                               Statement statement, String tableName) {
        this.request = request;
        this.segment = segment;
        this.statement = statement;
        this.tableName = tableName;
    }

    public DdlCheckRequestVO getRequest() {
        return request;
    }

    public DdlStatementSplitter.Segment getSegment() {
        return segment;
    }

    public Statement getStatement() {
        return statement;
    }

    public String getTableName() {
        return tableName;
    }

    public List<DdlIssueVO> getIssues() {
        return issues;
    }

    public void addIssue(String code, String level, String category, String columnName,
                         String message, String reason, String suggestion) {
        addIssueAt(code, level, category, columnName, message, reason, suggestion, 0, 1);
    }

    public void addIssueAtToken(String code, String level, String category, String columnName,
                                String message, String reason, String suggestion, String token) {
        addIssueAtToken(code, level, category, columnName, message, reason, suggestion, token, 0);
    }

    public void addIssueAtColumn(String code, String level, String category, String columnName,
                                 String message, String reason, String suggestion) {
        int from = Math.max(0, segment.getSql().indexOf('('));
        addIssueAtToken(code, level, category, columnName, message, reason, suggestion, columnName, from);
    }

    private void addIssueAtToken(String code, String level, String category, String columnName,
                                 String message, String reason, String suggestion, String token, int from) {
        String source = segment.getSql().toLowerCase(Locale.ROOT);
        String target = token == null ? "" : token.toLowerCase(Locale.ROOT);
        int localOffset = target.isEmpty() ? -1 : source.indexOf(target, Math.max(0, from));
        addIssueAt(code, level, category, columnName, message, reason, suggestion,
                Math.max(0, localOffset), Math.max(1, token == null ? 1 : token.length()));
    }

    public void addIssueAtLocalPosition(String code, String level, String category, String columnName,
                                        String message, String reason, String suggestion,
                                        int localLine, int localColumn) {
        int globalOffset = segment.toGlobalOffset(localLine, localColumn);
        addIssueAt(code, level, category, columnName, message, reason, suggestion,
                Math.max(0, globalOffset - segment.getStartOffset()), 1);
    }

    private void addIssueAt(String code, String level, String category, String columnName,
                            String message, String reason, String suggestion, int localOffset, int length) {
        int safeStart = Math.min(Math.max(0, localOffset), segment.getSql().length());
        int safeEnd = Math.min(segment.getSql().length(), safeStart + Math.max(1, length));
        int[] startPosition = positionAt(safeStart);
        int[] endPosition = positionAt(safeEnd);

        DdlIssueVO issue = new DdlIssueVO();
        issue.setCode(code);
        issue.setLevel(level);
        issue.setCategory(category);
        issue.setStatementIndex(segment.getIndex());
        issue.setTableName(tableName);
        issue.setColumnName(columnName);
        issue.setStartOffset(segment.getStartOffset() + safeStart);
        issue.setEndOffset(segment.getStartOffset() + safeEnd);
        issue.setStartLine(startPosition[0]);
        issue.setStartColumn(startPosition[1]);
        issue.setEndLine(endPosition[0]);
        issue.setEndColumn(endPosition[1]);
        issue.setMessage(message);
        issue.setReason(reason);
        issue.setSuggestion(suggestion);
        issues.add(issue);
    }

    private int[] positionAt(int localOffset) {
        int line = segment.getStartLine();
        int column = segment.getStartColumn();
        for (int i = 0; i < localOffset && i < segment.getSql().length(); i++) {
            if (segment.getSql().charAt(i) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        return new int[]{line, column};
    }

    public static String normalizeIdentifier(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("`", "")
                .replace("\"", "")
                .replace("[", "")
                .replace("]", "")
                .replace("__point__", ".");
    }
}
