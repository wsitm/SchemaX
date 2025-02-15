package org.wsitm.rdbms.utils;

import cn.hutool.core.util.ObjectUtil;
import org.wsitm.rdbms.entity.vo.UniverSheetVO;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.IndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class PoiUtil {

    /**
     * 将颜色索引转换为 RGB 格式的字符串
     */
    public static String getColorRGB(short colorIndex, Workbook workbook) {
        if (workbook instanceof XSSFWorkbook) {
            // 处理 .xlsx 文件
            IndexedColorMap indexedColorMap = ((XSSFWorkbook) workbook).getStylesSource().getIndexedColors();
            byte[] rgb = indexedColorMap.getRGB(colorIndex);
            if (rgb != null) {
                return String.format("rgb(%d,%d,%d)", rgb[0] & 0xFF, rgb[1] & 0xFF, rgb[2] & 0xFF);
            }

        } else if (workbook instanceof HSSFWorkbook) {
            // 处理 .xls 文件
            HSSFPalette palette = ((HSSFWorkbook) workbook).getCustomPalette();
            HSSFColor color = palette.getColor(colorIndex);
            if (color != null) {
                short[] rgb = color.getTriplet(); // 获取 RGB 值
                return String.format("rgb(%d,%d,%d)", rgb[0], rgb[1], rgb[2]);
            }
        }
        return null;
    }

    /**
     * 配置并获取 边框样式 数据
     *
     * @param borderStyle 边框样式
     * @param borderColor 边框颜色
     * @param workbook    工作簿
     * @return 边框样式 数据
     */
    public static UniverSheetVO.BorderStyleData getBorderStyleData(BorderStyle borderStyle, short borderColor, Workbook workbook) {
        UniverSheetVO.BorderStyleData topBorderStyle = new UniverSheetVO.BorderStyleData();
        topBorderStyle.setS((int) borderStyle.getCode());
        UniverSheetVO.ColorStyle borderTopColorStyle = new UniverSheetVO.ColorStyle();
        borderTopColorStyle.setRgb(PoiUtil.getColorRGB(borderColor, workbook));
        topBorderStyle.setCl(borderTopColorStyle);
        return topBorderStyle;
    }

    /**
     * 获取单元格的值
     *
     * @param cell 单元格
     * @return 值
     */
    public static Object getCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        Object val = null;
        try {
            if (ObjectUtil.isNotNull(cell)) {
                if (cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.FORMULA) {
                    val = cell.getNumericCellValue();
                    if (DateUtil.isCellDateFormatted(cell)) {
                        val = DateUtil.getJavaDate((Double) val); // POI Excel 日期格式转换
                    } else {
                        if ((Double) val % 1 != 0) {
                            val = new BigDecimal(val.toString());
                        } else {
                            val = new DecimalFormat("0").format(val);
                        }
                    }
                } else if (cell.getCellType() == CellType.STRING) {
                    val = cell.getStringCellValue();
                } else if (cell.getCellType() == CellType.BOOLEAN) {
                    val = cell.getBooleanCellValue();
                } else if (cell.getCellType() == CellType.ERROR) {
                    val = cell.getErrorCellValue();
                }

            }
        } catch (Exception e) {
            return val;
        }
        return val;
    }

    /**
     * 创建默认普通单元格样式
     *
     * <pre>
     * 1. 文字上下左右居中
     * 2. 细边框，黑色
     * </pre>
     *
     * @param workbook {@link Workbook} 工作簿
     * @return {@link CellStyle}
     */
    public static CellStyle createDefaultCellStyle(Workbook workbook) {
        final CellStyle cellStyle = createCellStyle(workbook);
        setAlign(cellStyle, HorizontalAlignment.CENTER, VerticalAlignment.CENTER);
        setBorder(cellStyle, BorderStyle.THIN, IndexedColors.BLACK);
        return cellStyle;
    }


    /**
     * 创建单元格样式
     *
     * @param workbook {@link Workbook} 工作簿
     * @return {@link CellStyle}
     * @see Workbook#createCellStyle()
     * @since 5.4.0
     */
    public static CellStyle createCellStyle(Workbook workbook) {
        if (null == workbook) {
            return null;
        }
        return workbook.createCellStyle();
    }

    /**
     * 设置cell文本对齐样式
     *
     * @param cellStyle {@link CellStyle}
     * @param halign    横向位置
     * @param valign    纵向位置
     * @return {@link CellStyle}
     */
    public static CellStyle setAlign(CellStyle cellStyle, HorizontalAlignment halign, VerticalAlignment valign) {
        cellStyle.setAlignment(halign);
        cellStyle.setVerticalAlignment(valign);
        return cellStyle;
    }

    /**
     * 设置cell的四个边框粗细和颜色
     *
     * @param cellStyle  {@link CellStyle}
     * @param borderSize 边框粗细{@link BorderStyle}枚举
     * @param colorIndex 颜色的short值
     * @return {@link CellStyle}
     */
    public static CellStyle setBorder(CellStyle cellStyle, BorderStyle borderSize, IndexedColors colorIndex) {
        cellStyle.setBorderBottom(borderSize);
        cellStyle.setBottomBorderColor(colorIndex.index);

        cellStyle.setBorderLeft(borderSize);
        cellStyle.setLeftBorderColor(colorIndex.index);

        cellStyle.setBorderRight(borderSize);
        cellStyle.setRightBorderColor(colorIndex.index);

        cellStyle.setBorderTop(borderSize);
        cellStyle.setTopBorderColor(colorIndex.index);

        return cellStyle;
    }
}
