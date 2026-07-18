package org.wsitm.schemax.utils;

import cn.hutool.core.util.StrUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 按 SQL 语义切分 DDL，并保留每条语句在原始文本中的位置。
 */
public final class DdlStatementSplitter {
    private DdlStatementSplitter() {
    }

    public static List<Segment> split(String source) {
        if (StrUtil.isBlank(source)) {
            return Collections.emptyList();
        }
        List<Integer> lineStarts = buildLineStarts(source);
        List<Segment> result = new ArrayList<>();
        boolean singleQuote = false;
        boolean doubleQuote = false;
        boolean backtick = false;
        boolean bracketQuote = false;
        boolean lineComment = false;
        boolean blockComment = false;
        String dollarTag = null;
        int statementStart = 0;

        for (int i = 0; i < source.length(); i++) {
            char current = source.charAt(i);
            char next = i + 1 < source.length() ? source.charAt(i + 1) : '\0';

            if (lineComment) {
                if (current == '\r' || current == '\n') {
                    lineComment = false;
                }
                continue;
            }
            if (blockComment) {
                if (current == '*' && next == '/') {
                    blockComment = false;
                    i++;
                }
                continue;
            }
            if (dollarTag != null) {
                if (source.startsWith(dollarTag, i)) {
                    i += dollarTag.length() - 1;
                    dollarTag = null;
                }
                continue;
            }

            if (!singleQuote && !doubleQuote && !backtick && !bracketQuote) {
                if (current == '-' && next == '-') {
                    lineComment = true;
                    i++;
                    continue;
                }
                if (current == '#' && isLineStartComment(source, i)) {
                    lineComment = true;
                    continue;
                }
                if (current == '/' && next == '*') {
                    blockComment = true;
                    i++;
                    continue;
                }
                if (current == '$') {
                    String tag = readDollarTag(source, i);
                    if (tag != null) {
                        dollarTag = tag;
                        i += tag.length() - 1;
                        continue;
                    }
                }
            }

            if (current == '\'' && !doubleQuote && !backtick && !bracketQuote) {
                if (singleQuote && next == '\'') {
                    i++;
                } else if (!isEscaped(source, i)) {
                    singleQuote = !singleQuote;
                }
                continue;
            }
            if (current == '"' && !singleQuote && !backtick && !bracketQuote) {
                if (doubleQuote && next == '"') {
                    i++;
                } else {
                    doubleQuote = !doubleQuote;
                }
                continue;
            }
            if (current == '`' && !singleQuote && !doubleQuote && !bracketQuote) {
                if (backtick && next == '`') {
                    i++;
                } else {
                    backtick = !backtick;
                }
                continue;
            }
            if (current == '[' && !singleQuote && !doubleQuote && !backtick && !bracketQuote) {
                bracketQuote = true;
                continue;
            }
            if (current == ']' && bracketQuote) {
                if (next == ']') {
                    i++;
                } else {
                    bracketQuote = false;
                }
                continue;
            }

            if (current == ';' && !singleQuote && !doubleQuote && !backtick && !bracketQuote) {
                addSegment(result, source, statementStart, i, lineStarts);
                statementStart = i + 1;
            }
        }
        addSegment(result, source, statementStart, source.length(), lineStarts);
        return result;
    }

    private static void addSegment(List<Segment> result, String source, int from, int to,
                                   List<Integer> lineStarts) {
        int start = from;
        int end = to;
        while (start < end && Character.isWhitespace(source.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(source.charAt(end - 1))) {
            end--;
        }
        if (start >= end) {
            return;
        }
        String sql = source.substring(start, end);
        if (StrUtil.isBlank(DdlParserSupport.removeComments(sql))) {
            return;
        }
        result.add(new Segment(result.size() + 1, sql, start, end,
                positionAt(lineStarts, start), positionAt(lineStarts, Math.max(start, end - 1))));
    }

    private static List<Integer> buildLineStarts(String source) {
        List<Integer> result = new ArrayList<>();
        result.add(0);
        for (int i = 0; i < source.length(); i++) {
            if (source.charAt(i) == '\n') {
                result.add(i + 1);
            }
        }
        return result;
    }

    private static Position positionAt(List<Integer> lineStarts, int offset) {
        int index = Collections.binarySearch(lineStarts, offset);
        int lineIndex = index >= 0 ? index : -index - 2;
        lineIndex = Math.max(0, lineIndex);
        return new Position(lineIndex + 1, offset - lineStarts.get(lineIndex) + 1);
    }

    private static String readDollarTag(String source, int start) {
        int end = source.indexOf('$', start + 1);
        if (end < 0) {
            return null;
        }
        String tagName = source.substring(start + 1, end);
        if (!tagName.matches("[A-Za-z0-9_]*")) {
            return null;
        }
        return source.substring(start, end + 1);
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

    public static final class Segment {
        private final int index;
        private final String sql;
        private final int startOffset;
        private final int endOffset;
        private final Position start;
        private final Position end;

        private Segment(int index, String sql, int startOffset, int endOffset, Position start, Position end) {
            this.index = index;
            this.sql = sql;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.start = start;
            this.end = end;
        }

        public int getIndex() {
            return index;
        }

        public String getSql() {
            return sql;
        }

        public int getStartOffset() {
            return startOffset;
        }

        public int getEndOffset() {
            return endOffset;
        }

        public int getStartLine() {
            return start.line;
        }

        public int getStartColumn() {
            return start.column;
        }

        public int getEndLine() {
            return end.line;
        }

        public int getEndColumn() {
            return end.column;
        }

        public int toGlobalOffset(int localLine, int localColumn) {
            int line = Math.max(1, localLine);
            int column = Math.max(1, localColumn);
            int localOffset = 0;
            int currentLine = 1;
            while (localOffset < sql.length() && currentLine < line) {
                if (sql.charAt(localOffset++) == '\n') {
                    currentLine++;
                }
            }
            localOffset = Math.min(sql.length(), localOffset + column - 1);
            return startOffset + localOffset;
        }
    }

    private static final class Position {
        private final int line;
        private final int column;

        private Position(int line, int column) {
            this.line = line;
            this.column = column;
        }
    }
}
