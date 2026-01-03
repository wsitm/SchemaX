package org.wsitm.schemax.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.wsitm.schemax.entity.core.R;
import org.wsitm.schemax.entity.vo.ConvertVO;
import org.wsitm.schemax.entity.vo.TableVO;
import org.wsitm.schemax.entity.vo.UniverSheetVO;
import org.wsitm.schemax.exception.ServiceException;
import org.wsitm.schemax.service.IConvertService;
import org.wsitm.schemax.utils.DDLUtil;
import org.wsitm.schemax.utils.PoiUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DDL转换Service业务层处理
 *
 * @author wsitm
 * @date 2025-01-27
 */
@Service
public class ConvertServiceImpl implements IConvertService {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ConvertServiceImpl.class);


    /**
     * excel文件上传
     *
     * @param file 文件
     * @return univer数据
     */
    @Override
    public R<Map<String, UniverSheetVO>> upload(MultipartFile file) {
        try (
                InputStream is = file.getInputStream();
                Workbook wb = WorkbookFactory.create(is);
        ) {
            Map<String, UniverSheetVO> result = new LinkedHashMap<>();

            int sheetSize = wb.getNumberOfSheets();
            for (int i = 0; i < sheetSize; i++) {
                Sheet sheet = wb.getSheetAt(i);
                UniverSheetVO univerSheetVO = new UniverSheetVO();
                univerSheetVO.setId("sheet-" + String.format("%02d", i + 1));
                univerSheetVO.setName(sheet.getSheetName());

                // 读取单元格数据
                Map<Integer, Map<Integer, UniverSheetVO.SheetCell>> cellRes = new LinkedHashMap<>();
                int lastRowNum = sheet.getLastRowNum();
                for (int j = 0; j <= lastRowNum; j++) {
                    Row row = sheet.getRow(j);
                    if (row == null) continue; // 跳过空行
                    Map<Integer, UniverSheetVO.SheetCell> rowData = new LinkedHashMap<>();

                    // 获取本行最后一个单元格的索引
                    int lastCellNum = row.getLastCellNum();
                    for (int k = 0; k < lastCellNum; k++) {
                        Cell cell = row.getCell(k);
                        if (cell == null) continue; // 跳过空单元格
                        Object value = PoiUtil.getCellValue(cell);
                        CellStyle cellStyle = cell.getCellStyle();

                        UniverSheetVO.SheetCell cellData = new UniverSheetVO.SheetCell();
                        cellData.setV(ObjectUtil.isNotEmpty(value) ? StrUtil.toString(value) : null);

                        UniverSheetVO.StyleData styleData = new UniverSheetVO.StyleData();
                        // 设置背景
                        UniverSheetVO.ColorStyle bgStyle = new UniverSheetVO.ColorStyle();
                        bgStyle.setRgb(PoiUtil.getColorRGB(cellStyle.getFillBackgroundColor(), wb));
                        styleData.setBg(bgStyle);

                        // 设置字体样式
                        Font font = wb.getFontAt(cellStyle.getFontIndexAsInt());
                        styleData.setBl(font.getBold() ? 1 : 0); // 加粗

                        // 设置字体颜色
                        UniverSheetVO.ColorStyle fontColor = new UniverSheetVO.ColorStyle();
                        fontColor.setRgb(PoiUtil.getFontColorRGB(font.getColor(), wb));

                        // 设置单元格边框
                        UniverSheetVO.BorderData borderData = new UniverSheetVO.BorderData();
                        borderData.setT(PoiUtil.getBorderStyleData(cellStyle.getBorderTop(), cellStyle.getTopBorderColor(), wb));
                        borderData.setR(PoiUtil.getBorderStyleData(cellStyle.getBorderRight(), cellStyle.getRightBorderColor(), wb));
                        borderData.setB(PoiUtil.getBorderStyleData(cellStyle.getBorderBottom(), cellStyle.getBottomBorderColor(), wb));
                        borderData.setL(PoiUtil.getBorderStyleData(cellStyle.getBorderLeft(), cellStyle.getLeftBorderColor(), wb));
                        styleData.setBd(borderData);

                        cellData.setS(styleData);
                        rowData.put(k, cellData);
                    }
                    if (CollUtil.isNotEmpty(rowData)) {
                        cellRes.put(j, rowData);
                    }
                }
                univerSheetVO.setCellData(cellRes);
                univerSheetVO.setRowCount(lastRowNum + 1);

                // 读取列宽信息
                List<UniverSheetVO.ColumnData> columnDataList = new ArrayList<>();
                // 确定需要处理的列数 - 取所有行中最大列数
                int maxCols = 0;
                for (int rowIndex = 0; rowIndex <= lastRowNum; rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row != null) {
                        maxCols = Math.max(maxCols, row.getLastCellNum());
                    }
                }
                // 读取列宽信息
                for (int colIndex = 0; colIndex < maxCols; colIndex++) {
                    UniverSheetVO.ColumnData columnData = new UniverSheetVO.ColumnData();
                    int width = sheet.getColumnWidth(colIndex); // POI中列宽单位是1/256个字符宽度
                    // 转换POI的列宽单位（256ths of a character width）为像素单位（近似转换）
                    // 通常一个字符宽度约为7-8像素
                    int pixelWidth = Math.round(width / 256.0f * 7.0f); // 近似转换
                    columnData.setW(pixelWidth > 0 ? pixelWidth : 100); // 设置默认宽度为100像素
                    columnData.setHd(0); // 默认不隐藏
                    columnDataList.add(columnData);
                }
                univerSheetVO.setColumnData(columnDataList);

                // 读取合并单元格数据
                int numMergedRegions = sheet.getNumMergedRegions(); // 获取合并区域的数量
                List<UniverSheetVO.Range> mergeData = new ArrayList<>();
                for (int j = 0; j < numMergedRegions; j++) {
                    CellRangeAddress mergedRegion = sheet.getMergedRegion(j);

                    UniverSheetVO.Range range = new UniverSheetVO.Range();
                    range.setRangeType(0);
                    range.setStartRow(mergedRegion.getFirstRow());
                    range.setEndRow(mergedRegion.getLastRow()); // Univer中end是排他的
                    range.setStartColumn(mergedRegion.getFirstColumn());
                    range.setEndColumn(mergedRegion.getLastColumn()); // Univer中end是排他的

                    mergeData.add(range);
                }
                univerSheetVO.setMergeData(mergeData);

                result.put(univerSheetVO.getId(), univerSheetVO);
            }
            return R.ok(result);
        } catch (IOException ioException) {
            log.error("读取excel文件异常", ioException);
        }
        return R.fail("读取失败");
    }

    /**
     * 转换DDL语句，可指定{database}类型
     *
     * @param convertVO DDL语句信息
     * @return 结果
     */
    @Override
    public R<Object> convertDDL(ConvertVO convertVO) {
        if (ObjectUtil.equals(1, convertVO.getInputType()) && StrUtil.isEmpty(convertVO.getInputDDL())) {
            throw new ServiceException("DDL语句不能为空");
        }
        if (ObjectUtil.equals(2, convertVO.getInputType()) && CollUtil.isEmpty(convertVO.getTableVOList())) {
            throw new ServiceException("元数据列表不能为空");
        }
        List<TableVO> tableVOList = ObjectUtil.equals(1, convertVO.getInputType()) ?
                DDLUtil.parserDDL(convertVO) : convertVO.getTableVOList();
        if (convertVO.getOutputType().equals(2)) {
            List<TableVO> filterList = tableVOList.stream()
                    .filter(tableVO -> ObjectUtil.isNull(tableVO.getExtend()))
                    .collect(Collectors.toList());
            return R.ok(filterList);
        }
        Map<String, String[]> tableDDLMap = DDLUtil.genDDL(tableVOList, convertVO.getOutputDatabase());
        return R.ok(tableDDLMap);
    }
}
