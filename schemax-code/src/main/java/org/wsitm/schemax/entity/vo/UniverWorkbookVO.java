package org.wsitm.schemax.entity.vo;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Univer Workbook 对象（用于后端组装返回给前端的 workbookData）
 *
 * 说明：根据 Univer 的 workbookData 结构（sheets + sheetOrder + styles），
 * 这里将样式提升到 workbook 级别做统一维护，并由 cell.s 引用 styleId。
 *
 * @author wsitm
 * @date 2026-01-03
 */
public class UniverWorkbookVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** workbook id */
    private String id;
    /** workbook name */
    private String name;

    /** sheet id order */
    private List<String> sheetOrder;

    /** sheets map: sheetId -> sheet */
    private Map<String, UniverSheetVO> sheets;

    /** styles map: styleId -> style */
    private Map<String, UniverSheetVO.StyleData> styles;

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

    public List<String> getSheetOrder() {
        return sheetOrder;
    }

    public void setSheetOrder(List<String> sheetOrder) {
        this.sheetOrder = sheetOrder;
    }

    public Map<String, UniverSheetVO> getSheets() {
        return sheets;
    }

    public void setSheets(Map<String, UniverSheetVO> sheets) {
        this.sheets = sheets;
    }

    public Map<String, UniverSheetVO.StyleData> getStyles() {
        return styles;
    }

    public void setStyles(Map<String, UniverSheetVO.StyleData> styles) {
        this.styles = styles;
    }
}

