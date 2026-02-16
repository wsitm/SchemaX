package org.wsitm.schemax.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.stereotype.Service;
import org.wsitm.schemax.entity.vo.ColumnVO;
import org.wsitm.schemax.entity.vo.TableVO;

import java.io.File;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TemplateRenderService {

    private static final Pattern FOR_DIRECTIVE_RE = Pattern.compile("^#for\\s*\\(\\s*([a-zA-Z_]\\w*)\\s+in\\s+([a-zA-Z_][\\w.]*)\\s*\\)\\s*$");
    private static final Pattern END_DIRECTIVE_RE = Pattern.compile("^#end\\s*$");
    private static final Pattern EXPRESSION_RE = Pattern.compile("\\$\\{\\s*([^}]+?)\\s*}");

    private static final Set<String> COLUMN_FIELD_SET = Set.of(
            "columnName",
            "columnType",
            "columnSize",
            "columnDigit",
            "columnNullable",
            "columnAutoIncrement",
            "columnPk",
            "columnDef",
            "columnComment",
            "name",
            "type",
            "typeName",
            "size",
            "digit",
            "nullable",
            "autoIncrement",
            "pk",
            "def",
            "comment"
    );

    private static final String CTX_IN_FOR = "_inFor";

    public String renderMarkdown(List<TableVO> tableVOList, String templateContent) {
        if (StrUtil.isBlank(templateContent)) {
            return "";
        }
        if (tableVOList == null || tableVOList.isEmpty()) {
            return "";
        }

        List<String> lines = Arrays.asList(templateContent.split("\\r?\\n", -1));
        List<String> blockList = new ArrayList<>();

        for (int i = 0; i < tableVOList.size(); i++) {
            Map<String, Object> tableCtx = buildTableContext(tableVOList.get(i), i + 1);
            List<String> renderedLines = renderTemplateItems(
                    lines,
                    tableCtx,
                    this::parseDirectiveText,
                    (line, ctx) -> {
                        if (!isInForContext(ctx) && textHasLegacyColumnExpression(line)) {
                            List<Map<String, Object>> columns = getContextColumnList(ctx);
                            if (columns.isEmpty()) {
                                return List.of(renderText(line, ctx));
                            }
                            List<String> out = new ArrayList<>();
                            for (int c = 0; c < columns.size(); c++) {
                                Map<String, Object> columnCtx = buildLoopContext(ctx, "col", columns.get(c), c + 1);
                                out.add(renderText(line, columnCtx));
                            }
                            return out;
                        }
                        return List.of(renderText(line, ctx));
                    }
            );
            blockList.add(String.join("\n", renderedLines));
        }

        return String.join("\n\n", blockList);
    }

    public JSONObject renderWorkbook(List<TableVO> tableVOList, String templateContent) {
        if (StrUtil.isBlank(templateContent)) {
            return null;
        }
        if (tableVOList == null || tableVOList.isEmpty()) {
            return null;
        }

        JSONObject templateWorkbook = parseJsonObject(templateContent);
        if (templateWorkbook == null) {
            return null;
        }

        JSONObject workbook = JSON.parseObject(templateWorkbook.toJSONString());
        JSONObject sheets = workbook.getJSONObject("sheets");
        if (sheets == null || sheets.isEmpty()) {
            return workbook;
        }

        for (String sheetId : sheets.keySet()) {
            JSONObject sheet = sheets.getJSONObject(sheetId);
            JSONObject cellData = sheet.getJSONObject("cellData");
            if (cellData == null || cellData.isEmpty()) {
                continue;
            }

            List<Integer> rowIndexList = cellData.keySet().stream()
                    .map(this::parseInteger)
                    .filter(i -> i != null)
                    .sorted()
                    .toList();
            List<WorkbookRowTemplate> templateRows = new ArrayList<>();
            for (Integer rowIndex : rowIndexList) {
                JSONObject rowObj = cellData.getJSONObject(String.valueOf(rowIndex));
                templateRows.add(new WorkbookRowTemplate(rowIndex, rowObj == null ? new JSONObject() : rowObj));
            }
            JSONArray templateMergeData = sheet.getJSONArray("mergeData");

            JSONObject newCellData = new JSONObject();
            JSONArray newMergeData = new JSONArray();
            int currentRow = 0;

            for (int t = 0; t < tableVOList.size(); t++) {
                Map<String, Object> tableCtx = buildTableContext(tableVOList.get(t), t + 1);
                int blockStartRow = currentRow;
                List<RenderedWorkbookRow> renderedRows = renderTemplateItems(
                        templateRows,
                        tableCtx,
                        rowItem -> getWorkbookRowDirective(rowItem.rowObj),
                        (rowItem, ctx) -> {
                            JSONObject rowObj = rowItem.rowObj;
                            if (!isInForContext(ctx) && rowHasLegacyColumnExpression(rowObj)) {
                                List<Map<String, Object>> columns = getContextColumnList(ctx);
                                if (columns.isEmpty()) {
                                    return List.of(new RenderedWorkbookRow(rowItem.rowIndex, renderWorkbookRow(rowObj, ctx)));
                                }
                                List<RenderedWorkbookRow> out = new ArrayList<>();
                                for (int c = 0; c < columns.size(); c++) {
                                    Map<String, Object> columnCtx = buildLoopContext(ctx, "col", columns.get(c), c + 1);
                                    out.add(new RenderedWorkbookRow(rowItem.rowIndex, renderWorkbookRow(rowObj, columnCtx)));
                                }
                                return out;
                            }
                            return List.of(new RenderedWorkbookRow(rowItem.rowIndex, renderWorkbookRow(rowObj, ctx)));
                        }
                );

                for (RenderedWorkbookRow renderedRow : renderedRows) {
                    newCellData.put(String.valueOf(currentRow++), renderedRow.rowObj);
                }
                newMergeData.addAll(remapMergeDataForBlock(templateMergeData, renderedRows, blockStartRow));
                if (t < tableVOList.size() - 1) {
                    // Keep one physical blank row between rendered table blocks.
                    newCellData.put(String.valueOf(currentRow), new JSONObject());
                    currentRow++;
                }
            }

            sheet.put("cellData", newCellData);
            sheet.put("rowCount", Math.max(100, currentRow + 10));
            sheet.put("mergeData", newMergeData);
        }

        return workbook;
    }

    public void writeWorkbookToExcel(File file, JSONObject workbookData) {
        if (workbookData == null) {
            return;
        }
        JSONObject sheets = workbookData.getJSONObject("sheets");
        if (sheets == null || sheets.isEmpty()) {
            return;
        }

        String firstSheetId = workbookData.getJSONArray("sheetOrder") != null
                && !workbookData.getJSONArray("sheetOrder").isEmpty()
                ? workbookData.getJSONArray("sheetOrder").getString(0)
                : sheets.keySet().iterator().next();

        JSONObject sheet = sheets.getJSONObject(firstSheetId);
        if (sheet == null) {
            return;
        }
        JSONObject cellData = sheet.getJSONObject("cellData");
        if (cellData == null) {
            return;
        }

        List<Integer> rowIndexList = cellData.keySet().stream()
                .map(this::parseInteger)
                .filter(i -> i != null)
                .sorted()
                .toList();
        JSONObject styles = workbookData.getJSONObject("styles");
        Workbook poiWb;
        DataFormat dataFormat;
        Map<String, CellStyle> cellStyleCache = new HashMap<>();
        Map<String, Font> fontCache = new HashMap<>();
        Sheet poiSheet;

        try (ExcelWriter excelWriter = ExcelUtil.getBigWriter(file)) {
            poiSheet = excelWriter.getSheet();
            poiWb = excelWriter.getWorkbook();
            dataFormat = poiWb.createDataFormat();
            cellStyleCache.clear();
            fontCache.clear();

            for (Integer rowIndex : rowIndexList) {
                JSONObject rowObj = cellData.getJSONObject(String.valueOf(rowIndex));
                if (rowObj == null) {
                    continue;
                }
                Row poiRow = poiSheet.getRow(rowIndex);
                if (poiRow == null) {
                    poiRow = poiSheet.createRow(rowIndex);
                }
                if (rowObj.isEmpty()) {
                    continue;
                }

                List<Integer> colIndexList = rowObj.keySet().stream()
                        .map(this::parseInteger)
                        .filter(i -> i != null)
                        .sorted(Comparator.naturalOrder())
                        .toList();
                if (colIndexList.isEmpty()) {
                    continue;
                }
                for (Integer colIndex : colIndexList) {
                    JSONObject cellObj = rowObj.getJSONObject(String.valueOf(colIndex));
                    if (cellObj == null) {
                        continue;
                    }
                    Cell poiCell = poiRow.getCell(colIndex);
                    if (poiCell == null) {
                        poiCell = poiRow.createCell(colIndex);
                    }
                    writeCellValue(poiCell, extractCellValue(cellObj));

                    JSONObject styleData = resolveStyleData(cellObj, styles);
                    if (styleData != null && !styleData.isEmpty()) {
                        CellStyle cellStyle = toPoiCellStyle(
                                poiWb,
                                dataFormat,
                                styleData,
                                cellStyleCache,
                                fontCache
                        );
                        if (cellStyle != null) {
                            poiCell.setCellStyle(cellStyle);
                        }
                    }
                }
            }

            JSONObject columnData = sheet.getJSONObject("columnData");
            if (columnData != null) {
                for (String key : columnData.keySet()) {
                    JSONObject obj = columnData.getJSONObject(key);
                    if (obj == null) {
                        continue;
                    }
                    Integer width = obj.getInteger("w");
                    if (width != null && width > 0) {
                        int colWidth = Math.max(6, width / 7);
                        Integer colIndex = parseInteger(key);
                        if (colIndex != null) {
                            excelWriter.setColumnWidth(colIndex, colWidth);
                            JSONObject colStyleData = obj.getJSONObject("s");
                            if (colStyleData != null && !colStyleData.isEmpty()) {
                                CellStyle colStyle = toPoiCellStyle(
                                        poiWb,
                                        dataFormat,
                                        colStyleData,
                                        cellStyleCache,
                                        fontCache
                                );
                                if (colStyle != null) {
                                    poiSheet.setDefaultColumnStyle(colIndex, colStyle);
                                }
                            }
                        }
                    }
                }
            }

            applyMergedRegions(poiSheet, sheet.getJSONArray("mergeData"));

            excelWriter.flush();
        }
    }

    private JSONObject renderWorkbookRow(JSONObject rowObj, Map<String, Object> context) {
        JSONObject rendered = JSON.parseObject(rowObj.toJSONString());
        for (String colKey : rendered.keySet()) {
            JSONObject cell = rendered.getJSONObject(colKey);
            if (cell == null) {
                continue;
            }
            if (cell.get("v") instanceof String val) {
                cell.put("v", renderText(val, context));
            }
            if (cell.get("m") instanceof String val) {
                cell.put("m", renderText(val, context));
            }
            JSONObject p = cell.getJSONObject("p");
            if (p == null) {
                continue;
            }
            JSONObject body = p.getJSONObject("body");
            if (body == null) {
                continue;
            }
            if (body.get("dataStream") instanceof String dataStream) {
                body.put("dataStream", renderText(dataStream, context));
            }
        }
        return rendered;
    }

    private List<JSONObject> remapMergeDataForBlock(JSONArray templateMergeData,
                                                    List<RenderedWorkbookRow> renderedRows,
                                                    int baseRow) {
        if (templateMergeData == null || templateMergeData.isEmpty()) {
            return List.of();
        }
        if (renderedRows == null || renderedRows.isEmpty()) {
            return List.of();
        }

        Map<Integer, List<Integer>> sourceRowPositions = new HashMap<>();
        for (int i = 0; i < renderedRows.size(); i++) {
            int sourceRowIndex = renderedRows.get(i).sourceRowIndex;
            sourceRowPositions.computeIfAbsent(sourceRowIndex, k -> new ArrayList<>()).add(i);
        }

        List<JSONObject> out = new ArrayList<>();
        for (int i = 0; i < templateMergeData.size(); i++) {
            JSONObject mergeObj = asJsonObject(templateMergeData.get(i));
            if (mergeObj == null || mergeObj.isEmpty()) {
                continue;
            }
            Integer rawStartRow = toInteger(mergeObj.get("startRow"));
            Integer rawEndRow = toInteger(mergeObj.get("endRow"));
            Integer rawStartColumn = toInteger(mergeObj.get("startColumn"));
            Integer rawEndColumn = toInteger(mergeObj.get("endColumn"));
            if (rawStartRow == null || rawEndRow == null || rawStartColumn == null || rawEndColumn == null) {
                continue;
            }

            int startRow = Math.min(rawStartRow, rawEndRow);
            int endRow = Math.max(rawStartRow, rawEndRow);
            int startColumn = Math.min(rawStartColumn, rawEndColumn);
            int endColumn = Math.max(rawStartColumn, rawEndColumn);

            if (startRow == endRow) {
                List<Integer> localRows = sourceRowPositions.getOrDefault(startRow, List.of());
                for (Integer localRow : localRows) {
                    int absRow = baseRow + localRow;
                    out.add(buildRemappedMerge(mergeObj, absRow, absRow, startColumn, endColumn));
                }
                continue;
            }

            List<RowMatch> filtered = new ArrayList<>();
            for (int localRow = 0; localRow < renderedRows.size(); localRow++) {
                int sourceRow = renderedRows.get(localRow).sourceRowIndex;
                if (sourceRow >= startRow && sourceRow <= endRow) {
                    filtered.add(new RowMatch(localRow, sourceRow));
                }
            }
            if (filtered.isEmpty()) {
                continue;
            }

            int groupStart = 0;
            for (int idx = 1; idx <= filtered.size(); idx++) {
                boolean shouldSplit;
                if (idx == filtered.size()) {
                    shouldSplit = true;
                } else {
                    RowMatch prev = filtered.get(idx - 1);
                    RowMatch curr = filtered.get(idx);
                    shouldSplit = curr.localRow != prev.localRow + 1 || curr.sourceRow < prev.sourceRow;
                }
                if (!shouldSplit) {
                    continue;
                }

                List<RowMatch> group = filtered.subList(groupStart, idx);
                boolean hasStart = false;
                boolean hasEnd = false;
                for (RowMatch rowMatch : group) {
                    if (rowMatch.sourceRow == startRow) {
                        hasStart = true;
                    }
                    if (rowMatch.sourceRow == endRow) {
                        hasEnd = true;
                    }
                }
                if (hasStart && hasEnd) {
                    int remapStartRow = baseRow + group.get(0).localRow;
                    int remapEndRow = baseRow + group.get(group.size() - 1).localRow;
                    out.add(buildRemappedMerge(mergeObj, remapStartRow, remapEndRow, startColumn, endColumn));
                }
                groupStart = idx;
            }
        }
        return out;
    }

    private JSONObject buildRemappedMerge(JSONObject mergeObj,
                                          int startRow,
                                          int endRow,
                                          int startColumn,
                                          int endColumn) {
        JSONObject out = JSON.parseObject(mergeObj.toJSONString());
        if (!out.containsKey("rangeType")) {
            out.put("rangeType", 0);
        }
        out.put("startRow", startRow);
        out.put("endRow", endRow);
        out.put("startColumn", startColumn);
        out.put("endColumn", endColumn);
        return out;
    }

    private void applyMergedRegions(Sheet poiSheet, JSONArray mergeData) {
        if (poiSheet == null || mergeData == null || mergeData.isEmpty()) {
            return;
        }
        Set<String> dedup = new HashSet<>();
        for (int i = 0; i < mergeData.size(); i++) {
            JSONObject mergeObj = asJsonObject(mergeData.get(i));
            if (mergeObj == null || mergeObj.isEmpty()) {
                continue;
            }
            Integer rawStartRow = toInteger(mergeObj.get("startRow"));
            Integer rawEndRow = toInteger(mergeObj.get("endRow"));
            Integer rawStartColumn = toInteger(mergeObj.get("startColumn"));
            Integer rawEndColumn = toInteger(mergeObj.get("endColumn"));
            if (rawStartRow == null || rawEndRow == null || rawStartColumn == null || rawEndColumn == null) {
                continue;
            }
            int startRow = Math.max(0, Math.min(rawStartRow, rawEndRow));
            int endRow = Math.max(startRow, Math.max(rawStartRow, rawEndRow));
            int startColumn = Math.max(0, Math.min(rawStartColumn, rawEndColumn));
            int endColumn = Math.max(startColumn, Math.max(rawStartColumn, rawEndColumn));
            String key = startRow + ":" + endRow + ":" + startColumn + ":" + endColumn;
            if (!dedup.add(key)) {
                continue;
            }
            try {
                poiSheet.addMergedRegion(new CellRangeAddress(startRow, endRow, startColumn, endColumn));
            } catch (Exception ignore) {
                // Skip invalid or conflicting merged regions to keep export stable.
            }
        }
    }

    private boolean rowHasLegacyColumnExpression(JSONObject rowObj) {
        if (rowObj == null || rowObj.isEmpty()) {
            return false;
        }
        for (String key : rowObj.keySet()) {
            JSONObject cell = rowObj.getJSONObject(key);
            if (cell == null) {
                continue;
            }
            for (String text : getCellTextCandidates(cell)) {
                if (textHasLegacyColumnExpression(text)) {
                    return true;
                }
            }
        }
        return false;
    }

    private Directive getWorkbookRowDirective(JSONObject rowObj) {
        if (rowObj == null || rowObj.isEmpty()) {
            return null;
        }
        Directive directive = null;
        for (String key : rowObj.keySet()) {
            JSONObject cell = rowObj.getJSONObject(key);
            if (cell == null) {
                continue;
            }
            List<String> textList = getCellTextCandidates(cell);
            if (textList.isEmpty()) {
                continue;
            }
            for (String text : textList) {
                String trimmed = StrUtil.trimToEmpty(text);
                if (trimmed.isEmpty()) {
                    continue;
                }
                Directive parsed = parseDirectiveText(trimmed);
                if (parsed == null) {
                    return null;
                }
                if (directive == null) {
                    directive = parsed;
                    continue;
                }
                if (!directive.isSame(parsed)) {
                    return null;
                }
            }
        }
        return directive;
    }

    private List<String> getCellTextCandidates(JSONObject cell) {
        List<String> out = new ArrayList<>();
        if (cell.get("v") instanceof String v) {
            out.add(v);
        }
        if (cell.get("m") instanceof String m) {
            out.add(m);
        }
        JSONObject p = cell.getJSONObject("p");
        if (p == null) {
            return out;
        }
        JSONObject body = p.getJSONObject("body");
        if (body != null && body.get("dataStream") instanceof String dataStream) {
            out.add(dataStream);
        }
        return out;
    }

    private String renderText(String text, Map<String, Object> context) {
        if (text == null) {
            return "";
        }
        Matcher matcher = EXPRESSION_RE.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String expr = StrUtil.trimToEmpty(matcher.group(1));
            Object value = resolvePath(context, expr);
            matcher.appendReplacement(sb, Matcher.quoteReplacement(formatValue(value)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private Object resolvePath(Map<String, Object> context, String expr) {
        if (StrUtil.isBlank(expr)) {
            return "";
        }
        String key = StrUtil.trim(expr);
        if ("UUID".equals(key)) {
            return IdUtil.fastSimpleUUID();
        }
        if ("nanoId".equals(key)) {
            return IdUtil.nanoId();
        }
        if ("order".equals(key)) {
            Object order = context.get("order");
            return order == null ? 1 : order;
        }

        String[] parts = key.split("\\.");
        Object cursor = context;
        for (String part : parts) {
            if (cursor == null) {
                return "";
            }
            if (cursor instanceof Map<?, ?> map) {
                cursor = map.get(part);
                continue;
            }
            if (cursor instanceof JSONObject jsonObj) {
                cursor = jsonObj.get(part);
                continue;
            }
            return "";
        }
        return cursor == null ? "" : cursor;
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Boolean boolVal) {
            return boolVal ? "YES" : "NO";
        }
        if (value instanceof Map<?, ?> || value instanceof List<?> || value instanceof JSONObject || value instanceof JSONArray) {
            return "";
        }
        return String.valueOf(value);
    }

    private Directive parseDirectiveText(String text) {
        String content = StrUtil.trimToEmpty(text);
        if (content.isEmpty()) {
            return null;
        }
        if (END_DIRECTIVE_RE.matcher(content).matches()) {
            return Directive.end();
        }
        Matcher matcher = FOR_DIRECTIVE_RE.matcher(content);
        if (!matcher.matches()) {
            return null;
        }
        return Directive.forLoop(matcher.group(1), matcher.group(2));
    }

    private boolean textHasLegacyColumnExpression(String text) {
        if (StrUtil.isBlank(text)) {
            return false;
        }
        Matcher matcher = EXPRESSION_RE.matcher(text);
        while (matcher.find()) {
            String expr = StrUtil.trimToEmpty(matcher.group(1));
            if (isLegacyColumnExpression(expr)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLegacyColumnExpression(String expr) {
        if (COLUMN_FIELD_SET.contains(expr)) {
            return true;
        }
        int idx = expr.indexOf('.');
        if (idx <= 0 || idx >= expr.length() - 1) {
            return false;
        }
        String right = StrUtil.trim(expr.substring(idx + 1));
        return COLUMN_FIELD_SET.contains(right);
    }

    private Map<String, Object> buildTableContext(TableVO tableVO, int tableOrder) {
        Map<String, Object> tableMap = normalizeTable(tableVO, tableOrder);
        Map<String, Object> context = new HashMap<>(tableMap);
        context.put("table", tableMap);
        context.put("order", tableOrder);
        context.put(CTX_IN_FOR, false);
        return context;
    }

    private Map<String, Object> buildLoopContext(Map<String, Object> parent, String alias, Object item, int loopOrder) {
        Map<String, Object> next = new HashMap<>(parent);
        next.put(alias, item);
        next.put("order", loopOrder);
        next.put(CTX_IN_FOR, true);
        if (item instanceof Map<?, ?> itemMap) {
            for (Map.Entry<?, ?> entry : itemMap.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    continue;
                }
                next.putIfAbsent(key, entry.getValue());
            }
        }
        return next;
    }

    private Map<String, Object> normalizeTable(TableVO tableVO, int tableOrder) {
        List<Map<String, Object>> columnList = new ArrayList<>();
        List<ColumnVO> sourceColumns = safeColumns(tableVO);
        for (int i = 0; i < sourceColumns.size(); i++) {
            columnList.add(normalizeColumn(sourceColumns.get(i), i + 1));
        }

        Map<String, Object> tableMap = new HashMap<>();
        tableMap.put("schema", StrUtil.nullToEmpty(tableVO.getSchema()));
        tableMap.put("catalog", StrUtil.nullToEmpty(tableVO.getCatalog()));
        tableMap.put("tableName", StrUtil.nullToEmpty(tableVO.getTableName()));
        tableMap.put("tableComment", StrUtil.nullToEmpty(tableVO.getComment()));
        tableMap.put("comment", StrUtil.nullToEmpty(tableVO.getComment()));
        tableMap.put("numRows", tableVO.getNumRows() == null ? "" : tableVO.getNumRows());
        tableMap.put("order", tableOrder);
        tableMap.put("columnList", columnList);
        return tableMap;
    }

    private Map<String, Object> normalizeColumn(ColumnVO columnVO, int order) {
        String name = StrUtil.nullToEmpty(columnVO.getName());
        String typeName = StrUtil.nullToEmpty(columnVO.getTypeName());
        String nullable = columnVO.isNullable() ? "YES" : "NO";
        String autoIncrement = columnVO.isAutoIncrement() ? "YES" : "NO";
        String pk = columnVO.isPk() ? "YES" : "NO";
        String def = StrUtil.nullToEmpty(columnVO.getColumnDef());
        String comment = StrUtil.nullToEmpty(columnVO.getComment());

        Map<String, Object> col = new HashMap<>();
        col.put("order", order);
        col.put("name", name);
        col.put("type", typeName);
        col.put("typeName", typeName);
        col.put("size", columnVO.getSize());
        col.put("digit", columnVO.getDigit() == null ? "" : columnVO.getDigit());
        col.put("nullable", nullable);
        col.put("autoIncrement", autoIncrement);
        col.put("pk", pk);
        col.put("def", def);
        col.put("columnDef", def);
        col.put("comment", comment);
        col.put("columnName", name);
        col.put("columnType", typeName);
        col.put("columnSize", columnVO.getSize());
        col.put("columnDigit", columnVO.getDigit() == null ? "" : columnVO.getDigit());
        col.put("columnNullable", nullable);
        col.put("columnAutoIncrement", autoIncrement);
        col.put("columnPk", pk);
        col.put("columnComment", comment);
        return col;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getContextColumnList(Map<String, Object> context) {
        Object columnListObj = context.get("columnList");
        if (!(columnListObj instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?>) {
                out.add((Map<String, Object>) item);
            }
        }
        return out;
    }

    private List<Map<String, Object>> resolveLoopSource(Map<String, Object> context, String listExpr) {
        Object source = resolvePath(context, listExpr);
        if (!(source instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (listExpr != null && listExpr.endsWith("columnList") && item instanceof Map<?, ?> itemMap) {
                out.add(normalizeLoopColumnMap(itemMap, i + 1));
                continue;
            }
            if (item instanceof Map<?, ?> itemMap) {
                @SuppressWarnings("unchecked")
                Map<String, Object> row = (Map<String, Object>) itemMap;
                out.add(row);
            }
        }
        return out;
    }

    private Map<String, Object> normalizeLoopColumnMap(Map<?, ?> itemMap, int order) {
        String name = valueToString(itemMap.get("name"));
        String typeName = valueToString(itemMap.containsKey("typeName") ? itemMap.get("typeName") : itemMap.get("type"));
        String nullable = toYesNo(valueToString(itemMap.get("nullable")));
        String autoIncrement = toYesNo(valueToString(itemMap.get("autoIncrement")));
        String pk = toYesNo(valueToString(itemMap.get("pk")));
        String def = valueToString(itemMap.containsKey("def") ? itemMap.get("def") : itemMap.get("columnDef"));
        String comment = valueToString(itemMap.get("comment"));

        Map<String, Object> col = new HashMap<>();
        col.put("order", itemMap.containsKey("order") ? itemMap.get("order") : order);
        col.put("name", name);
        col.put("type", typeName);
        col.put("typeName", typeName);
        col.put("size", getMapValueOrDefault(itemMap, "size", ""));
        col.put("digit", getMapValueOrDefault(itemMap, "digit", ""));
        col.put("nullable", nullable);
        col.put("autoIncrement", autoIncrement);
        col.put("pk", pk);
        col.put("def", def);
        col.put("columnDef", def);
        col.put("comment", comment);
        col.put("columnName", name);
        col.put("columnType", typeName);
        col.put("columnSize", getMapValueOrDefault(itemMap, "size", ""));
        col.put("columnDigit", getMapValueOrDefault(itemMap, "digit", ""));
        col.put("columnNullable", nullable);
        col.put("columnAutoIncrement", autoIncrement);
        col.put("columnPk", pk);
        col.put("columnComment", comment);
        return col;
    }

    private String toYesNo(String raw) {
        if (StrUtil.isBlank(raw)) {
            return "";
        }
        String normalized = raw.trim().toLowerCase();
        if ("true".equals(normalized) || "yes".equals(normalized) || "1".equals(normalized) || "y".equals(normalized)) {
            return "YES";
        }
        if ("false".equals(normalized) || "no".equals(normalized) || "0".equals(normalized) || "n".equals(normalized)) {
            return "NO";
        }
        return raw;
    }

    private String valueToString(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value);
    }

    private Object getMapValueOrDefault(Map<?, ?> map, String key, Object defaultValue) {
        Object value = map.get(key);
        return value == null ? defaultValue : value;
    }

    private JSONObject resolveStyleData(JSONObject cellObj, JSONObject styles) {
        Object s = cellObj.get("s");
        if (s instanceof JSONObject styleObj) {
            return styleObj;
        }
        if (s instanceof String styleId && styles != null) {
            return styles.getJSONObject(styleId);
        }
        return null;
    }

    private CellStyle toPoiCellStyle(Workbook wb,
                                     DataFormat dataFormat,
                                     JSONObject styleData,
                                     Map<String, CellStyle> cellStyleCache,
                                     Map<String, Font> fontCache) {
        if (styleData == null || styleData.isEmpty()) {
            return null;
        }
        String styleKey = styleData.toJSONString();
        CellStyle cached = cellStyleCache.get(styleKey);
        if (cached != null) {
            return cached;
        }

        CellStyle cellStyle = wb.createCellStyle();

        Integer ht = readInt(styleData, "ht");
        if (ht != null) {
            cellStyle.setAlignment(toHorizontalAlignment(ht));
        }

        Integer vt = readInt(styleData, "vt");
        if (vt != null) {
            cellStyle.setVerticalAlignment(toVerticalAlignment(vt));
        }

        Integer tb = readInt(styleData, "tb");
        if (tb != null) {
            cellStyle.setWrapText(tb == 1);
        }

        String numberFmt = readString(styleData, "n");
        if (StrUtil.isNotBlank(numberFmt)) {
            cellStyle.setDataFormat(dataFormat.getFormat(numberFmt));
        }

        String bgRgb = readRgb(styleData.get("bg"));
        Short bgColor = toIndexedColor(bgRgb);
        if (bgColor != null) {
            cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cellStyle.setFillForegroundColor(bgColor);
        }

        JSONObject bd = styleData.getJSONObject("bd");
        if (bd != null) {
            applyBorder(cellStyle, bd.getJSONObject("t"), Side.TOP);
            applyBorder(cellStyle, bd.getJSONObject("r"), Side.RIGHT);
            applyBorder(cellStyle, bd.getJSONObject("b"), Side.BOTTOM);
            applyBorder(cellStyle, bd.getJSONObject("l"), Side.LEFT);
        }

        Font font = toPoiFont(wb, styleData, fontCache);
        if (font != null) {
            cellStyle.setFont(font);
        }

        cellStyleCache.put(styleKey, cellStyle);
        return cellStyle;
    }

    private Font toPoiFont(Workbook wb, JSONObject styleData, Map<String, Font> fontCache) {
        String key = String.join("|",
                StrUtil.nullToEmpty(readString(styleData, "ff")),
                String.valueOf(readInt(styleData, "fs")),
                String.valueOf(readInt(styleData, "bl")),
                String.valueOf(readInt(styleData, "it")),
                String.valueOf(readInt(styleData, "ul")),
                String.valueOf(readInt(styleData, "st")),
                StrUtil.nullToEmpty(readRgb(styleData.get("cl")))
        );
        Font cached = fontCache.get(key);
        if (cached != null) {
            return cached;
        }

        Font font = wb.createFont();
        String ff = readString(styleData, "ff");
        if (StrUtil.isNotBlank(ff)) {
            font.setFontName(ff);
        }
        Integer fs = readInt(styleData, "fs");
        if (fs != null && fs > 0) {
            font.setFontHeightInPoints(fs.shortValue());
        }
        if (Integer.valueOf(1).equals(readInt(styleData, "bl"))) {
            font.setBold(true);
        }
        if (Integer.valueOf(1).equals(readInt(styleData, "it"))) {
            font.setItalic(true);
        }
        if (Integer.valueOf(1).equals(readInt(styleData, "ul"))) {
            font.setUnderline(Font.U_SINGLE);
        }
        if (Integer.valueOf(1).equals(readInt(styleData, "st"))) {
            font.setStrikeout(true);
        }

        Short color = toIndexedColor(readRgb(styleData.get("cl")));
        if (color != null) {
            font.setColor(color);
        }

        fontCache.put(key, font);
        return font;
    }

    private void applyBorder(CellStyle cellStyle, JSONObject borderObj, Side side) {
        if (borderObj == null || borderObj.isEmpty()) {
            return;
        }
        BorderStyle borderStyle = toBorderStyle(toInteger(borderObj.get("s")));
        Short color = toIndexedColor(readRgb(borderObj.get("cl")));

        switch (side) {
            case TOP -> {
                cellStyle.setBorderTop(borderStyle);
                if (color != null) {
                    cellStyle.setTopBorderColor(color);
                }
            }
            case RIGHT -> {
                cellStyle.setBorderRight(borderStyle);
                if (color != null) {
                    cellStyle.setRightBorderColor(color);
                }
            }
            case BOTTOM -> {
                cellStyle.setBorderBottom(borderStyle);
                if (color != null) {
                    cellStyle.setBottomBorderColor(color);
                }
            }
            case LEFT -> {
                cellStyle.setBorderLeft(borderStyle);
                if (color != null) {
                    cellStyle.setLeftBorderColor(color);
                }
            }
        }
    }

    private BorderStyle toBorderStyle(Integer code) {
        if (code == null) {
            return BorderStyle.NONE;
        }
        for (BorderStyle bs : BorderStyle.values()) {
            if (bs.getCode() == code.shortValue()) {
                return bs;
            }
        }
        return BorderStyle.NONE;
    }

    private Integer readInt(JSONObject obj, String key) {
        if (obj == null) {
            return null;
        }
        return toInteger(obj.get(key));
    }

    private String readString(JSONObject obj, String key) {
        if (obj == null) {
            return null;
        }
        return toStringValue(obj.get(key));
    }

    private Integer toInteger(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Integer i) {
            return i;
        }
        if (raw instanceof Number n) {
            return n.intValue();
        }
        if (raw instanceof Boolean b) {
            return b ? 1 : 0;
        }
        if (raw instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (Exception ignore) {
                return null;
            }
        }
        if (raw instanceof JSONObject jsonObj) {
            Integer val = toInteger(jsonObj.get("v"));
            if (val != null) return val;
            val = toInteger(jsonObj.get("value"));
            if (val != null) return val;
            val = toInteger(jsonObj.get("s"));
            if (val != null) return val;
            return toInteger(jsonObj.get("id"));
        }
        if (raw instanceof JSONArray arr && !arr.isEmpty()) {
            return toInteger(arr.get(0));
        }
        return null;
    }

    private String toStringValue(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof String s) {
            return s;
        }
        if (raw instanceof Number || raw instanceof Boolean) {
            return String.valueOf(raw);
        }
        if (raw instanceof JSONObject jsonObj) {
            String v = toStringValue(jsonObj.get("v"));
            if (StrUtil.isNotBlank(v)) return v;
            v = toStringValue(jsonObj.get("value"));
            if (StrUtil.isNotBlank(v)) return v;
            return toStringValue(jsonObj.get("text"));
        }
        if (raw instanceof JSONArray arr && !arr.isEmpty()) {
            return toStringValue(arr.get(0));
        }
        return null;
    }

    private String readRgb(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof String s) {
            return s;
        }
        if (raw instanceof JSONObject jsonObj) {
            String rgb = toStringValue(jsonObj.get("rgb"));
            if (StrUtil.isNotBlank(rgb)) {
                return rgb;
            }
            return readRgb(jsonObj.get("cl"));
        }
        return null;
    }

    private HorizontalAlignment toHorizontalAlignment(Integer ht) {
        if (ht == null) {
            return HorizontalAlignment.GENERAL;
        }
        return switch (ht) {
            case 1 -> HorizontalAlignment.LEFT;
            case 2 -> HorizontalAlignment.CENTER;
            case 3 -> HorizontalAlignment.RIGHT;
            default -> HorizontalAlignment.GENERAL;
        };
    }

    private VerticalAlignment toVerticalAlignment(Integer vt) {
        if (vt == null) {
            return VerticalAlignment.BOTTOM;
        }
        return switch (vt) {
            case 0 -> VerticalAlignment.TOP;
            case 1 -> VerticalAlignment.CENTER;
            case 2 -> VerticalAlignment.BOTTOM;
            default -> VerticalAlignment.BOTTOM;
        };
    }

    private Short toIndexedColor(String rgb) {
        int[] target = parseRgb(rgb);
        if (target == null) {
            return null;
        }
        short best = 0;
        int bestDiff = Integer.MAX_VALUE;
        for (Map.Entry<Short, int[]> entry : INDEXED_COLOR_RGB.entrySet()) {
            int[] c = entry.getValue();
            int diff = Math.abs(c[0] - target[0]) + Math.abs(c[1] - target[1]) + Math.abs(c[2] - target[2]);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = entry.getKey();
            }
        }
        return best;
    }

    private int[] parseRgb(String rgb) {
        if (StrUtil.isBlank(rgb)) {
            return null;
        }
        String text = rgb.trim();
        try {
            if (text.startsWith("#")) {
                String hex = text.substring(1);
                if (hex.length() == 3) {
                    int r = Integer.parseInt(hex.substring(0, 1) + hex.substring(0, 1), 16);
                    int g = Integer.parseInt(hex.substring(1, 2) + hex.substring(1, 2), 16);
                    int b = Integer.parseInt(hex.substring(2, 3) + hex.substring(2, 3), 16);
                    return new int[]{r, g, b};
                }
                if (hex.length() == 6) {
                    int r = Integer.parseInt(hex.substring(0, 2), 16);
                    int g = Integer.parseInt(hex.substring(2, 4), 16);
                    int b = Integer.parseInt(hex.substring(4, 6), 16);
                    return new int[]{r, g, b};
                }
            }
            String low = text.toLowerCase();
            if (low.startsWith("rgb(") && low.endsWith(")")) {
                String content = low.substring(4, low.length() - 1);
                String[] arr = content.split(",");
                if (arr.length == 3) {
                    int r = Integer.parseInt(arr[0].trim());
                    int g = Integer.parseInt(arr[1].trim());
                    int b = Integer.parseInt(arr[2].trim());
                    return new int[]{r, g, b};
                }
            }
        } catch (Exception ignore) {
            return null;
        }
        return null;
    }

    private void writeCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setBlank();
            return;
        }
        if (value instanceof Number num) {
            cell.setCellValue(num.doubleValue());
            return;
        }
        if (value instanceof Boolean boolVal) {
            cell.setCellValue(boolVal);
            return;
        }
        cell.setCellValue(String.valueOf(value));
    }

    private enum Side {
        TOP,
        RIGHT,
        BOTTOM,
        LEFT
    }

    private static final Map<Short, int[]> INDEXED_COLOR_RGB;

    static {
        Map<Short, int[]> m = new HashMap<>();
        m.put((short) 8, new int[]{0, 0, 0});         // black
        m.put((short) 9, new int[]{255, 255, 255});   // white
        m.put((short) 10, new int[]{255, 0, 0});      // red
        m.put((short) 11, new int[]{0, 255, 0});      // bright green
        m.put((short) 12, new int[]{0, 0, 255});      // blue
        m.put((short) 13, new int[]{255, 255, 0});    // yellow
        m.put((short) 14, new int[]{255, 0, 255});    // pink/magenta
        m.put((short) 15, new int[]{0, 255, 255});    // turquoise/cyan
        m.put((short) 16, new int[]{128, 0, 0});      // dark red
        m.put((short) 17, new int[]{0, 128, 0});      // green
        m.put((short) 18, new int[]{0, 0, 128});      // dark blue
        m.put((short) 22, new int[]{128, 128, 128});  // grey
        m.put((short) 42, new int[]{204, 255, 204});  // light green used by header in current templates
        INDEXED_COLOR_RGB = Collections.unmodifiableMap(m);
    }

    private boolean isInForContext(Map<String, Object> context) {
        Object inFor = context.get(CTX_IN_FOR);
        return inFor instanceof Boolean boolVal && boolVal;
    }

    private List<ColumnVO> safeColumns(TableVO tableVO) {
        return tableVO.getColumnList() == null ? List.of() : tableVO.getColumnList();
    }

    private Object extractCellValue(JSONObject cellObj) {
        if (cellObj == null) {
            return null;
        }
        Object val = cellObj.get("v");
        if (val != null) {
            return val;
        }
        Object markdownVal = cellObj.get("m");
        if (markdownVal != null) {
            return markdownVal;
        }
        JSONObject p = cellObj.getJSONObject("p");
        if (p == null) {
            return null;
        }
        JSONObject body = p.getJSONObject("body");
        if (body != null && body.get("dataStream") instanceof String dataStream) {
            return trimCellDataStream(dataStream);
        }
        return null;
    }

    private String trimCellDataStream(String dataStream) {
        if (dataStream == null) {
            return "";
        }
        String out = dataStream;
        if (out.endsWith("\r\n")) {
            out = out.substring(0, out.length() - 2);
        } else if (out.endsWith("\n")) {
            out = out.substring(0, out.length() - 1);
        }
        return out;
    }

    private Integer parseInteger(String raw) {
        if (StrUtil.isBlank(raw)) {
            return null;
        }
        try {
            return Integer.parseInt(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private JSONObject parseJsonObject(String text) {
        try {
            return JSON.parseObject(text);
        } catch (Exception e) {
            return null;
        }
    }

    private JSONObject asJsonObject(Object raw) {
        if (raw instanceof JSONObject jsonObj) {
            return jsonObj;
        }
        if (raw == null) {
            return null;
        }
        try {
            return JSON.parseObject(JSON.toJSONString(raw));
        } catch (Exception e) {
            return null;
        }
    }

    private <T, R> List<R> renderTemplateItems(List<T> items,
                                               Map<String, Object> context,
                                               DirectiveExtractor<T> directiveExtractor,
                                               ItemRenderer<T, R> renderer) {
        List<R> output = new ArrayList<>();
        int i = 0;
        while (i < items.size()) {
            T item = items.get(i);
            Directive directive = directiveExtractor.resolve(item);

            if (directive != null && directive.type == DirectiveType.FOR) {
                int endIndex = findForEndIndex(items, i + 1, directiveExtractor);
                if (endIndex < 0) {
                    output.addAll(renderer.render(item, context));
                    i++;
                    continue;
                }

                List<T> blockItems = items.subList(i + 1, endIndex);
                List<Map<String, Object>> loopList = resolveLoopSource(context, directive.listExpr);
                for (int idx = 0; idx < loopList.size(); idx++) {
                    Map<String, Object> loopCtx = buildLoopContext(context, directive.alias, loopList.get(idx), idx + 1);
                    output.addAll(renderTemplateItems(blockItems, loopCtx, directiveExtractor, renderer));
                }
                i = endIndex + 1;
                continue;
            }

            if (directive != null && directive.type == DirectiveType.END) {
                i++;
                continue;
            }

            output.addAll(renderer.render(item, context));
            i++;
        }
        return output;
    }

    private <T> int findForEndIndex(List<T> items, int fromIndex, DirectiveExtractor<T> directiveExtractor) {
        int depth = 0;
        for (int i = fromIndex; i < items.size(); i++) {
            Directive directive = directiveExtractor.resolve(items.get(i));
            if (directive == null) {
                continue;
            }
            if (directive.type == DirectiveType.FOR) {
                depth++;
                continue;
            }
            if (directive.type == DirectiveType.END) {
                if (depth == 0) {
                    return i;
                }
                depth--;
            }
        }
        return -1;
    }

    @FunctionalInterface
    private interface DirectiveExtractor<T> {
        Directive resolve(T item);
    }

    @FunctionalInterface
    private interface ItemRenderer<T, R> {
        List<R> render(T item, Map<String, Object> context);
    }

    private static class WorkbookRowTemplate {
        private final int rowIndex;
        private final JSONObject rowObj;

        private WorkbookRowTemplate(int rowIndex, JSONObject rowObj) {
            this.rowIndex = rowIndex;
            this.rowObj = rowObj;
        }
    }

    private static class RenderedWorkbookRow {
        private final int sourceRowIndex;
        private final JSONObject rowObj;

        private RenderedWorkbookRow(int sourceRowIndex, JSONObject rowObj) {
            this.sourceRowIndex = sourceRowIndex;
            this.rowObj = rowObj;
        }
    }

    private static class RowMatch {
        private final int localRow;
        private final int sourceRow;

        private RowMatch(int localRow, int sourceRow) {
            this.localRow = localRow;
            this.sourceRow = sourceRow;
        }
    }

    private enum DirectiveType {
        FOR,
        END
    }

    private static class Directive {
        private final DirectiveType type;
        private final String alias;
        private final String listExpr;

        private Directive(DirectiveType type, String alias, String listExpr) {
            this.type = type;
            this.alias = alias;
            this.listExpr = listExpr;
        }

        private static Directive forLoop(String alias, String listExpr) {
            return new Directive(DirectiveType.FOR, alias, listExpr);
        }

        private static Directive end() {
            return new Directive(DirectiveType.END, null, null);
        }

        private boolean isSame(Directive other) {
            if (other == null || this.type != other.type) {
                return false;
            }
            if (this.type == DirectiveType.END) {
                return true;
            }
            return StrUtil.equals(this.alias, other.alias) && StrUtil.equals(this.listExpr, other.listExpr);
        }
    }
}
