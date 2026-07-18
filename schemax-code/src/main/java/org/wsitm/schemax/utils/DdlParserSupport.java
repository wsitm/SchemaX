package org.wsitm.schemax.utils;

import cn.hutool.core.util.StrUtil;

/**
 * JSqlParser 兼容处理集中在这里，便于后续替换解析器或收敛方言补丁。
 */
public final class DdlParserSupport {
    private DdlParserSupport() {
    }

    public static String prepareForCompatibility(String sql) {
        if (StrUtil.isBlank(sql)) {
            return sql;
        }
        String prepared = removeComments(sql).replaceAll("\\s+", " ").trim();
        if ((StrUtil.startWithAnyIgnoreCase(prepared, "create table")
                || StrUtil.startWithAnyIgnoreCase(prepared, "create index"))
                && StrUtil.contains(prepared, ".")) {
            prepared = prepared.replace(".", DDLUtil.POINT_TAG);
        }
        if (StrUtil.startWithAnyIgnoreCase(prepared, "create table")
                && StrUtil.containsIgnoreCase(prepared, "unique index")) {
            prepared = prepared.replaceAll("(?i)(?<=UNIQUE).*?(?=\\()", " ");
        }
        if (StrUtil.startWithAnyIgnoreCase(prepared, "create index")
                && StrUtil.containsIgnoreCase(prepared, "on table")) {
            prepared = prepared.replaceAll("(?i)on\\s+table", "on");
        }
        return prepared;
    }

    public static String removeComments(String sql) {
        if (sql == null || sql.isEmpty()) {
            return sql;
        }
        StringBuilder result = new StringBuilder(sql.length());
        boolean singleQuote = false;
        boolean doubleQuote = false;
        boolean backtick = false;
        boolean lineComment = false;
        boolean blockComment = false;

        for (int i = 0; i < sql.length(); i++) {
            char current = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';

            if (lineComment) {
                if (current == '\r' || current == '\n') {
                    lineComment = false;
                    result.append(current);
                } else {
                    result.append(' ');
                }
                continue;
            }
            if (blockComment) {
                if (current == '*' && next == '/') {
                    result.append("  ");
                    blockComment = false;
                    i++;
                } else {
                    result.append(current == '\r' || current == '\n' ? current : ' ');
                }
                continue;
            }
            if (!singleQuote && !doubleQuote && !backtick) {
                if (current == '-' && next == '-') {
                    result.append("  ");
                    lineComment = true;
                    i++;
                    continue;
                }
                if (current == '#' && isLineStartComment(sql, i)) {
                    result.append(' ');
                    lineComment = true;
                    continue;
                }
                if (current == '/' && next == '*') {
                    result.append("  ");
                    blockComment = true;
                    i++;
                    continue;
                }
            }

            result.append(current);
            if (current == '\'' && !doubleQuote && !backtick) {
                if (singleQuote && next == '\'') {
                    result.append(next);
                    i++;
                } else if (!isEscaped(sql, i)) {
                    singleQuote = !singleQuote;
                }
            } else if (current == '"' && !singleQuote && !backtick) {
                if (doubleQuote && next == '"') {
                    result.append(next);
                    i++;
                } else {
                    doubleQuote = !doubleQuote;
                }
            } else if (current == '`' && !singleQuote && !doubleQuote) {
                backtick = !backtick;
            }
        }
        return result.toString();
    }

    private static boolean isLineStartComment(String value, int index) {
        for (int i = index - 1; i >= 0; i--) {
            char current = value.charAt(i);
            if (current == '\r' || current == '\n') {
                return true;
            }
            if (!Character.isWhitespace(current)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isEscaped(String value, int index) {
        int slashCount = 0;
        for (int i = index - 1; i >= 0 && value.charAt(i) == '\\'; i--) {
            slashCount++;
        }
        return slashCount % 2 == 1;
    }
}
