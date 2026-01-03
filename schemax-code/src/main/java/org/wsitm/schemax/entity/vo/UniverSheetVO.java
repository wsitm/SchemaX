package org.wsitm.schemax.entity.vo;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Univer Sheet 对象
 * 对应前端univer数据格式
 *
 * @author wsitm
 * @date 2025-02-01
 */
public class UniverSheetVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * sheet id
     */
    private String id;

    /**
     * sheet name
     */
    private String name;

    /**
     * 数据行数
     */
    private Integer rowCount;

    /**
     * 单元格数据
     * key: row index, value: column index -> cell
     */
    private Map<Integer, Map<Integer, SheetCell>> cellData;

    /**
     * 合并单元格数据
     */
    private List<Range> mergeData;

    /**
     * 列配置，控制列框和样式
     */
    private List<ColumnData> columnData;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getRowCount() {
        return rowCount;
    }

    public void setRowCount(Integer rowCount) {
        this.rowCount = rowCount;
    }

    public Map<Integer, Map<Integer, SheetCell>> getCellData() {
        return cellData;
    }

    public void setCellData(Map<Integer, Map<Integer, SheetCell>> cellData) {
        this.cellData = cellData;
    }

    public List<Range> getMergeData() {
        return mergeData;
    }

    public void setMergeData(List<Range> mergeData) {
        this.mergeData = mergeData;
    }

    public List<ColumnData> getColumnData() {
        return columnData;
    }

    public void setColumnData(List<ColumnData> columnData) {
        this.columnData = columnData;
    }

    public static class SheetCell implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 单元格的值
         */
        private String v;
        /**
         * 单元格样式
         *
         * 说明：为了与 Univer workbookData 的 styles 机制对齐，这里改为 styleId 引用。
         * 由后端在 workbook.styles 中统一维护样式，cell.s 仅存样式ID。
         */
        private String s;

        public String getV() {
            return v;
        }

        public void setV(String v) {
            this.v = v;
        }

        public String getS() {
            return s;
        }

        public void setS(String s) {
            this.s = s;
        }
    }

    public static class StyleData implements Serializable {
        /**
         * background
         */
        private ColorStyle bg;
        /**
         * text color (font color)
         */
        private ColorStyle cl;
        /**
         * font family
         */
        private String ff;
        /**
         * font size (pt)
         */
        private Integer fs;
        /**
         * bold 0: false 1: true
         */
        private Integer bl;
        /**
         * italic 0/1
         */
        private Integer it;
        /**
         * underline 0/1
         */
        private Integer ul;
        /**
         * strikethrough 0/1
         */
        private Integer st;
        /**
         * horizontal alignment (0 general, 1 left, 2 center, 3 right)
         */
        private Integer ht;
        /**
         * vertical alignment (0 top, 1 middle, 2 bottom)
         */
        private Integer vt;
        /**
         * wrap text 0/1
         */
        private Integer tb;
        /**
         * number format pattern (Excel-like)
         */
        private String n;

        /**
         * 上、下、左、右边框的样式属性
         */
        private BorderData bd;

        public ColorStyle getBg() {
            return bg;
        }

        public void setBg(ColorStyle bg) {
            this.bg = bg;
        }

        public ColorStyle getCl() {
            return cl;
        }

        public void setCl(ColorStyle cl) {
            this.cl = cl;
        }

        public String getFf() {
            return ff;
        }

        public void setFf(String ff) {
            this.ff = ff;
        }

        public Integer getFs() {
            return fs;
        }

        public void setFs(Integer fs) {
            this.fs = fs;
        }

        public Integer getBl() {
            return bl;
        }

        public void setBl(Integer bl) {
            this.bl = bl;
        }

        public Integer getIt() {
            return it;
        }

        public void setIt(Integer it) {
            this.it = it;
        }

        public Integer getUl() {
            return ul;
        }

        public void setUl(Integer ul) {
            this.ul = ul;
        }

        public Integer getSt() {
            return st;
        }

        public void setSt(Integer st) {
            this.st = st;
        }

        public Integer getHt() {
            return ht;
        }

        public void setHt(Integer ht) {
            this.ht = ht;
        }

        public Integer getVt() {
            return vt;
        }

        public void setVt(Integer vt) {
            this.vt = vt;
        }

        public Integer getTb() {
            return tb;
        }

        public void setTb(Integer tb) {
            this.tb = tb;
        }

        public String getN() {
            return n;
        }

        public void setN(String n) {
            this.n = n;
        }

        public BorderData getBd() {
            return bd;
        }

        public void setBd(BorderData bd) {
            this.bd = bd;
        }
    }

    public static class ColorStyle implements Serializable {
        // RGB color
        private String rgb;

        public String getRgb() {
            return rgb;
        }

        public void setRgb(String rgb) {
            this.rgb = rgb;
        }
    }

    public static class BorderData implements Serializable {

        /**
         * 上边框样式属性
         */
        private BorderStyleData t;
        /**
         * 右边框样式属性
         */
        private BorderStyleData r;
        /**
         * 下边框样式属性
         */
        private BorderStyleData b;
        /**
         * 左边框样式属性
         */
        private BorderStyleData l;
//        private BorderStyleData tl_br;
//        private BorderStyleData tl_bc;
//        private BorderStyleData tl_mr;
//        private BorderStyleData bl_tr;
//        private BorderStyleData ml_tr;
//        private BorderStyleData bc_tr;

        public BorderStyleData getT() {
            return t;
        }

        public void setT(BorderStyleData t) {
            this.t = t;
        }

        public BorderStyleData getR() {
            return r;
        }

        public void setR(BorderStyleData r) {
            this.r = r;
        }

        public BorderStyleData getB() {
            return b;
        }

        public void setB(BorderStyleData b) {
            this.b = b;
        }

        public BorderStyleData getL() {
            return l;
        }

        public void setL(BorderStyleData l) {
            this.l = l;
        }

//        public BorderStyleData getTl_br() {
//            return tl_br;
//        }
//
//        public void setTl_br(BorderStyleData tl_br) {
//            this.tl_br = tl_br;
//        }
//
//        public BorderStyleData getTl_bc() {
//            return tl_bc;
//        }
//
//        public void setTl_bc(BorderStyleData tl_bc) {
//            this.tl_bc = tl_bc;
//        }
//
//        public BorderStyleData getTl_mr() {
//            return tl_mr;
//        }
//
//        public void setTl_mr(BorderStyleData tl_mr) {
//            this.tl_mr = tl_mr;
//        }
//
//        public BorderStyleData getBl_tr() {
//            return bl_tr;
//        }
//
//        public void setBl_tr(BorderStyleData bl_tr) {
//            this.bl_tr = bl_tr;
//        }
//
//        public BorderStyleData getMl_tr() {
//            return ml_tr;
//        }
//
//        public void setMl_tr(BorderStyleData ml_tr) {
//            this.ml_tr = ml_tr;
//        }
//
//        public BorderStyleData getBc_tr() {
//            return bc_tr;
//        }
//
//        public void setBc_tr(BorderStyleData bc_tr) {
//            this.bc_tr = bc_tr;
//        }
    }

    public static class BorderStyleData implements Serializable {
        /**
         * NONE = 0,
         * THIN = 1,
         * HAIR = 2,
         * DOTTED = 3,
         * DASHED = 4,
         * DASH_DOT = 5,
         * DASH_DOT_DOT = 6,
         * DOUBLE = 7,
         * MEDIUM = 8,
         * MEDIUM_DASHED = 9,
         * MEDIUM_DASH_DOT = 10,
         * MEDIUM_DASH_DOT_DOT = 11,
         * SLANT_DASH_DOT = 12,
         * THICK = 13
         */
        private Integer s;
        /**
         * 颜色样式
         */
        private ColorStyle cl;

        public Integer getS() {
            return s;
        }

        public void setS(Integer s) {
            this.s = s;
        }

        public ColorStyle getCl() {
            return cl;
        }

        public void setCl(ColorStyle cl) {
            this.cl = cl;
        }
    }

    public static class Range implements Serializable {
        private static final long serialVersionUID = 1L;
        /**
         * NORMAL = 0,
         * ROW = 1,
         * COLUMN = 2,
         * ALL = 3
         */
        private Integer rangeType;
        /**
         * The start row (inclusive) of the range startRow
         */
        private Integer startRow;
        /**
         * The end row (exclusive) of the range endRow
         */
        private Integer endRow;
        /**
         * The start column (inclusive) of the range startColumn
         */
        private Integer startColumn;
        /**
         * The end column (exclusive) of the range endColumn
         */
        private Integer endColumn;

        public Integer getRangeType() {
            return rangeType;
        }

        public void setRangeType(Integer rangeType) {
            this.rangeType = rangeType;
        }

        public Integer getStartRow() {
            return startRow;
        }

        public void setStartRow(Integer startRow) {
            this.startRow = startRow;
        }

        public Integer getEndRow() {
            return endRow;
        }

        public void setEndRow(Integer endRow) {
            this.endRow = endRow;
        }

        public Integer getStartColumn() {
            return startColumn;
        }

        public void setStartColumn(Integer startColumn) {
            this.startColumn = startColumn;
        }

        public Integer getEndColumn() {
            return endColumn;
        }

        public void setEndColumn(Integer endColumn) {
            this.endColumn = endColumn;
        }
    }

    public static class ColumnData implements Serializable {
        /**
         * 列宽
         */
        private Integer w;
        /**
         * 是否隐藏，1、是，0、否
         */
        private Integer hd;
        /**
         * 列样式
         */
        private StyleData s;

        public Integer getW() {
            return w;
        }

        public void setW(Integer w) {
            this.w = w;
        }

        public Integer getHd() {
            return hd;
        }

        public void setHd(Integer hd) {
            this.hd = hd;
        }

        public StyleData getS() {
            return s;
        }

        public void setS(StyleData s) {
            this.s = s;
        }
    }

}
