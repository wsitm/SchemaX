package org.wsitm.schemax.service.template.word;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Component;
import org.wsitm.schemax.exception.ServiceException;
import org.wsitm.schemax.service.template.word.WordDocumentModel.HeaderFooterBody;
import org.wsitm.schemax.service.template.word.WordDocumentModel.WordBlock;
import org.wsitm.schemax.service.template.word.WordDocumentModel.WordDocument;
import org.wsitm.schemax.utils.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TinyMCE模板内容信封与SchemaX中立Word文档模型之间的适配器。
 */
@Component
public class TinyMceWordTemplateAdapter {

    public static final int FORMAT_VERSION = 2;
    public static final String EDITOR_NAME = "tinymce";
    private static final double PX_PER_MM = 96D / 25.4D;

    private final TinyMceHtmlParser parser = new TinyMceHtmlParser();
    private final TinyMceHtmlRenderer renderer = new TinyMceHtmlRenderer();

    public boolean supports(JSONObject root) {
        return root != null
                && FORMAT_VERSION == root.getIntValue("version")
                && EDITOR_NAME.equalsIgnoreCase(root.getString("editor"));
    }

    public WordDocument parse(JSONObject envelope) {
        if (!supports(envelope)) {
            throw new ServiceException("Word模板不是有效的TinyMCE模板数据");
        }
        JSONObject root = new JSONObject();
        JSONObject documentStyle = pageToDocumentStyle(envelope.getJSONObject("page"));
        root.put("documentStyle", documentStyle);
        Map<String, HeaderFooterBody> headers = parseHeaderFooter(
                envelope.getJSONObject("headers"), "header", "headerId", documentStyle
        );
        Map<String, HeaderFooterBody> footers = parseHeaderFooter(
                envelope.getJSONObject("footers"), "footer", "footerId", documentStyle
        );
        String bodyHtml = StrUtil.blankToDefault(envelope.getString("bodyHtml"), "<p></p>");
        return new WordDocument(root, parser.parse(bodyHtml, true), headers, footers);
    }

    public JSONObject serialize(WordDocument document) {
        JSONObject envelope = new JSONObject();
        envelope.put("version", FORMAT_VERSION);
        envelope.put("editor", EDITOR_NAME);
        envelope.put("format", "html");
        envelope.put("bodyHtml", renderer.render(document.getBlocks()));
        envelope.put("page", documentStyleToPage(document.getDocumentStyle()));
        envelope.put("headers", serializeHeaderFooter(document, true));
        envelope.put("footers", serializeHeaderFooter(document, false));
        return envelope;
    }

    public JSONObject createDefaultContent() {
        JSONObject result = new JSONObject();
        result.put("version", FORMAT_VERSION);
        result.put("editor", EDITOR_NAME);
        result.put("format", "html");
        result.put("bodyHtml", "<h1>\u0024{tableName}</h1><p>\u0024{tableComment}</p>"
                + "<table style=\"width: 100%\"><thead><tr><th>字段</th><th>类型</th>"
                + "<th>长度</th><th>可空</th><th>注释</th></tr></thead><tbody>"
                + "<tr><td>\u0024{name}</td><td>\u0024{type}</td><td>\u0024{size}</td>"
                + "<td>\u0024{nullable}</td><td>\u0024{comment}</td></tr></tbody></table>");
        result.put("page", defaultPage());
        result.put("headers", new JSONObject());
        result.put("footers", new JSONObject());
        return result;
    }

    private Map<String, HeaderFooterBody> parseHeaderFooter(JSONObject source, String prefix,
                                                             String idField, JSONObject style) {
        Map<String, HeaderFooterBody> result = new LinkedHashMap<>();
        if (source == null) {
            return result;
        }
        for (String type : List.of("default", "first", "even")) {
            String html = source.getString(type);
            if (StrUtil.isBlank(html)) {
                continue;
            }
            String id = prefix + "-" + type;
            List<WordBlock> blocks = parser.parse(html, false);
            result.put(id, new HeaderFooterBody(id, idField, blocks));
            String field = switch (type) {
                case "first" -> "firstPage" + capitalize(prefix) + "Id";
                case "even" -> "evenPage" + capitalize(prefix) + "Id";
                default -> "default" + capitalize(prefix) + "Id";
            };
            style.put(field, id);
        }
        return result;
    }

    private JSONObject serializeHeaderFooter(WordDocument document, boolean header) {
        JSONObject result = new JSONObject();
        JSONObject style = document.getDocumentStyle();
        if (style == null) {
            return result;
        }
        String name = header ? "Header" : "Footer";
        Map<String, HeaderFooterBody> source = header ? document.getHeaders() : document.getFooters();
        putSection(result, "default", source, style.getString("default" + name + "Id"));
        putSection(result, "first", source, style.getString("firstPage" + name + "Id"));
        putSection(result, "even", source, style.getString("evenPage" + name + "Id"));
        return result;
    }

    private void putSection(JSONObject target, String type,
                            Map<String, HeaderFooterBody> source, String id) {
        HeaderFooterBody body = StrUtil.isBlank(id) ? null : source.get(id);
        if (body != null) {
            target.put(type, renderer.render(body.getBlocks()));
        }
    }

    private JSONObject pageToDocumentStyle(JSONObject page) {
        JSONObject source = page == null ? defaultPage() : page;
        double width = source.getDouble("widthMm") == null ? 210D : source.getDoubleValue("widthMm");
        double height = source.getDouble("heightMm") == null ? 297D : source.getDoubleValue("heightMm");
        if ("landscape".equalsIgnoreCase(source.getString("orientation"))) {
            double swap = width;
            width = height;
            height = swap;
        }
        JSONObject size = new JSONObject();
        size.put("width", width * PX_PER_MM);
        size.put("height", height * PX_PER_MM);

        JSONObject style = new JSONObject();
        style.put("pageSize", size);
        JSONObject margins = source.getJSONObject("margins");
        style.put("marginTop", mmToPx(margins, "top", 20));
        style.put("marginBottom", mmToPx(margins, "bottom", 20));
        style.put("marginLeft", mmToPx(margins, "left", 25));
        style.put("marginRight", mmToPx(margins, "right", 25));
        style.put("marginHeader", mmToPx(margins, "header", 10));
        style.put("marginFooter", mmToPx(margins, "footer", 10));
        style.put("useFirstPageHeaderFooter", source.getBooleanValue("differentFirstPage") ? 1 : 0);
        style.put("evenAndOddHeaders", source.getBooleanValue("differentOddEven") ? 1 : 0);

        JSONObject textStyle = new JSONObject();
        textStyle.put("ff", StrUtil.blankToDefault(source.getString("fontFamily"), "Microsoft YaHei"));
        textStyle.put("fs", source.getDouble("fontSize") == null ? 10.5D : source.getDoubleValue("fontSize"));
        style.put("textStyle", textStyle);
        return style;
    }

    private JSONObject documentStyleToPage(JSONObject style) {
        JSONObject page = defaultPage();
        if (style == null) {
            return page;
        }
        JSONObject size = style.getJSONObject("pageSize");
        if (size != null && size.getDouble("width") != null && size.getDouble("height") != null) {
            double width = size.getDoubleValue("width") / PX_PER_MM;
            double height = size.getDoubleValue("height") / PX_PER_MM;
            boolean landscape = width > height;
            double portraitWidth = landscape ? height : width;
            double portraitHeight = landscape ? width : height;
            page.put("size", detectPageSize(portraitWidth, portraitHeight));
            page.put("orientation", landscape ? "landscape" : "portrait");
            page.put("widthMm", round(portraitWidth));
            page.put("heightMm", round(portraitHeight));
        }
        JSONObject margins = page.getJSONObject("margins");
        margins.put("top", pxToMm(style.getDouble("marginTop"), 20));
        margins.put("bottom", pxToMm(style.getDouble("marginBottom"), 20));
        margins.put("left", pxToMm(style.getDouble("marginLeft"), 25));
        margins.put("right", pxToMm(style.getDouble("marginRight"), 25));
        margins.put("header", pxToMm(style.getDouble("marginHeader"), 10));
        margins.put("footer", pxToMm(style.getDouble("marginFooter"), 10));
        page.put("differentFirstPage", style.getIntValue("useFirstPageHeaderFooter") == 1);
        page.put("differentOddEven", style.getIntValue("evenAndOddHeaders") == 1);

        JSONObject textStyle = style.getJSONObject("textStyle");
        if (textStyle != null) {
            page.put("fontFamily", StrUtil.blankToDefault(textStyle.getString("ff"), "Microsoft YaHei"));
            page.put("fontSize", textStyle.getDouble("fs") == null ? 10.5D : textStyle.getDoubleValue("fs"));
        }
        return page;
    }

    private JSONObject defaultPage() {
        JSONObject margins = new JSONObject();
        margins.put("top", 20);
        margins.put("bottom", 20);
        margins.put("left", 25);
        margins.put("right", 25);
        margins.put("header", 10);
        margins.put("footer", 10);
        JSONObject page = new JSONObject();
        page.put("size", "A4");
        page.put("orientation", "portrait");
        page.put("widthMm", 210);
        page.put("heightMm", 297);
        page.put("margins", margins);
        page.put("differentFirstPage", false);
        page.put("differentOddEven", false);
        page.put("fontFamily", "Microsoft YaHei");
        page.put("fontSize", 10.5D);
        return page;
    }

    private String detectPageSize(double width, double height) {
        if (Math.abs(width - 297D) < 1D && Math.abs(height - 420D) < 1D) {
            return "A3";
        }
        if (Math.abs(width - 215.9D) < 1D && Math.abs(height - 279.4D) < 1D) {
            return "LETTER";
        }
        return "A4";
    }

    private double mmToPx(JSONObject margins, String key, double fallback) {
        Double value = margins == null ? null : margins.getDouble(key);
        return (value == null ? fallback : value) * PX_PER_MM;
    }

    private double pxToMm(Double value, double fallback) {
        return value == null ? fallback : round(value / PX_PER_MM);
    }

    private double round(double value) {
        return Math.round(value * 100D) / 100D;
    }

    private String capitalize(String value) {
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}