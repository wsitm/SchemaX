package org.wsitm.rdbms.entity.vo;

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

    private String id;

    private String name;

    private Integer rowCount;

    private Map<Integer, Map<Integer, SheetCell>> cellData;

    private List<Range> mergeData;

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

    public static class SheetCell {
        private String v;
        private StyleData s;

        public String getV() {
            return v;
        }

        public void setV(String v) {
            this.v = v;
        }

        public StyleData getS() {
            return s;
        }

        public void setS(StyleData s) {
            this.s = s;
        }
    }

    public static class StyleData {
        /**
         * background
         */
        private ColorStyle bg;
        /**
         * bold 0: false 1: true
         */
        private Integer bl;

        /**
         * Style properties of top, bottom, left and right border
         */
        private BorderData bd;

        public ColorStyle getBg() {
            return bg;
        }

        public void setBg(ColorStyle bg) {
            this.bg = bg;
        }

        public Integer getBl() {
            return bl;
        }

        public void setBl(Integer bl) {
            this.bl = bl;
        }

        public BorderData getBd() {
            return bd;
        }

        public void setBd(BorderData bd) {
            this.bd = bd;
        }
    }

    public static class ColorStyle {
        // RGB color
        private String rgb;

        public String getRgb() {
            return rgb;
        }

        public void setRgb(String rgb) {
            this.rgb = rgb;
        }
    }

    // Style properties of top, bottom, left and right border
    public static class BorderData {

        private BorderStyleData t;
        private BorderStyleData r;
        private BorderStyleData b;
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

    public static class BorderStyleData {
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

    public static class Range {
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
}
