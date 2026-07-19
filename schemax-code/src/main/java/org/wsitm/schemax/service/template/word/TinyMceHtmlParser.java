package org.wsitm.schemax.service.template.word;

import cn.hutool.core.util.StrUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.wsitm.schemax.exception.ServiceException;
import org.wsitm.schemax.service.template.word.WordDocumentModel.ParagraphBlock;
import org.wsitm.schemax.service.template.word.WordDocumentModel.TableBlock;
import org.wsitm.schemax.service.template.word.WordDocumentModel.TableCell;
import org.wsitm.schemax.service.template.word.WordDocumentModel.TableRow;
import org.wsitm.schemax.service.template.word.WordDocumentModel.TextRun;
import org.wsitm.schemax.service.template.word.WordDocumentModel.WordBlock;
import org.wsitm.schemax.utils.JsonUtil;
import org.wsitm.schemax.utils.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将受控的TinyMCE HTML解析为SchemaX Word文档块。
 */
final class TinyMceHtmlParser {

    private static final double PX_PER_MM = 96D / 25.4D;
    private static final Safelist SAFELIST = Safelist.relaxed()
            .addTags("div", "span", "sub", "sup")
            .addAttributes(":all", "style", "class", "contenteditable",
                    "data-expression", "data-schemax-block", "data-directive",
                    "data-alias", "data-source")
            .addAttributes("table", "border", "cellpadding", "cellspacing", "width")
            .addAttributes("td", "rowspan", "colspan", "width", "height", "valign")
            .addAttributes("th", "rowspan", "colspan", "width", "height", "valign");

    List<WordBlock> parse(String html, boolean allowTables) {
        Document raw = Jsoup.parseBodyFragment(StrUtil.nullToEmpty(html));
        if (!raw.select("img,video,audio,iframe,object,embed,svg,canvas,script").isEmpty()) {
            throw new ServiceException("Word模板暂不支持图片、音视频、脚本或嵌入式内容");
        }
        Document cleaned = new Cleaner(SAFELIST).clean(raw);
        List<WordBlock> blocks = new ArrayList<>();
        for (Element child : cleaned.body().children()) {
            appendBlock(blocks, child, allowTables);
        }
        if (blocks.isEmpty()) {
            blocks.add(new ParagraphBlock(List.of(), new JSONObject(), null));
        }
        return blocks;
    }

    private void appendBlock(List<WordBlock> target, Element element, boolean allowTables) {
        String tag = element.normalName();
        if (isPageBreak(element)) {
            target.add(ParagraphBlock.pageBreak());
        } else if ("table".equals(tag)) {
            if (!allowTables) {
                throw new ServiceException("Word模板暂不支持页眉页脚中的表格");
            }
            target.add(parseTable(element));
        } else if ("ul".equals(tag) || "ol".equals(tag)) {
            parseList(target, element, 0);
        } else if (isParagraphTag(tag)) {
            target.add(parseParagraph(element, null));
            appendNestedLists(target, element, 1);
        } else {
            List<TextRun> runs = parseInline(element, new JSONObject());
            if (!runs.isEmpty()) {
                target.add(new ParagraphBlock(runs, paragraphStyle(element), null));
            }
        }
    }

    private void appendNestedLists(List<WordBlock> target, Element parent, int level) {
        for (Element child : parent.children()) {
            if ("ul".equals(child.normalName()) || "ol".equals(child.normalName())) {
                parseList(target, child, level);
            }
        }
    }

    private void parseList(List<WordBlock> target, Element list, int level) {
        String listType = "ol".equals(list.normalName()) ? "ordered" : "unordered";
        int order = 1;
        for (Element item : list.children()) {
            if (!"li".equals(item.normalName())) {
                continue;
            }
            JSONObject bullet = new JSONObject();
            bullet.put("listType", listType);
            bullet.put("level", level);
            bullet.put("order", order++);
            target.add(parseParagraph(item, bullet));
            appendNestedLists(target, item, level + 1);
        }
    }

    private ParagraphBlock parseParagraph(Element element, JSONObject bullet) {
        JSONObject paragraphStyle = paragraphStyle(element);
        JSONObject baseTextStyle = textStyle(element, new JSONObject());
        if (!baseTextStyle.isEmpty()) {
            paragraphStyle.put("textStyle", baseTextStyle);
        }
        List<TextRun> runs = new ArrayList<>();
        for (Node child : element.childNodes()) {
            if (child instanceof Element childElement
                    && ("ul".equals(childElement.normalName()) || "ol".equals(childElement.normalName()))) {
                continue;
            }
            appendInline(runs, child, new JSONObject());
        }
        return new ParagraphBlock(runs, paragraphStyle, bullet);
    }

    private List<TextRun> parseInline(Element element, JSONObject inherited) {
        List<TextRun> runs = new ArrayList<>();
        JSONObject style = textStyle(element, inherited);
        for (Node child : element.childNodes()) {
            appendInline(runs, child, style);
        }
        return runs;
    }

    private void appendInline(List<TextRun> target, Node node, JSONObject inherited) {
        if (node instanceof TextNode textNode) {
            appendRun(target, textNode.getWholeText().replace('\u00A0', ' '), inherited);
            return;
        }
        if (!(node instanceof Element element)) {
            return;
        }
        if ("br".equals(element.normalName())) {
            appendRun(target, String.valueOf(WordDocumentModel.LINE_BREAK), inherited);
            return;
        }
        JSONObject style = textStyle(element, inherited);
        String expression = element.attr("data-expression");
        if (StrUtil.isNotBlank(expression)) {
            appendRun(target, expression, style);
            return;
        }
        for (Node child : element.childNodes()) {
            appendInline(target, child, style);
        }
    }

    private TableBlock parseTable(Element tableElement) {
        JSONObject tableStyle = new JSONObject();
        tableStyle.put("align", switch (css(tableElement).getOrDefault("text-align", "")) {
            case "center" -> 1;
            case "right" -> 2;
            default -> 0;
        });
        List<TableRow> rows = new ArrayList<>();
        Map<Integer, RowSpanState> spans = new LinkedHashMap<>();
        for (Element rowElement : tableElement.getElementsByTag("tr")) {
            if (closestParent(rowElement, "table") != tableElement) {
                continue;
            }
            List<TableCell> cells = new ArrayList<>();
            int column = 0;
            for (Element cellElement : rowElement.children()) {
                if (!"td".equals(cellElement.normalName()) && !"th".equals(cellElement.normalName())) {
                    continue;
                }
                while (spans.containsKey(column)) {
                    RowSpanState span = spans.get(column);
                    cells.add(coveredCell(span.columnSpan()));
                    decrementSpan(spans, column, span);
                    column += span.columnSpan();
                }
                int rowSpan = positiveInt(cellElement.attr("rowspan"), 1);
                int columnSpan = positiveInt(cellElement.attr("colspan"), 1);
                cells.add(new TableCell(cellStyle(cellElement, rowSpan, columnSpan),
                        parseCellParagraphs(cellElement)));
                if (rowSpan > 1) {
                    spans.put(column, new RowSpanState(rowSpan - 1, columnSpan));
                }
                column += columnSpan;
            }
            List<Integer> trailing = new ArrayList<>(spans.keySet());
            trailing.sort(Integer::compareTo);
            for (Integer spanColumn : trailing) {
                if (spanColumn < column) {
                    continue;
                }
                RowSpanState span = spans.get(spanColumn);
                cells.add(coveredCell(span.columnSpan()));
                decrementSpan(spans, spanColumn, span);
                column = spanColumn + span.columnSpan();
            }
            JSONObject rowStyle = new JSONObject();
            if ("thead".equals(rowElement.parent().normalName())) {
                rowStyle.put("repeatHeaderRow", 1);
            }
            if (!cells.isEmpty()) {
                rows.add(new TableRow(rowStyle, cells));
            }
        }
        return new TableBlock(tableStyle, rows);
    }

    private void decrementSpan(Map<Integer, RowSpanState> spans, int column, RowSpanState span) {
        if (span.remaining() <= 1) {
            spans.remove(column);
        } else {
            spans.put(column, new RowSpanState(span.remaining() - 1, span.columnSpan()));
        }
    }

    private TableCell coveredCell(int columnSpan) {
        JSONObject style = new JSONObject();
        style.put("rowSpan", 0);
        style.put("columnSpan", columnSpan);
        return new TableCell(style, List.of());
    }

    private List<ParagraphBlock> parseCellParagraphs(Element cell) {
        List<ParagraphBlock> paragraphs = new ArrayList<>();
        for (Element child : cell.children()) {
            if (isParagraphTag(child.normalName())) {
                paragraphs.add(parseParagraph(child, null));
            }
        }
        if (paragraphs.isEmpty()) {
            paragraphs.add(new ParagraphBlock(parseInline(cell, new JSONObject()), new JSONObject(), null));
        }
        return paragraphs;
    }

    private JSONObject paragraphStyle(Element element) {
        Map<String, String> css = css(element);
        JSONObject style = new JSONObject();
        style.put("horizontalAlign", switch (css.getOrDefault("text-align", "").toLowerCase(Locale.ROOT)) {
            case "center" -> 2;
            case "right" -> 3;
            case "justify" -> 4;
            default -> 1;
        });
        putUnit(style, "spaceAbove", css.get("margin-top"));
        putUnit(style, "spaceBelow", css.get("margin-bottom"));
        putUnit(style, "indentStart", css.get("margin-left"));
        putUnit(style, "indentEnd", css.get("margin-right"));
        String lineHeight = css.get("line-height");
        if (lineHeight != null && lineHeight.matches("\\d+(?:\\.\\d+)?")) {
            style.put("lineSpacing", Double.parseDouble(lineHeight));
        }
        if (element.normalName().matches("h[1-6]")) {
            style.put("namedStyleType", Integer.parseInt(element.normalName().substring(1)) + 3);
            style.put("keepNext", 1);
        }
        if ("blockquote".equals(element.normalName())) {
            style.put("indentStart", unit(24));
        }
        return style;
    }

    private JSONObject textStyle(Element element, JSONObject inherited) {
        JSONObject style = copy(inherited);
        String tag = element.normalName();
        if ("strong".equals(tag) || "b".equals(tag) || "th".equals(tag)) {
            style.put("bl", 1);
        }
        if ("em".equals(tag) || "i".equals(tag)) {
            style.put("it", 1);
        }
        if ("u".equals(tag)) {
            style.put("ul", decoration());
        }
        if ("s".equals(tag) || "strike".equals(tag) || "del".equals(tag)) {
            style.put("st", decoration());
        }
        if ("sub".equals(tag)) {
            style.put("va", 2);
        } else if ("sup".equals(tag)) {
            style.put("va", 3);
        }
        Map<String, String> css = css(element);
        String family = css.get("font-family");
        if (StrUtil.isNotBlank(family)) {
            style.put("ff", family.replace("\"", "").replace("'", "").split(",")[0].trim());
        }
        Double fontSize = parsePointSize(css.get("font-size"));
        if (fontSize != null) {
            style.put("fs", fontSize);
        }
        String weight = css.get("font-weight");
        if ("bold".equalsIgnoreCase(weight) || parseInt(weight, 0) >= 600) {
            style.put("bl", 1);
        }
        if ("italic".equalsIgnoreCase(css.get("font-style"))) {
            style.put("it", 1);
        }
        String line = css.getOrDefault("text-decoration", "")
                + " " + css.getOrDefault("text-decoration-line", "");
        if (line.contains("underline")) {
            style.put("ul", decoration());
        }
        if (line.contains("line-through")) {
            style.put("st", decoration());
        }
        String color = normalizeColor(css.get("color"));
        if (color != null) {
            style.put("cl", color(color));
        }
        String background = normalizeColor(css.get("background-color"));
        if (background != null) {
            style.put("bg", color(background));
        }
        return style;
    }

    private JSONObject cellStyle(Element element, int rowSpan, int columnSpan) {
        JSONObject style = new JSONObject();
        style.put("rowSpan", rowSpan);
        style.put("columnSpan", columnSpan);
        Map<String, String> css = css(element);
        String background = normalizeColor(css.get("background-color"));
        if (background != null) {
            style.put("backgroundColor", color(background));
        }
        String vertical = StrUtil.blankToDefault(css.get("vertical-align"), element.attr("valign"));
        style.put("vAlign", switch (vertical.toLowerCase(Locale.ROOT)) {
            case "middle", "center" -> 3;
            case "bottom" -> 4;
            default -> 2;
        });
        return style;
    }

    private Map<String, String> css(Element element) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String declaration : element.attr("style").split(";")) {
            int colon = declaration.indexOf(':');
            if (colon > 0) {
                String name = declaration.substring(0, colon).trim().toLowerCase(Locale.ROOT);
                String value = declaration.substring(colon + 1).trim();
                if (!name.isEmpty() && !value.isEmpty()) {
                    values.put(name, value);
                }
            }
        }
        return values;
    }

    private Element closestParent(Element element, String tag) {
        Element parent = element.parent();
        while (parent != null && !tag.equals(parent.normalName())) {
            parent = parent.parent();
        }
        return parent;
    }

    private boolean isParagraphTag(String tag) {
        return tag.matches("p|div|h[1-6]|blockquote|pre|li");
    }

    private boolean isPageBreak(Element element) {
        return "page-break".equals(element.attr("data-schemax-block"))
                || element.hasClass("schemax-page-break");
    }

    private void appendRun(List<TextRun> target, String text, JSONObject style) {
        if (text == null || text.isEmpty()) {
            return;
        }
        JSONObject copied = copy(style);
        if (!target.isEmpty()) {
            TextRun previous = target.get(target.size() - 1);
            if (Objects.equals(previous.getStyle().toJSONString(), copied.toJSONString())) {
                previous.text += text;
                return;
            }
        }
        target.add(new TextRun(text, copied));
    }

    private JSONObject copy(JSONObject source) {
        return source == null ? new JSONObject() : JsonUtil.parseObject(source.toJSONString());
    }

    private JSONObject decoration() {
        JSONObject value = new JSONObject();
        value.put("s", 1);
        return value;
    }

    private JSONObject color(String rgb) {
        JSONObject value = new JSONObject();
        value.put("rgb", rgb);
        return value;
    }

    private JSONObject unit(double value) {
        JSONObject result = new JSONObject();
        result.put("v", value);
        return result;
    }

    private void putUnit(JSONObject target, String key, String value) {
        Double pixels = parsePixels(value);
        if (pixels != null) {
            target.put(key, unit(pixels));
        }
    }

    private Double parsePointSize(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        try {
            if (normalized.endsWith("pt")) {
                return Double.parseDouble(normalized.substring(0, normalized.length() - 2));
            }
            if (normalized.endsWith("px")) {
                return Double.parseDouble(normalized.substring(0, normalized.length() - 2)) * 0.75D;
            }
            return Double.parseDouble(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Double parsePixels(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        try {
            if (normalized.endsWith("px")) {
                return Double.parseDouble(normalized.substring(0, normalized.length() - 2));
            }
            if (normalized.endsWith("pt")) {
                return Double.parseDouble(normalized.substring(0, normalized.length() - 2)) / 0.75D;
            }
            if (normalized.endsWith("mm")) {
                return Double.parseDouble(normalized.substring(0, normalized.length() - 2)) * PX_PER_MM;
            }
            return Double.parseDouble(normalized);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String normalizeColor(String color) {
        if (StrUtil.isBlank(color)) {
            return null;
        }
        String value = color.trim().toLowerCase(Locale.ROOT);
        if (value.matches("#[0-9a-f]{6}")) {
            return value;
        }
        if (value.matches("#[0-9a-f]{3}")) {
            return "#" + value.charAt(1) + value.charAt(1)
                    + value.charAt(2) + value.charAt(2)
                    + value.charAt(3) + value.charAt(3);
        }
        Matcher rgb = Pattern.compile("rgba?\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)").matcher(value);
        if (rgb.find()) {
            return String.format("#%02x%02x%02x",
                    Math.min(255, Integer.parseInt(rgb.group(1))),
                    Math.min(255, Integer.parseInt(rgb.group(2))),
                    Math.min(255, Integer.parseInt(rgb.group(3))));
        }
        return null;
    }

    private int positiveInt(String value, int fallback) {
        int result = parseInt(value, fallback);
        return result > 0 ? result : fallback;
    }

    private int parseInt(String value, int fallback) {
        try {
            return StrUtil.isBlank(value) ? fallback : Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private record RowSpanState(int remaining, int columnSpan) {
    }
}