package org.wsitm.schemax.service.template.word;

import cn.hutool.core.util.StrUtil;

import org.apache.poi.xwpf.model.XWPFHeaderFooterPolicy;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.IBody;
import org.apache.poi.xwpf.usermodel.LineSpacingRule;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TableRowAlign;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.VerticalAlign;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFHeaderFooter;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;
import org.springframework.stereotype.Component;
import org.wsitm.schemax.service.template.word.WordDocumentModel.HeaderFooterBody;
import org.wsitm.schemax.service.template.word.WordDocumentModel.ParagraphBlock;
import org.wsitm.schemax.service.template.word.WordDocumentModel.TableBlock;
import org.wsitm.schemax.service.template.word.WordDocumentModel.TableCell;
import org.wsitm.schemax.service.template.word.WordDocumentModel.TableRow;
import org.wsitm.schemax.service.template.word.WordDocumentModel.TextRun;
import org.wsitm.schemax.service.template.word.WordDocumentModel.WordBlock;
import org.wsitm.schemax.service.template.word.WordDocumentModel.WordDocument;
import org.wsitm.schemax.utils.json.JSONArray;
import org.wsitm.schemax.utils.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 Apache POI XWPF 的第一期 Word 导出实现。
 */
@Component
public class XwpfWordDocumentExporter implements WordDocumentExporter {

    private static final int TWIPS_PER_PIXEL = 15;

    @Override
    public void export(File target, WordDocument document) throws IOException {
        try (XWPFDocument xwpfDocument = new XWPFDocument();
             FileOutputStream output = new FileOutputStream(target)) {
            JSONObject documentStyle = document.getDocumentStyle();
            applyDocumentStyle(xwpfDocument, documentStyle);
            JSONObject defaultTextStyle = documentStyle == null
                    ? new JSONObject() : copy(documentStyle.getJSONObject("textStyle"));
            for (WordBlock block : document.getBlocks()) {
                if (block instanceof ParagraphBlock paragraph) {
                    writeParagraph(xwpfDocument.createParagraph(), paragraph, defaultTextStyle);
                } else if (block instanceof TableBlock table) {
                    writeTable(xwpfDocument, table, defaultTextStyle);
                }
            }
            writeHeadersAndFooters(xwpfDocument, document, defaultTextStyle);
            xwpfDocument.write(output);
        }
    }

    private void applyDocumentStyle(XWPFDocument document, JSONObject style) {
        if (style == null) {
            return;
        }
        CTBody body = document.getDocument().getBody();
        CTSectPr section = body.isSetSectPr() ? body.getSectPr() : body.addNewSectPr();
        JSONObject pageSize = style.getJSONObject("pageSize");
        if (pageSize != null) {
            CTPageSz size = section.isSetPgSz() ? section.getPgSz() : section.addNewPgSz();
            Double width = pageSize.getDouble("width");
            Double height = pageSize.getDouble("height");
            if (width != null && width > 0) {
                size.setW(BigInteger.valueOf(pxToTwips(width)));
            }
            if (height != null && height > 0) {
                size.setH(BigInteger.valueOf(pxToTwips(height)));
            }
        }

        CTPageMar margin = section.isSetPgMar() ? section.getPgMar() : section.addNewPgMar();
        setMargin(style, "marginTop", margin::setTop);
        setMargin(style, "marginBottom", margin::setBottom);
        setMargin(style, "marginLeft", margin::setLeft);
        setMargin(style, "marginRight", margin::setRight);
        setMargin(style, "marginHeader", margin::setHeader);
        setMargin(style, "marginFooter", margin::setFooter);

        if (style.getIntValue("evenAndOddHeaders") == 1) {
            document.setEvenAndOddHeadings(true);
        }
        if (style.getIntValue("useFirstPageHeaderFooter") == 1 && !section.isSetTitlePg()) {
            section.addNewTitlePg();
        }
    }

    private void writeHeadersAndFooters(XWPFDocument document,
                                        WordDocument source,
                                        JSONObject defaultTextStyle) {
        JSONObject style = source.getDocumentStyle();
        if (style == null) {
            return;
        }
        XWPFHeaderFooterPolicy policy = document.createHeaderFooterPolicy();
        writeHeader(policy, source.getHeaders().get(style.getString("defaultHeaderId")),
                XWPFHeaderFooterPolicy.DEFAULT, defaultTextStyle);
        writeHeader(policy, source.getHeaders().get(style.getString("firstPageHeaderId")),
                XWPFHeaderFooterPolicy.FIRST, defaultTextStyle);
        writeHeader(policy, source.getHeaders().get(style.getString("evenPageHeaderId")),
                XWPFHeaderFooterPolicy.EVEN, defaultTextStyle);
        writeFooter(policy, source.getFooters().get(style.getString("defaultFooterId")),
                XWPFHeaderFooterPolicy.DEFAULT, defaultTextStyle);
        writeFooter(policy, source.getFooters().get(style.getString("firstPageFooterId")),
                XWPFHeaderFooterPolicy.FIRST, defaultTextStyle);
        writeFooter(policy, source.getFooters().get(style.getString("evenPageFooterId")),
                XWPFHeaderFooterPolicy.EVEN, defaultTextStyle);
    }

    private void writeHeader(XWPFHeaderFooterPolicy policy,
                             HeaderFooterBody source,
                             org.openxmlformats.schemas.wordprocessingml.x2006.main.STHdrFtr.Enum type,
                             JSONObject defaultTextStyle) {
        if (source == null) {
            return;
        }
        XWPFHeader header = policy.createHeader(type);
        writeHeaderFooterBody(header, source, defaultTextStyle);
    }

    private void writeFooter(XWPFHeaderFooterPolicy policy,
                             HeaderFooterBody source,
                             org.openxmlformats.schemas.wordprocessingml.x2006.main.STHdrFtr.Enum type,
                             JSONObject defaultTextStyle) {
        if (source == null) {
            return;
        }
        XWPFFooter footer = policy.createFooter(type);
        writeHeaderFooterBody(footer, source, defaultTextStyle);
    }

    private void writeHeaderFooterBody(XWPFHeaderFooter target,
                                       HeaderFooterBody source,
                                       JSONObject defaultTextStyle) {
        List<XWPFParagraph> existing = target.getParagraphs();
        int paragraphIndex = 0;
        for (WordBlock block : source.getBlocks()) {
            if (!(block instanceof ParagraphBlock paragraph)) {
                continue;
            }
            XWPFParagraph targetParagraph = paragraphIndex < existing.size()
                    ? existing.get(paragraphIndex) : target.createParagraph();
            clearParagraph(targetParagraph);
            writeParagraph(targetParagraph, paragraph, defaultTextStyle);
            paragraphIndex++;
        }
    }

    private void writeTable(XWPFDocument document, TableBlock source, JSONObject defaultTextStyle) {
        XWPFTable table = document.createTable();
        while (table.getNumberOfRows() > 0) {
            table.removeRow(0);
        }
        applyTableStyle(table, source.getStyle());

        Map<Integer, Integer> verticalMerges = new HashMap<>();
        for (TableRow sourceRow : source.getRows()) {
            XWPFTableRow row = table.createRow();
            while (!row.getTableCells().isEmpty()) {
                row.removeCell(0);
            }
            applyRowStyle(row, sourceRow.getStyle());
            int logicalColumn = 0;
            List<TableCell> sourceCells = sourceRow.getCells();
            for (int cellIndex = 0; cellIndex < sourceCells.size(); cellIndex++) {
                TableCell sourceCell = sourceCells.get(cellIndex);
                JSONObject cellStyle = sourceCell.getStyle();
                int rowSpan = cellStyle.getIntValue("rowSpan", 1);
                int columnSpan = cellStyle.getIntValue("columnSpan", 1);
                boolean covered = rowSpan == 0 || columnSpan == 0;
                if (covered && verticalMerges.getOrDefault(logicalColumn, 0) <= 0) {
                    logicalColumn++;
                    continue;
                }

                XWPFTableCell cell = row.createCell();
                if (covered) {
                    if (columnSpan > 1) {
                        applyColumnSpan(cell, columnSpan);
                    }
                    applyVerticalMerge(cell, false);
                    verticalMerges.computeIfPresent(logicalColumn, (key, remaining) -> remaining - 1);
                    logicalColumn += Math.max(1, columnSpan);
                    continue;
                }

                applyCellStyle(cell, cellStyle, source.getStyle(), logicalColumn);
                if (columnSpan > 1) {
                    applyColumnSpan(cell, columnSpan);
                }
                if (rowSpan > 1) {
                    applyVerticalMerge(cell, true);
                    verticalMerges.put(logicalColumn, rowSpan - 1);
                }
                writeCellBody(cell, sourceCell, defaultTextStyle);
                logicalColumn += Math.max(1, columnSpan);
            }
        }
    }

    private void applyTableStyle(XWPFTable table, JSONObject style) {
        table.setWidth("100%");
        int alignment = style == null ? 0 : style.getIntValue("align");
        if (alignment == 1) {
            table.setTableAlignment(TableRowAlign.CENTER);
        } else if (alignment == 2) {
            table.setTableAlignment(TableRowAlign.RIGHT);
        } else {
            table.setTableAlignment(TableRowAlign.LEFT);
        }
        table.setInsideHBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "B7BDC7");
        table.setInsideVBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "B7BDC7");
        table.setTopBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "B7BDC7");
        table.setBottomBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "B7BDC7");
        table.setLeftBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "B7BDC7");
        table.setRightBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "B7BDC7");

        JSONObject margin = style == null ? null : style.getJSONObject("cellMargin");
        if (margin != null) {
            table.setCellMargins(
                    numberUnitToTwips(margin.getJSONObject("top")),
                    numberUnitToTwips(margin.getJSONObject("start")),
                    numberUnitToTwips(margin.getJSONObject("bottom")),
                    numberUnitToTwips(margin.getJSONObject("end"))
            );
        }
    }

    private void applyRowStyle(XWPFTableRow row, JSONObject style) {
        if (style == null) {
            return;
        }
        JSONObject height = style.getJSONObject("trHeight");
        JSONObject value = height == null ? null : height.getJSONObject("val");
        if (value != null && value.getDouble("v") != null) {
            row.setHeight(pxToTwips(value.getDoubleValue("v")));
            int heightRule = height.getIntValue("hRule");
            row.setHeightRule(heightRule == 2
                    ? org.apache.poi.xwpf.usermodel.TableRowHeightRule.EXACT
                    : heightRule == 1
                    ? org.apache.poi.xwpf.usermodel.TableRowHeightRule.AT_LEAST
                    : org.apache.poi.xwpf.usermodel.TableRowHeightRule.AUTO);
        }
        row.setCantSplitRow(style.getIntValue("cantSplit") == 1);
        row.setRepeatHeader(style.getIntValue("repeatHeaderRow") == 1);
    }

    private void applyCellStyle(XWPFTableCell cell,
                                JSONObject style,
                                JSONObject tableStyle,
                                int logicalColumn) {
        JSONObject background = style.getJSONObject("backgroundColor");
        String backgroundColor = color(background);
        if (backgroundColor != null) {
            cell.setColor(backgroundColor);
        }
        int verticalAlignment = style.getIntValue("vAlign");
        if (verticalAlignment == 3) {
            cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
        } else if (verticalAlignment == 4) {
            cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.BOTTOM);
        } else {
            cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.TOP);
        }

        JSONArray columns = tableStyle == null ? null : tableStyle.getJSONArray("tableColumns");
        if (columns != null && logicalColumn < columns.size()) {
            JSONObject column = columns.getJSONObject(logicalColumn);
            JSONObject size = column == null ? null : column.getJSONObject("size");
            JSONObject width = size == null ? null : size.getJSONObject("width");
            if (width != null && width.getDouble("v") != null) {
                cell.setWidth(String.valueOf(pxToTwips(width.getDoubleValue("v"))));
            }
        }
    }

    private void applyColumnSpan(XWPFTableCell cell, int span) {
        CTTcPr properties = getCellProperties(cell);
        if (properties.isSetGridSpan()) {
            properties.getGridSpan().setVal(BigInteger.valueOf(span));
        } else {
            properties.addNewGridSpan().setVal(BigInteger.valueOf(span));
        }
    }

    private void applyVerticalMerge(XWPFTableCell cell, boolean restart) {
        CTTcPr properties = getCellProperties(cell);
        if (properties.isSetVMerge()) {
            properties.getVMerge().setVal(restart ? STMerge.RESTART : STMerge.CONTINUE);
        } else {
            properties.addNewVMerge().setVal(restart ? STMerge.RESTART : STMerge.CONTINUE);
        }
    }

    private CTTcPr getCellProperties(XWPFTableCell cell) {
        return cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
    }

    private void writeCellBody(XWPFTableCell cell, TableCell source, JSONObject defaultTextStyle) {
        List<XWPFParagraph> paragraphs = cell.getParagraphs();
        for (int index = 0; index < source.getParagraphs().size(); index++) {
            XWPFParagraph target = index < paragraphs.size() ? paragraphs.get(index) : cell.addParagraph();
            clearParagraph(target);
            writeParagraph(target, source.getParagraphs().get(index), defaultTextStyle);
        }
    }

    private void writeParagraph(XWPFParagraph target,
                                ParagraphBlock source,
                                JSONObject defaultTextStyle) {
        applyParagraphStyle(target, source.getStyle());
        JSONObject paragraphTextStyle = source.getStyle().getJSONObject("textStyle");
        if (source.getBullet() != null && !source.getBullet().isEmpty()) {
            int level = source.getBullet().getIntValue("level");
            target.setIndentationLeft((level + 1) * 360);
            XWPFRun bulletRun = target.createRun();
            applyTextStyle(bulletRun, mergeStyles(defaultTextStyle, paragraphTextStyle));
            String prefix = "ordered".equals(source.getBullet().getString("listType"))
                    ? source.getBullet().getIntValue("order", 1) + ". " : "• ";
            bulletRun.setText(prefix);
        }
        for (TextRun sourceRun : source.getRuns()) {
            XWPFRun run = target.createRun();
            applyTextStyle(run, mergeStyles(defaultTextStyle, paragraphTextStyle, sourceRun.getStyle()));
            writeRunText(run, sourceRun.getText());
        }
    }

    private void applyParagraphStyle(XWPFParagraph paragraph, JSONObject style) {
        if (style == null) {
            return;
        }
        int horizontalAlignment = style.getIntValue("horizontalAlign");
        paragraph.setAlignment(switch (horizontalAlignment) {
            case 2 -> ParagraphAlignment.CENTER;
            case 3 -> ParagraphAlignment.RIGHT;
            case 4, 5 -> ParagraphAlignment.BOTH;
            case 6 -> ParagraphAlignment.DISTRIBUTE;
            default -> ParagraphAlignment.LEFT;
        });
        Double lineSpacing = style.getDouble("lineSpacing");
        if (lineSpacing != null && lineSpacing > 0) {
            paragraph.setSpacingBetween(lineSpacing, LineSpacingRule.AUTO);
        }
        paragraph.setSpacingBefore(numberUnitToTwips(style.getJSONObject("spaceAbove")));
        paragraph.setSpacingAfter(numberUnitToTwips(style.getJSONObject("spaceBelow")));
        paragraph.setIndentationLeft(numberUnitToTwips(style.getJSONObject("indentStart")));
        paragraph.setIndentationRight(numberUnitToTwips(style.getJSONObject("indentEnd")));
        paragraph.setIndentationFirstLine(numberUnitToTwips(style.getJSONObject("indentFirstLine")));
        paragraph.setIndentationHanging(numberUnitToTwips(style.getJSONObject("hanging")));
        paragraph.setKeepNext(style.getIntValue("keepNext") == 1);

        int namedStyle = style.getIntValue("namedStyleType");
        if (namedStyle == 2) {
            paragraph.setStyle("Title");
        } else if (namedStyle >= 4 && namedStyle <= 9) {
            paragraph.setStyle("Heading" + (namedStyle - 3));
        }
    }

    private void applyTextStyle(XWPFRun run, JSONObject style) {
        if (style == null) {
            return;
        }
        String fontFamily = style.getString("ff");
        if (StrUtil.isNotBlank(fontFamily)) {
            run.setFontFamily(fontFamily);
        }
        Double fontSize = style.getDouble("fs");
        if (fontSize != null && fontSize > 0) {
            run.setFontSize(fontSize);
        }
        run.setBold(style.getIntValue("bl") == 1);
        run.setItalic(style.getIntValue("it") == 1);
        run.setStrikeThrough(decorationEnabled(style.getJSONObject("st")));
        if (decorationEnabled(style.getJSONObject("ul"))) {
            run.setUnderline(UnderlinePatterns.SINGLE);
        }
        String color = color(style.getJSONObject("cl"));
        if (color != null) {
            run.setColor(color);
        }
        int verticalAlign = style.getIntValue("va");
        if (verticalAlign == 2) {
            run.setSubscript(VerticalAlign.SUBSCRIPT);
        } else if (verticalAlign == 3) {
            run.setSubscript(VerticalAlign.SUPERSCRIPT);
        }
    }

    private void writeRunText(XWPFRun run, String text) {
        StringBuilder normalText = new StringBuilder();
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (character == WordTemplateService.PAGE_BREAK
                    || character == WordTemplateService.LINE_BREAK || character == '\t') {
                flushText(run, normalText);
                if (character == WordTemplateService.PAGE_BREAK) {
                    run.addBreak(BreakType.PAGE);
                } else if (character == WordTemplateService.LINE_BREAK) {
                    run.addBreak();
                } else {
                    run.addTab();
                }
            } else {
                normalText.append(character);
            }
        }
        flushText(run, normalText);
    }

    private void flushText(XWPFRun run, StringBuilder text) {
        if (!text.isEmpty()) {
            run.setText(text.toString());
            text.setLength(0);
        }
    }

    private void clearParagraph(XWPFParagraph paragraph) {
        for (int index = paragraph.getRuns().size() - 1; index >= 0; index--) {
            paragraph.removeRun(index);
        }
    }

    private JSONObject mergeStyles(JSONObject... styles) {
        JSONObject merged = new JSONObject();
        for (JSONObject style : styles) {
            if (style != null) {
                merged.putAll(style);
            }
        }
        return merged;
    }

    private boolean decorationEnabled(JSONObject decoration) {
        return decoration != null && decoration.getIntValue("s") == 1;
    }

    private String color(JSONObject color) {
        if (color == null) {
            return null;
        }
        String rgb = color.getString("rgb");
        if (rgb == null) {
            return null;
        }
        String normalized = rgb.trim();
        if (normalized.matches("#[0-9a-fA-F]{6}")) {
            return normalized.substring(1).toUpperCase();
        }
        return null;
    }

    private int numberUnitToTwips(JSONObject value) {
        return value == null || value.getDouble("v") == null ? 0 : pxToTwips(value.getDoubleValue("v"));
    }

    private int pxToTwips(double value) {
        return (int) Math.round(value * TWIPS_PER_PIXEL);
    }

    private void setMargin(JSONObject source, String key, BigIntegerConsumer setter) {
        Double value = source.getDouble(key);
        if (value != null && value >= 0) {
            setter.accept(BigInteger.valueOf(pxToTwips(value)));
        }
    }

    private JSONObject copy(JSONObject source) {
        return source == null ? new JSONObject() : org.wsitm.schemax.utils.JsonUtil.parseObject(source.toJSONString());
    }

    @FunctionalInterface
    private interface BigIntegerConsumer {
        void accept(BigInteger value);
    }
}
