package org.wsitm.rdbms.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import org.wsitm.rdbms.entity.core.R;
import org.wsitm.rdbms.entity.vo.ConvertVO;
import org.wsitm.rdbms.entity.vo.TableVO;
import org.wsitm.rdbms.entity.vo.UniverSheetVO;
import org.wsitm.rdbms.exception.ServiceException;
import org.wsitm.rdbms.service.IConvertService;
import org.wsitm.rdbms.utils.DDLUtil;
import org.wsitm.rdbms.utils.PoiUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
                for (int j = 0; j <= sheet.getLastRowNum(); j++) {
                    Row row = sheet.getRow(j);
                    Map<Integer, UniverSheetVO.SheetCell> rowData = new LinkedHashMap<>();
                    for (int k = 0; k < row.getLastCellNum(); k++) {
                        Cell cell = row.getCell(k);
                        Object value = PoiUtil.getCellValue(cell);
//                        if (ObjectUtil.isEmpty(value)) {
//                            continue;
//                        }
                        CellStyle cellStyle = cell.getCellStyle();

                        UniverSheetVO.SheetCell cellData = new UniverSheetVO.SheetCell();
                        cellData.setV(ObjectUtil.isNotEmpty(value) ? StrUtil.toString(value) : null);

                        UniverSheetVO.StyleData styleData = new UniverSheetVO.StyleData();
                        // 设置背景
                        UniverSheetVO.ColorStyle colorStyle = new UniverSheetVO.ColorStyle();
                        colorStyle.setRgb(PoiUtil.getColorRGB(cellStyle.getFillBackgroundColor(), wb));
                        styleData.setBg(colorStyle);

                        // 设置字体是否加粗
                        Font font = wb.getFontAt(cellStyle.getFontIndexAsInt()); // 获取字体
                        styleData.setBl(font.getBold() ? 1 : 0);

                        // 设置单元格样式
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
                univerSheetVO.setRowCount(sheet.getLastRowNum() + 1);

                // 读取合并单元格数据
                int numMergedRegions = sheet.getNumMergedRegions(); // 获取合并区域的数量
                List<UniverSheetVO.Range> mergeData = new ArrayList<>();
                for (int j = 0; j < numMergedRegions; j++) {
                    CellRangeAddress mergedRegion = sheet.getMergedRegion(j);

                    UniverSheetVO.Range range = new UniverSheetVO.Range();
                    range.setRangeType(0);
                    range.setStartRow(mergedRegion.getFirstRow());
                    range.setEndRow(mergedRegion.getLastRow());
                    range.setStartColumn(mergedRegion.getFirstColumn());
                    range.setEndColumn(mergedRegion.getLastColumn());

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
