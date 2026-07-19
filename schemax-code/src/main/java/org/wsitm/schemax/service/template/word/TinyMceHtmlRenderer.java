package org.wsitm.schemax.service.template.word;

import cn.hutool.core.util.StrUtil;
import org.wsitm.schemax.service.template.word.WordDocumentModel.ParagraphBlock;
import org.wsitm.schemax.service.template.word.WordDocumentModel.TableBlock;
import org.wsitm.schemax.service.template.word.WordDocumentModel.TableCell;
import org.wsitm.schemax.service.template.word.WordDocumentModel.TableRow;
import org.wsitm.schemax.service.template.word.WordDocumentModel.TextRun;
import org.wsitm.schemax.service.template.word.WordDocumentModel.WordBlock;
import org.wsitm.schemax.utils.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 将SchemaX Word文档块渲染为受控的TinyMCE HTML。
 */
final class TinyMceHtmlRenderer {

    String render(List<WordBlock> blocks) {
        StringBuilder html = new StringBuilder();
        String openList = null;
        for (WordBlock block : blocks) {
            if (block instanceof ParagraphBlock paragraph
                    && paragraph.getBullet() != null
                    && !paragraph.getBullet().isEmpty()) {
                String listTag = "ordered".equals(paragraph.getBullet().getString("listType")) ? "ol" : "ul";
                if (!Objects.equals(openList, listTag)) {
                    if (openList != null) {
                        html.append("</").append(openList).append('>');
                    }
                    html.append('<').append(listTag).append('>');
                    openList = listTag;
                }
                html.append("<li").append(paragraphStyleAttribute(paragraph.getStyle())).append('>');
                appendRuns(html, paragraph.getRuns());
                html.append("</li>");
                continue;
            }
            if (openList != null) {
                html.append("</").append(openList).append('>');
                openList = null;
            }
            if (block instanceof ParagraphBlock paragraph) {
                appendParagraph(html, paragraph);
            } else if (block instanceof TableBlock table) {
                appendTable(html, table);
            }
        }
        if (openList != null) {
            html.append("</").append(openList).append('>');
        }
        return html.toString();
    }

    private void appendParagraph(StringBuilder html, ParagraphBlock paragraph) {
        if (paragraph.plainText().indexOf(WordDocumentModel.PAGE_BREAK) >= 0) {
            html.append("<div class=\"schemax-page-break mceNonEditable\" "
                    + "data-schemax-block=\"page-break\" contenteditable=\"false\"></div>");
            return;
        }
        int namedStyle = paragraph.getStyle().getIntValue("namedStyleType");
        String tag = namedStyle >= 4 && namedStyle <= 9 ? "h" + (namedStyle - 3) : "p";
        html.append('<').append(tag)
                .append(paragraphStyleAttribute(paragraph.getStyle())).append('>');
        appendRuns(html, paragraph.getRuns());
        html.append("</").append(tag).append('>');
    }

    private void appendTable(StringBuilder html, TableBlock table) {
        html.append("<table style=\"width: 100%; border-collapse: collapse\"><tbody>");
        for (TableRow row : table.getRows()) {
            html.append("<tr>");
            for (TableCell cell : row.getCells()) {
                int rowSpan = cell.getStyle().getIntValue("rowSpan", 1);
                int columnSpan = cell.getStyle().getIntValue("columnSpan", 1);
                if (rowSpan == 0 || columnSpan == 0) {
                    continue;
                }
                html.append("<td");
                if (rowSpan > 1) {
                    html.append(" rowspan=\"").append(rowSpan).append('\"');
                }
                if (columnSpan > 1) {
                    html.append(" colspan=\"").append(columnSpan).append('\"');
                }
                html.append(" style=\"").append(escapeAttribute(cellCss(cell.getStyle()))).append("\">");
                for (ParagraphBlock paragraph : cell.getParagraphs()) {
                    appendParagraph(html, paragraph);
                }
                html.append("</td>");
            }
            html.append("</tr>");
        }
        html.append("</tbody></table>");
    }

    private void appendRuns(StringBuilder html, List<TextRun> runs) {
        for (TextRun run : runs) {
            String css = textCss(run.getStyle());
            StringBuilder body = new StringBuilder();
            appendEscapedText(body, run.getText());
            if (css.isEmpty()) {
                html.append(body);
            } else {
                html.append("<span style=\"").append(escapeAttribute(css)).append("\">")
                        .append(body).append("</span>");
            }
        }
    }

    private void appendEscapedText(StringBuilder html, String text) {
        int cursor = 0;
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == WordDocumentModel.LINE_BREAK) {
                html.append(escapeText(text.substring(cursor, index))).append("<br>");
                cursor = index + 1;
            }
        }
        html.append(escapeText(text.substring(cursor)));
    }

    private String paragraphStyleAttribute(JSONObject style) {
        List<String> css = new ArrayList<>();
        css.add("text-align: " + switch (style.getIntValue("horizontalAlign")) {
            case 2 -> "center";
            case 3 -> "right";
            case 4, 5, 6 -> "justify";
            default -> "left";
        });
        addCssUnit(css, "margin-top", style.getJSONObject("spaceAbove"));
        addCssUnit(css, "margin-bottom", style.getJSONObject("spaceBelow"));
        addCssUnit(css, "margin-left", style.getJSONObject("indentStart"));
        addCssUnit(css, "margin-right", style.getJSONObject("indentEnd"));
        return " style=\"" + escapeAttribute(String.join("; ", css)) + "\"";
    }

    private String textCss(JSONObject style) {
        List<String> css = new ArrayList<>();
        if (StrUtil.isNotBlank(style.getString("ff"))) {
            css.add("font-family: " + style.getString("ff"));
        }
        if (style.getDouble("fs") != null) {
            css.add("font-size: " + style.getDouble("fs") + "pt");
        }
        if (style.getIntValue("bl") == 1) {
            css.add("font-weight: bold");
        }
        if (style.getIntValue("it") == 1) {
            css.add("font-style: italic");
        }
        List<String> decorations = new ArrayList<>();
        if (enabled(style.getJSONObject("ul"))) {
            decorations.add("underline");
        }
        if (enabled(style.getJSONObject("st"))) {
            decorations.add("line-through");
        }
        if (!decorations.isEmpty()) {
            css.add("text-decoration: " + String.join(" ", decorations));
        }
        String color = rgb(style.getJSONObject("cl"));
        if (color != null) {
            css.add("color: " + color);
        }
        String background = rgb(style.getJSONObject("bg"));
        if (background != null) {
            css.add("background-color: " + background);
        }
        if (style.getIntValue("va") == 2) {
            css.add("vertical-align: sub");
        } else if (style.getIntValue("va") == 3) {
            css.add("vertical-align: super");
        }
        return String.join("; ", css);
    }

    private String cellCss(JSONObject style) {
        List<String> css = new ArrayList<>();
        String background = rgb(style.getJSONObject("backgroundColor"));
        if (background != null) {
            css.add("background-color: " + background);
        }
        css.add("vertical-align: " + switch (style.getIntValue("vAlign")) {
            case 3 -> "middle";
            case 4 -> "bottom";
            default -> "top";
        });
        css.add("border: 1px solid #b7bdc7");
        css.add("padding: 6px 8px");
        return String.join("; ", css);
    }

    private void addCssUnit(List<String> css, String name, JSONObject value) {
        if (value != null && value.getDouble("v") != null && value.getDoubleValue("v") != 0) {
            css.add(name + ": " + round(value.getDoubleValue("v")) + "px");
        }
    }

    private String rgb(JSONObject color) {
        if (color == null) {
            return null;
        }
        String value = color.getString("rgb");
        return value != null && value.matches("#[0-9a-fA-F]{6}") ? value : null;
    }

    private boolean enabled(JSONObject value) {
        return value != null && value.getIntValue("s") == 1;
    }

    private double round(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private String escapeText(String text) {
        return StrUtil.nullToEmpty(text)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String escapeAttribute(String text) {
        return escapeText(text).replace("\"", "&quot;");
    }
}