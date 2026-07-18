package org.wsitm.schemax.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;
import org.wsitm.schemax.entity.vo.ColumnVO;
import org.wsitm.schemax.entity.vo.TableVO;
import org.wsitm.schemax.utils.json.JSONArray;
import org.wsitm.schemax.utils.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Excel、Word 和 Markdown 模板共用的数据上下文及表达式解析器。
 */
@Component
public class TemplateContextService {

    public static final String CTX_IN_FOR = "_inFor";

    private static final Pattern EXPRESSION_RE = Pattern.compile("\\$\\{\\s*([^}]+?)\\s*}");
    private static final Set<String> COLUMN_FIELD_SET = Set.of(
            "columnName", "columnType", "columnSize", "columnDigit", "columnNullable",
            "columnAutoIncrement", "columnPk", "columnDef", "columnComment", "name",
            "type", "typeName", "size", "digit", "nullable", "autoIncrement", "pk",
            "def", "comment"
    );

    public Map<String, Object> buildTableContext(TableVO tableVO, int tableOrder) {
        Map<String, Object> tableMap = normalizeTable(tableVO, tableOrder);
        Map<String, Object> context = new HashMap<>(tableMap);
        context.put("table", tableMap);
        context.put("order", tableOrder);
        context.put(CTX_IN_FOR, false);
        return context;
    }

    public Map<String, Object> buildLoopContext(Map<String, Object> parent,
                                                String alias,
                                                Object item,
                                                int loopOrder) {
        Map<String, Object> next = new HashMap<>(parent);
        next.put(alias, item);
        next.put("order", loopOrder);
        next.put(CTX_IN_FOR, true);
        if (item instanceof Map<?, ?> itemMap) {
            for (Map.Entry<?, ?> entry : itemMap.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    next.putIfAbsent(key, entry.getValue());
                }
            }
        }
        return next;
    }

    public String renderText(String text, Map<String, Object> context) {
        if (text == null) {
            return "";
        }
        Matcher matcher = EXPRESSION_RE.matcher(text);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            Object value = resolvePath(context, StrUtil.trimToEmpty(matcher.group(1)));
            matcher.appendReplacement(output, Matcher.quoteReplacement(formatValue(value)));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    public boolean hasColumnExpression(String text) {
        if (StrUtil.isBlank(text)) {
            return false;
        }
        Matcher matcher = EXPRESSION_RE.matcher(text);
        while (matcher.find()) {
            String expression = StrUtil.trimToEmpty(matcher.group(1));
            if (isColumnExpression(expression)) {
                return true;
            }
        }
        return false;
    }

    public boolean isInForContext(Map<String, Object> context) {
        return Boolean.TRUE.equals(context.get(CTX_IN_FOR));
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getContextColumnList(Map<String, Object> context) {
        Object columnList = context.get("columnList");
        if (!(columnList instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?>) {
                result.add((Map<String, Object>) item);
            }
        }
        return result;
    }

    public List<Map<String, Object>> resolveLoopSource(Map<String, Object> context, String listExpression) {
        Object source = resolvePath(context, listExpression);
        if (!(source instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (listExpression != null && listExpression.endsWith("columnList") && item instanceof Map<?, ?> map) {
                result.add(normalizeLoopColumnMap(map, i + 1));
            } else if (item instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> row = (Map<String, Object>) map;
                result.add(row);
            }
        }
        return result;
    }

    private Object resolvePath(Map<String, Object> context, String expression) {
        if (StrUtil.isBlank(expression)) {
            return "";
        }
        String key = expression.trim();
        if ("UUID".equals(key)) {
            return IdUtil.fastSimpleUUID();
        }
        if ("nanoId".equals(key)) {
            return IdUtil.nanoId();
        }
        if ("order".equals(key)) {
            return context.getOrDefault("order", 1);
        }

        Object cursor = context;
        for (String part : key.split("\\.")) {
            if (cursor instanceof Map<?, ?> map) {
                cursor = map.get(part);
            } else if (cursor instanceof JSONObject jsonObject) {
                cursor = jsonObject.get(part);
            } else {
                return "";
            }
            if (cursor == null) {
                return "";
            }
        }
        return cursor;
    }

    private String formatValue(Object value) {
        if (value == null || value instanceof Map<?, ?> || value instanceof List<?>
                || value instanceof JSONObject || value instanceof JSONArray) {
            return "";
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue ? "YES" : "NO";
        }
        return String.valueOf(value);
    }

    private boolean isColumnExpression(String expression) {
        if (COLUMN_FIELD_SET.contains(expression)) {
            return true;
        }
        int dot = expression.indexOf('.');
        return dot > 0 && dot < expression.length() - 1
                && COLUMN_FIELD_SET.contains(expression.substring(dot + 1).trim());
    }

    private Map<String, Object> normalizeTable(TableVO tableVO, int tableOrder) {
        List<Map<String, Object>> columnList = new ArrayList<>();
        List<ColumnVO> columns = tableVO.getColumnList() == null ? List.of() : tableVO.getColumnList();
        for (int i = 0; i < columns.size(); i++) {
            columnList.add(normalizeColumn(columns.get(i), i + 1));
        }

        Map<String, Object> table = new HashMap<>();
        table.put("schema", StrUtil.nullToEmpty(tableVO.getSchema()));
        table.put("catalog", StrUtil.nullToEmpty(tableVO.getCatalog()));
        table.put("tableName", StrUtil.nullToEmpty(tableVO.getTableName()));
        table.put("tableComment", StrUtil.nullToEmpty(tableVO.getComment()));
        table.put("comment", StrUtil.nullToEmpty(tableVO.getComment()));
        table.put("numRows", tableVO.getNumRows() == null ? "" : tableVO.getNumRows());
        table.put("order", tableOrder);
        table.put("columnList", columnList);
        return table;
    }

    private Map<String, Object> normalizeColumn(ColumnVO columnVO, int order) {
        String name = StrUtil.nullToEmpty(columnVO.getName());
        String typeName = StrUtil.nullToEmpty(columnVO.getTypeName());
        String nullable = columnVO.isNullable() ? "" : "YES";
        String autoIncrement = columnVO.isAutoIncrement() ? "YES" : "";
        String primaryKey = columnVO.isPk() ? "YES" : "";
        String defaultValue = StrUtil.nullToEmpty(columnVO.getColumnDef());
        String comment = StrUtil.nullToEmpty(columnVO.getComment());

        Map<String, Object> column = new HashMap<>();
        column.put("order", order);
        column.put("name", name);
        column.put("type", typeName);
        column.put("typeName", typeName);
        column.put("size", columnVO.getSize());
        column.put("digit", columnVO.getDigit() == null ? "" : columnVO.getDigit());
        column.put("nullable", nullable);
        column.put("autoIncrement", autoIncrement);
        column.put("pk", primaryKey);
        column.put("def", defaultValue);
        column.put("columnDef", defaultValue);
        column.put("comment", comment);
        column.put("columnName", name);
        column.put("columnType", typeName);
        column.put("columnSize", columnVO.getSize());
        column.put("columnDigit", columnVO.getDigit() == null ? "" : columnVO.getDigit());
        column.put("columnNullable", nullable);
        column.put("columnAutoIncrement", autoIncrement);
        column.put("columnPk", primaryKey);
        column.put("columnComment", comment);
        return column;
    }

    private Map<String, Object> normalizeLoopColumnMap(Map<?, ?> source, int order) {
        String name = valueToString(source.get("name"));
        String typeName = valueToString(source.containsKey("typeName") ? source.get("typeName") : source.get("type"));
        String nullable = resolveNotNullDisplay(source);
        String autoIncrement = toYesNo(valueToString(source.get("autoIncrement")));
        String primaryKey = toYesNo(valueToString(source.get("pk")));
        String defaultValue = valueToString(source.containsKey("def") ? source.get("def") : source.get("columnDef"));
        String comment = valueToString(source.get("comment"));

        Map<String, Object> column = new HashMap<>();
        column.put("order", source.containsKey("order") ? source.get("order") : order);
        column.put("name", name);
        column.put("type", typeName);
        column.put("typeName", typeName);
        column.put("size", valueOrDefault(source, "size", ""));
        column.put("digit", valueOrDefault(source, "digit", ""));
        column.put("nullable", nullable);
        column.put("autoIncrement", autoIncrement);
        column.put("pk", primaryKey);
        column.put("def", defaultValue);
        column.put("columnDef", defaultValue);
        column.put("comment", comment);
        column.put("columnName", name);
        column.put("columnType", typeName);
        column.put("columnSize", valueOrDefault(source, "size", ""));
        column.put("columnDigit", valueOrDefault(source, "digit", ""));
        column.put("columnNullable", nullable);
        column.put("columnAutoIncrement", autoIncrement);
        column.put("columnPk", primaryKey);
        column.put("columnComment", comment);
        return column;
    }

    private String resolveNotNullDisplay(Map<?, ?> source) {
        if (source.containsKey("columnNullable")) {
            return toYesBlank(source.get("columnNullable"));
        }
        Object nullable = source.get("nullable");
        if (nullable instanceof Boolean booleanValue) {
            return booleanValue ? "" : "YES";
        }
        if (nullable instanceof Number numberValue) {
            return numberValue.intValue() == 0 ? "YES" : "";
        }
        String text = valueToString(nullable).trim().toLowerCase();
        if (Set.of("true", "yes", "1", "y").contains(text)) {
            return "";
        }
        if (Set.of("false", "no", "0", "n").contains(text)) {
            return "YES";
        }
        return toYesBlank(nullable);
    }

    private String toYesBlank(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue ? "YES" : "";
        }
        if (value instanceof Number numberValue) {
            return numberValue.intValue() == 0 ? "" : "YES";
        }
        String text = valueToString(value).trim().toLowerCase();
        return Set.of("yes", "true", "1", "y").contains(text) ? "YES" : "";
    }

    private String toYesNo(String value) {
        String normalized = StrUtil.trimToEmpty(value).toLowerCase();
        if (Set.of("true", "yes", "1", "y").contains(normalized)) {
            return "YES";
        }
        if (Set.of("false", "no", "0", "n").contains(normalized)) {
            return "NO";
        }
        return value;
    }

    private Object valueOrDefault(Map<?, ?> source, String key, Object defaultValue) {
        Object value = source.get(key);
        return value == null ? defaultValue : value;
    }

    private String valueToString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
