package org.wsitm.schemax.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Service;
import org.wsitm.schemax.entity.vo.ColumnVO;
import org.wsitm.schemax.entity.vo.TableVO;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class TemplateRenderService {

    private static final String PH_SCHEMA = "${schema}";
    private static final String PH_CATALOG = "${catalog}";
    private static final String PH_TABLE_NAME = "${tableName}";
    private static final String PH_TABLE_COMMENT = "${tableComment}";
    private static final String PH_NUM_ROWS = "${numRows}";

    private static final String PH_COLUMN_NAME = "${columnName}";
    private static final String PH_COLUMN_TYPE = "${columnType}";
    private static final String PH_COLUMN_SIZE = "${columnSize}";
    private static final String PH_COLUMN_DIGIT = "${columnDigit}";
    private static final String PH_COLUMN_NULLABLE = "${columnNullable}";
    private static final String PH_COLUMN_AUTO_INC = "${columnAutoIncrement}";
    private static final String PH_COLUMN_PK = "${columnPk}";
    private static final String PH_COLUMN_DEF = "${columnDef}";
    private static final String PH_COLUMN_COMMENT = "${columnComment}";

    private static final String PH_UUID = "${UUID}";
    private static final String PH_NANO_ID = "${nanoId}";
    private static final String PH_ORDER = "${order}";

    private static final Set<String> COLUMN_PLACEHOLDERS = Set.of(
            PH_COLUMN_NAME,
            PH_COLUMN_TYPE,
            PH_COLUMN_SIZE,
            PH_COLUMN_DIGIT,
            PH_COLUMN_NULLABLE,
            PH_COLUMN_AUTO_INC,
            PH_COLUMN_PK,
            PH_COLUMN_DEF,
            PH_COLUMN_COMMENT
    );

    public String renderMarkdown(List<TableVO> tableVOList, String templateContent) {
        if (StrUtil.isBlank(templateContent)) {
            return "";
        }
        if (tableVOList == null || tableVOList.isEmpty()) {
            return "";
        }

        List<String> blockList = new ArrayList<>();
        String[] lineArr = templateContent.split("\\r?\\n", -1);
        for (int tIndex = 0; tIndex < tableVOList.size(); tIndex++) {
            TableVO tableVO = tableVOList.get(tIndex);
            List<String> renderedLines = new ArrayList<>();
            for (String line : lineArr) {
                if (hasColumnPlaceholder(line)) {
                    List<ColumnVO> columns = safeColumns(tableVO);
                    if (columns.isEmpty()) {
                        renderedLines.add(replaceAll(line, tableVO, null, tIndex + 1, null));
                    } else {
                        for (int cIndex = 0; cIndex < columns.size(); cIndex++) {
                            renderedLines.add(replaceAll(line, tableVO, columns.get(cIndex), tIndex + 1, cIndex + 1));
                        }
                    }
                } else {
                    renderedLines.add(replaceAll(line, tableVO, null, tIndex + 1, null));
                }
            }
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
                    .map(Integer::parseInt)
                    .sorted()
                    .toList();

            JSONObject newCellData = new JSONObject();
            int currentRow = 0;
            for (int tIndex = 0; tIndex < tableVOList.size(); tIndex++) {
                TableVO tableVO = tableVOList.get(tIndex);
                for (Integer rowIndex : rowIndexList) {
                    JSONObject rowObj = cellData.getJSONObject(String.valueOf(rowIndex));
                    if (rowObj == null) {
                        newCellData.put(String.valueOf(currentRow++), new JSONObject());
                        continue;
                    }

                    if (rowHasColumnPlaceholder(rowObj)) {
                        List<ColumnVO> columns = safeColumns(tableVO);
                        if (columns.isEmpty()) {
                            newCellData.put(String.valueOf(currentRow++), renderRow(rowObj, tableVO, null, tIndex + 1, null));
                        } else {
                            for (int cIndex = 0; cIndex < columns.size(); cIndex++) {
                                newCellData.put(
                                        String.valueOf(currentRow++),
                                        renderRow(rowObj, tableVO, columns.get(cIndex), tIndex + 1, cIndex + 1)
                                );
                            }
                        }
                    } else {
                        newCellData.put(String.valueOf(currentRow++), renderRow(rowObj, tableVO, null, tIndex + 1, null));
                    }
                }
                if (tIndex < tableVOList.size() - 1) {
                    currentRow++;
                }
            }

            sheet.put("cellData", newCellData);
            sheet.put("rowCount", Math.max(currentRow + 10, currentRow));
            sheet.put("mergeData", new JSONArray());
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
                .map(Integer::parseInt)
                .sorted()
                .toList();

        try (ExcelWriter excelWriter = ExcelUtil.getBigWriter(file)) {
            for (Integer rowIndex : rowIndexList) {
                JSONObject rowObj = cellData.getJSONObject(String.valueOf(rowIndex));
                if (rowObj == null || rowObj.isEmpty()) {
                    excelWriter.writeRow(new ArrayList<>());
                    continue;
                }

                List<Integer> colIndexList = rowObj.keySet().stream()
                        .map(Integer::parseInt)
                        .sorted(Comparator.naturalOrder())
                        .toList();

                int maxCol = colIndexList.get(colIndexList.size() - 1);
                List<Object> rowValues = new ArrayList<>(maxCol + 1);
                for (int i = 0; i <= maxCol; i++) {
                    rowValues.add(null);
                }
                for (Integer colIndex : colIndexList) {
                    JSONObject cellObj = rowObj.getJSONObject(String.valueOf(colIndex));
                    rowValues.set(colIndex, extractCellValue(cellObj));
                }
                excelWriter.writeRow(rowValues);
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
                        excelWriter.setColumnWidth(Integer.parseInt(key), colWidth);
                    }
                }
            }

            excelWriter.flush();
        }
    }

    private JSONObject renderRow(JSONObject rowObj, TableVO tableVO, ColumnVO columnVO, int tableOrder, Integer columnOrder) {
        JSONObject result = new JSONObject();
        for (String colKey : rowObj.keySet()) {
            JSONObject srcCell = rowObj.getJSONObject(colKey);
            if (srcCell == null) {
                continue;
            }
            JSONObject cell = JSON.parseObject(srcCell.toJSONString());
            if (cell.get("v") instanceof String val) {
                cell.put("v", replaceAll(val, tableVO, columnVO, tableOrder, columnOrder));
            }
            if (cell.get("m") instanceof String val) {
                cell.put("m", replaceAll(val, tableVO, columnVO, tableOrder, columnOrder));
            }
            result.put(colKey, cell);
        }
        return result;
    }

    private boolean rowHasColumnPlaceholder(JSONObject rowObj) {
        for (String key : rowObj.keySet()) {
            JSONObject cell = rowObj.getJSONObject(key);
            if (cell == null) {
                continue;
            }
            Object vObj = cell.get("v");
            if (vObj instanceof String str && hasColumnPlaceholder(str)) {
                return true;
            }
            Object mObj = cell.get("m");
            if (mObj instanceof String str && hasColumnPlaceholder(str)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasColumnPlaceholder(String text) {
        if (StrUtil.isBlank(text)) {
            return false;
        }
        for (String placeholder : COLUMN_PLACEHOLDERS) {
            if (text.contains(placeholder)) {
                return true;
            }
        }
        return false;
    }

    private String replaceAll(String text, TableVO tableVO, ColumnVO columnVO, int tableOrder, Integer columnOrder) {
        String result = StrUtil.nullToEmpty(text);
        result = StrUtil.replace(result, PH_SCHEMA, StrUtil.nullToEmpty(tableVO.getSchema()));
        result = StrUtil.replace(result, PH_CATALOG, StrUtil.nullToEmpty(tableVO.getCatalog()));
        result = StrUtil.replace(result, PH_TABLE_NAME, StrUtil.nullToEmpty(tableVO.getTableName()));
        result = StrUtil.replace(result, PH_TABLE_COMMENT, StrUtil.nullToEmpty(tableVO.getComment()));
        result = StrUtil.replace(result, PH_NUM_ROWS, String.valueOf(tableVO.getNumRows() == null ? "" : tableVO.getNumRows()));

        if (columnVO != null) {
            result = StrUtil.replace(result, PH_COLUMN_NAME, StrUtil.nullToEmpty(columnVO.getName()));
            result = StrUtil.replace(result, PH_COLUMN_TYPE, StrUtil.nullToEmpty(columnVO.getTypeName()));
            result = StrUtil.replace(result, PH_COLUMN_SIZE, String.valueOf(columnVO.getSize()));
            result = StrUtil.replace(result, PH_COLUMN_DIGIT, String.valueOf(columnVO.getDigit() == null ? "" : columnVO.getDigit()));
            result = StrUtil.replace(result, PH_COLUMN_NULLABLE, columnVO.isNullable() ? "YES" : "NO");
            result = StrUtil.replace(result, PH_COLUMN_AUTO_INC, columnVO.isAutoIncrement() ? "YES" : "NO");
            result = StrUtil.replace(result, PH_COLUMN_PK, columnVO.isPk() ? "YES" : "NO");
            result = StrUtil.replace(result, PH_COLUMN_DEF, StrUtil.nullToEmpty(columnVO.getColumnDef()));
            result = StrUtil.replace(result, PH_COLUMN_COMMENT, StrUtil.nullToEmpty(columnVO.getComment()));
        } else {
            result = StrUtil.replace(result, PH_COLUMN_NAME, "");
            result = StrUtil.replace(result, PH_COLUMN_TYPE, "");
            result = StrUtil.replace(result, PH_COLUMN_SIZE, "");
            result = StrUtil.replace(result, PH_COLUMN_DIGIT, "");
            result = StrUtil.replace(result, PH_COLUMN_NULLABLE, "");
            result = StrUtil.replace(result, PH_COLUMN_AUTO_INC, "");
            result = StrUtil.replace(result, PH_COLUMN_PK, "");
            result = StrUtil.replace(result, PH_COLUMN_DEF, "");
            result = StrUtil.replace(result, PH_COLUMN_COMMENT, "");
        }

        int order = columnOrder == null ? tableOrder : columnOrder;
        result = StrUtil.replace(result, PH_ORDER, String.valueOf(order));
        if (result.contains(PH_UUID)) {
            result = StrUtil.replace(result, PH_UUID, IdUtil.fastSimpleUUID());
        }
        if (result.contains(PH_NANO_ID)) {
            result = StrUtil.replace(result, PH_NANO_ID, IdUtil.nanoId());
        }
        return result;
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
        return cellObj.get("m");
    }

    private JSONObject parseJsonObject(String text) {
        try {
            return JSON.parseObject(text);
        } catch (Exception e) {
            return null;
        }
    }
}
