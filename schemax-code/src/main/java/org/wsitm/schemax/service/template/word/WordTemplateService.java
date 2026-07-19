package org.wsitm.schemax.service.template.word;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.wsitm.schemax.entity.vo.TableVO;
import org.wsitm.schemax.exception.ServiceException;
import org.wsitm.schemax.service.impl.TemplateContextService;
import org.wsitm.schemax.utils.JsonUtil;
import org.wsitm.schemax.utils.json.JSONArray;
import org.wsitm.schemax.utils.json.JSONObject;

import org.wsitm.schemax.service.template.word.WordDocumentModel.HeaderFooterBody;
import org.wsitm.schemax.service.template.word.WordDocumentModel.ParagraphBlock;
import org.wsitm.schemax.service.template.word.WordDocumentModel.TableBlock;
import org.wsitm.schemax.service.template.word.WordDocumentModel.TableCell;
import org.wsitm.schemax.service.template.word.WordDocumentModel.TableRow;
import org.wsitm.schemax.service.template.word.WordDocumentModel.TextRun;
import org.wsitm.schemax.service.template.word.WordDocumentModel.WordBlock;
import org.wsitm.schemax.service.template.word.WordDocumentModel.WordDocument;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Word模板渲染服务，兼容旧版Univer快照和新版TinyMCE HTML模板。
 */
@Service
public class WordTemplateService {

    static final char PARAGRAPH = '\r';
    static final char SECTION_BREAK = '\n';
    static final char TABLE_START = '\u001A';
    static final char TABLE_ROW_START = '\u001B';
    static final char TABLE_CELL_START = '\u001C';
    static final char TABLE_CELL_END = '\u001D';
    static final char TABLE_ROW_END = '\u000E';
    static final char TABLE_END = '\u000F';
    static final char PAGE_BREAK = WordDocumentModel.PAGE_BREAK;
    static final char LINE_BREAK = WordDocumentModel.LINE_BREAK;

    private static final Pattern FOR_DIRECTIVE = Pattern.compile(
            "^#for\\s*\\(\\s*([a-zA-Z_]\\w*)\\s+in\\s+([a-zA-Z_][\\w.]*)\\s*\\)\\s*$"
    );
    private static final Pattern END_DIRECTIVE = Pattern.compile("^#end\\s*$");
    private static final Pattern EXPRESSION = Pattern.compile("\\$\\{\\s*([^}]+?)\\s*}");

    private final TemplateContextService contextService;
    private final WordDocumentExporter documentExporter;
    private final TinyMceWordTemplateAdapter tinyMceAdapter;

    public WordTemplateService(TemplateContextService contextService,
                               WordDocumentExporter documentExporter) {
        this(contextService, documentExporter, new TinyMceWordTemplateAdapter());
    }

    @Autowired
    public WordTemplateService(TemplateContextService contextService,
                               WordDocumentExporter documentExporter,
                               TinyMceWordTemplateAdapter tinyMceAdapter) {
        this.contextService = contextService;
        this.documentExporter = documentExporter;
        this.tinyMceAdapter = tinyMceAdapter;
    }

    public void validateSnapshot(String templateContent) {
        parseTemplate(templateContent);
    }

    public JSONObject renderSnapshot(List<TableVO> tableList, String templateContent) {
        WordDocument document = renderDocument(tableList, templateContent);
        return serialize(document);
    }

    public JSONObject toEditorContent(String templateContent) {
        return tinyMceAdapter.serialize(parseTemplate(templateContent));
    }

    public JSONObject createDefaultEditorContent() {
        return tinyMceAdapter.createDefaultContent();
    }

    public JSONObject renderHtmlPreview(List<TableVO> tableList, String templateContent) {
        return tinyMceAdapter.serialize(renderDocument(tableList, templateContent));
    }

    public void export(File target, List<TableVO> tableList, String templateContent) throws IOException {
        documentExporter.export(target, renderDocument(tableList, templateContent));
    }

    WordDocument renderDocument(List<TableVO> tableList, String templateContent) {
        WordDocument template = parseTemplate(templateContent);
        List<Map<String, Object>> contexts = new ArrayList<>();
        if (tableList != null) {
            for (int i = 0; i < tableList.size(); i++) {
                contexts.add(contextService.buildTableContext(tableList.get(i), i + 1));
            }
        }
        if (contexts.isEmpty()) {
            contexts.add(Map.of());
        }

        List<WordBlock> renderedBlocks = new ArrayList<>();
        for (Map<String, Object> context : contexts) {
            renderedBlocks.addAll(renderBlocks(template.blocks, context));
        }

        Map<String, HeaderFooterBody> renderedHeaders = renderHeaderFooter(template.headers, contexts.get(0));
        Map<String, HeaderFooterBody> renderedFooters = renderHeaderFooter(template.footers, contexts.get(0));
        return new WordDocument(copyJson(template.root), renderedBlocks, renderedHeaders, renderedFooters);
    }

    private WordDocument parseTemplate(String templateContent) {
        if (StrUtil.isBlank(templateContent)) {
            throw new ServiceException("Word模板内容不能为空");
        }
        JSONObject root;
        try {
            root = JsonUtil.parseObject(templateContent);
        } catch (Exception exception) {
            throw new ServiceException("Word模板内容不是有效的文档数据");
        }
        if (tinyMceAdapter.supports(root)) {
            return tinyMceAdapter.parse(root);
        }
        JSONObject body = root == null ? null : root.getJSONObject("body");
        if (body == null || body.getString("dataStream") == null) {
            throw new ServiceException("Word模板缺少正文数据");
        }
        JSONArray customBlocks = body.getJSONArray("customBlocks");
        JSONObject drawings = root.getJSONObject("drawings");
        if ((customBlocks != null && !customBlocks.isEmpty()) || (drawings != null && !drawings.isEmpty())) {
            throw new ServiceException("Word模板第一期暂不支持图片或自定义文档块");
        }

        JSONObject tableSource = root.getJSONObject("tableSource");
        List<WordBlock> blocks = parseBlocks(body, tableSource == null ? new JSONObject() : tableSource);
        Map<String, HeaderFooterBody> headers = parseHeaderFooter(root.getJSONObject("headers"), "headerId");
        Map<String, HeaderFooterBody> footers = parseHeaderFooter(root.getJSONObject("footers"), "footerId");
        return new WordDocument(copyJson(root), blocks, headers, footers);
    }

    private Map<String, HeaderFooterBody> parseHeaderFooter(JSONObject source, String idField) {
        Map<String, HeaderFooterBody> result = new LinkedHashMap<>();
        if (source == null) {
            return result;
        }
        for (String id : source.keySet()) {
            JSONObject data = source.getJSONObject(id);
            JSONObject body = data == null ? null : data.getJSONObject("body");
            if (body == null) {
                continue;
            }
            List<WordBlock> blocks = parseBlocks(body, new JSONObject());
            for (WordBlock block : blocks) {
                if (block instanceof TableBlock) {
                    throw new ServiceException("Word模板第一期暂不支持页眉页脚中的表格");
                }
            }
            result.put(id, new HeaderFooterBody(id, idField, blocks));
        }
        return result;
    }

    private List<WordBlock> parseBlocks(JSONObject body, JSONObject tableSource) {
        String dataStream = StrUtil.nullToEmpty(body.getString("dataStream"));
        List<StyleSpan> textStyles = parseTextStyles(body.getJSONArray("textRuns"));
        Map<Integer, ParagraphMeta> paragraphs = parseParagraphs(body.getJSONArray("paragraphs"));
        Map<Integer, String> tableIds = parseTableIds(body.getJSONArray("tables"));
        List<WordBlock> blocks = new ArrayList<>();

        int index = 0;
        int paragraphStart = 0;
        while (index < dataStream.length()) {
            char token = dataStream.charAt(index);
            if (token == TABLE_START) {
                if (paragraphStart < index) {
                    blocks.add(parseParagraph(dataStream, paragraphStart, index, paragraphs.get(index), textStyles));
                }
                ParsedTable parsedTable = parseTable(dataStream, index, tableIds.get(index), tableSource,
                        paragraphs, textStyles);
                blocks.add(parsedTable.table);
                index = parsedTable.nextIndex;
                paragraphStart = index;
                continue;
            }
            if (token == PARAGRAPH) {
                blocks.add(parseParagraph(dataStream, paragraphStart, index, paragraphs.get(index), textStyles));
                index++;
                paragraphStart = index;
                continue;
            }
            if (token == SECTION_BREAK || token == '\0') {
                if (paragraphStart < index) {
                    blocks.add(parseParagraph(dataStream, paragraphStart, index, null, textStyles));
                }
                index++;
                paragraphStart = index;
                continue;
            }
            index++;
        }
        if (paragraphStart < dataStream.length()) {
            blocks.add(parseParagraph(dataStream, paragraphStart, dataStream.length(), null, textStyles));
        }
        return blocks;
    }

    private ParsedTable parseTable(String stream,
                                   int startIndex,
                                   String tableId,
                                   JSONObject tableSource,
                                   Map<Integer, ParagraphMeta> paragraphs,
                                   List<StyleSpan> textStyles) {
        JSONObject tableStyle = tableId == null ? new JSONObject() : copyJson(tableSource.getJSONObject(tableId));
        JSONArray sourceRows = tableStyle.getJSONArray("tableRows");
        List<TableRow> rows = new ArrayList<>();
        int index = startIndex + 1;
        int rowIndex = 0;
        while (index < stream.length() && stream.charAt(index) != TABLE_END) {
            if (stream.charAt(index) != TABLE_ROW_START) {
                index++;
                continue;
            }
            JSONObject sourceRow = sourceRows != null && rowIndex < sourceRows.size()
                    ? copyJson(sourceRows.getJSONObject(rowIndex)) : new JSONObject();
            JSONArray sourceCells = sourceRow.getJSONArray("tableCells");
            List<TableCell> cells = new ArrayList<>();
            index++;
            int cellIndex = 0;
            while (index < stream.length() && stream.charAt(index) != TABLE_ROW_END) {
                if (stream.charAt(index) != TABLE_CELL_START) {
                    index++;
                    continue;
                }
                int cellStart = ++index;
                while (index < stream.length() && stream.charAt(index) != TABLE_CELL_END) {
                    index++;
                }
                JSONObject sourceCell = sourceCells != null && cellIndex < sourceCells.size()
                        ? copyJson(sourceCells.getJSONObject(cellIndex)) : new JSONObject();
                List<ParagraphBlock> cellParagraphs = parseParagraphRange(
                        stream, cellStart, index, paragraphs, textStyles
                );
                if (cellParagraphs.isEmpty()) {
                    cellParagraphs.add(new ParagraphBlock(List.of(), new JSONObject(), null));
                }
                cells.add(new TableCell(sourceCell, cellParagraphs));
                cellIndex++;
                if (index < stream.length()) {
                    index++;
                }
            }
            rows.add(new TableRow(sourceRow, cells));
            rowIndex++;
            if (index < stream.length() && stream.charAt(index) == TABLE_ROW_END) {
                index++;
            }
        }
        if (index < stream.length() && stream.charAt(index) == TABLE_END) {
            index++;
        }
        return new ParsedTable(new TableBlock(tableStyle, rows), index);
    }

    private List<ParagraphBlock> parseParagraphRange(String stream,
                                                     int start,
                                                     int end,
                                                     Map<Integer, ParagraphMeta> paragraphs,
                                                     List<StyleSpan> textStyles) {
        List<ParagraphBlock> result = new ArrayList<>();
        int paragraphStart = start;
        for (int index = start; index < end; index++) {
            char token = stream.charAt(index);
            if (token == PARAGRAPH) {
                result.add(parseParagraph(stream, paragraphStart, index, paragraphs.get(index), textStyles));
                paragraphStart = index + 1;
            } else if (token == SECTION_BREAK) {
                if (paragraphStart < index) {
                    result.add(parseParagraph(stream, paragraphStart, index, null, textStyles));
                }
                paragraphStart = index + 1;
            }
        }
        if (paragraphStart < end) {
            result.add(parseParagraph(stream, paragraphStart, end, null, textStyles));
        }
        return result;
    }

    private ParagraphBlock parseParagraph(String stream,
                                          int start,
                                          int end,
                                          ParagraphMeta paragraphMeta,
                                          List<StyleSpan> textStyles) {
        List<TextRun> runs = new ArrayList<>();
        int cursor = start;
        while (cursor < end) {
            StyleSpan span = findStyle(textStyles, cursor);
            int next = span == null ? nextStyleStart(textStyles, cursor, end) : Math.min(end, span.end + 1);
            if (next <= cursor) {
                next = cursor + 1;
            }
            String text = stream.substring(cursor, next);
            if (!text.isEmpty()) {
                runs.add(new TextRun(text, span == null ? new JSONObject() : copyJson(span.style)));
            }
            cursor = next;
        }
        JSONObject paragraphStyle = paragraphMeta == null ? new JSONObject() : copyJson(paragraphMeta.style);
        JSONObject bullet = paragraphMeta == null ? null : copyJson(paragraphMeta.bullet);
        return new ParagraphBlock(runs, paragraphStyle, bullet);
    }

    private List<StyleSpan> parseTextStyles(JSONArray textRuns) {
        List<StyleSpan> result = new ArrayList<>();
        if (textRuns == null) {
            return result;
        }
        for (int i = 0; i < textRuns.size(); i++) {
            JSONObject run = textRuns.getJSONObject(i);
            if (run == null) {
                continue;
            }
            Integer start = run.getInteger("st");
            Integer end = run.getInteger("ed");
            if (start != null && end != null && end >= start) {
                result.add(new StyleSpan(start, end, copyJson(run.getJSONObject("ts"))));
            }
        }
        result.sort((left, right) -> Integer.compare(left.start, right.start));
        return result;
    }

    private Map<Integer, ParagraphMeta> parseParagraphs(JSONArray paragraphs) {
        Map<Integer, ParagraphMeta> result = new LinkedHashMap<>();
        if (paragraphs == null) {
            return result;
        }
        for (int i = 0; i < paragraphs.size(); i++) {
            JSONObject paragraph = paragraphs.getJSONObject(i);
            Integer start = paragraph == null ? null : paragraph.getInteger("startIndex");
            if (start != null) {
                result.put(start, new ParagraphMeta(
                        copyJson(paragraph.getJSONObject("paragraphStyle")),
                        copyJson(paragraph.getJSONObject("bullet"))
                ));
            }
        }
        return result;
    }

    private Map<Integer, String> parseTableIds(JSONArray tables) {
        Map<Integer, String> result = new LinkedHashMap<>();
        if (tables == null) {
            return result;
        }
        for (int i = 0; i < tables.size(); i++) {
            JSONObject table = tables.getJSONObject(i);
            Integer start = table == null ? null : table.getInteger("startIndex");
            if (start != null) {
                result.put(start, table.getString("tableId"));
            }
        }
        return result;
    }

    private StyleSpan findStyle(List<StyleSpan> styles, int index) {
        for (StyleSpan span : styles) {
            if (span.start > index) {
                return null;
            }
            if (span.start <= index && span.end >= index) {
                return span;
            }
        }
        return null;
    }

    private int nextStyleStart(List<StyleSpan> styles, int index, int fallback) {
        for (StyleSpan span : styles) {
            if (span.start > index) {
                return Math.min(fallback, span.start);
            }
        }
        return fallback;
    }

    private List<WordBlock> renderBlocks(List<WordBlock> source, Map<String, Object> context) {
        List<WordBlock> result = new ArrayList<>();
        int index = 0;
        while (index < source.size()) {
            WordBlock block = source.get(index);
            Directive directive = block instanceof ParagraphBlock paragraph
                    ? parseDirective(paragraph.plainText()) : null;
            if (directive != null && directive.type == DirectiveType.FOR) {
                int endIndex = findBlockEnd(source, index + 1);
                if (endIndex < 0) {
                    throw new ServiceException("Word模板循环指令缺少#end");
                }
                List<WordBlock> nested = source.subList(index + 1, endIndex);
                List<Map<String, Object>> items = contextService.resolveLoopSource(context, directive.listExpression);
                for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
                    Map<String, Object> loopContext = contextService.buildLoopContext(
                            context, directive.alias, items.get(itemIndex), itemIndex + 1
                    );
                    result.addAll(renderBlocks(nested, loopContext));
                }
                index = endIndex + 1;
                continue;
            }
            if (directive != null && directive.type == DirectiveType.END) {
                index++;
                continue;
            }
            if (block instanceof ParagraphBlock paragraph
                    && !contextService.isInForContext(context)
                    && contextService.hasColumnExpression(paragraph.plainText())) {
                List<Map<String, Object>> columns = contextService.getContextColumnList(context);
                if (!columns.isEmpty()) {
                    for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                        Map<String, Object> columnContext = contextService.buildLoopContext(
                                context, "col", columns.get(columnIndex), columnIndex + 1
                        );
                        result.add(renderParagraph(paragraph, columnContext));
                    }
                    index++;
                    continue;
                }
            }
            result.add(renderBlock(block, context));
            index++;
        }
        return result;
    }

    private int findBlockEnd(List<WordBlock> blocks, int fromIndex) {
        int depth = 0;
        for (int index = fromIndex; index < blocks.size(); index++) {
            if (!(blocks.get(index) instanceof ParagraphBlock paragraph)) {
                continue;
            }
            Directive directive = parseDirective(paragraph.plainText());
            if (directive == null) {
                continue;
            }
            if (directive.type == DirectiveType.FOR) {
                depth++;
            } else if (depth == 0) {
                return index;
            } else {
                depth--;
            }
        }
        return -1;
    }

    private WordBlock renderBlock(WordBlock block, Map<String, Object> context) {
        if (block instanceof ParagraphBlock paragraph) {
            return renderParagraph(paragraph, context);
        }
        if (block instanceof TableBlock table) {
            return renderTable(table, context);
        }
        throw new ServiceException("Word模板包含无法识别的文档块");
    }

    private TableBlock renderTable(TableBlock table, Map<String, Object> context) {
        List<TableRow> rows = renderRows(table.rows, context);
        return new TableBlock(copyJson(table.style), rows);
    }

    private List<TableRow> renderRows(List<TableRow> source, Map<String, Object> context) {
        List<TableRow> result = new ArrayList<>();
        int index = 0;
        while (index < source.size()) {
            TableRow row = source.get(index);
            Directive directive = parseRowDirective(row);
            if (directive != null && directive.type == DirectiveType.FOR) {
                int endIndex = findRowEnd(source, index + 1);
                if (endIndex < 0) {
                    throw new ServiceException("Word模板表格循环指令缺少#end");
                }
                List<TableRow> nested = source.subList(index + 1, endIndex);
                List<Map<String, Object>> items = contextService.resolveLoopSource(context, directive.listExpression);
                for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
                    Map<String, Object> loopContext = contextService.buildLoopContext(
                            context, directive.alias, items.get(itemIndex), itemIndex + 1
                    );
                    result.addAll(renderRows(nested, loopContext));
                }
                index = endIndex + 1;
                continue;
            }
            if (directive != null && directive.type == DirectiveType.END) {
                index++;
                continue;
            }
            if (!contextService.isInForContext(context) && contextService.hasColumnExpression(row.plainText())) {
                List<Map<String, Object>> columns = contextService.getContextColumnList(context);
                if (!columns.isEmpty()) {
                    for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                        Map<String, Object> columnContext = contextService.buildLoopContext(
                                context, "col", columns.get(columnIndex), columnIndex + 1
                        );
                        result.add(renderRow(row, columnContext));
                    }
                    index++;
                    continue;
                }
            }
            result.add(renderRow(row, context));
            index++;
        }
        return result;
    }

    private int findRowEnd(List<TableRow> rows, int fromIndex) {
        int depth = 0;
        for (int index = fromIndex; index < rows.size(); index++) {
            Directive directive = parseRowDirective(rows.get(index));
            if (directive == null) {
                continue;
            }
            if (directive.type == DirectiveType.FOR) {
                depth++;
            } else if (depth == 0) {
                return index;
            } else {
                depth--;
            }
        }
        return -1;
    }

    private Directive parseRowDirective(TableRow row) {
        Directive found = null;
        for (TableCell cell : row.cells) {
            String text = cell.plainText().trim();
            if (text.isEmpty()) {
                continue;
            }
            Directive current = parseDirective(text);
            if (current == null) {
                return null;
            }
            if (found != null && !found.sameAs(current)) {
                return null;
            }
            found = current;
        }
        return found;
    }

    private TableRow renderRow(TableRow row, Map<String, Object> context) {
        List<TableCell> cells = new ArrayList<>();
        for (TableCell cell : row.cells) {
            List<ParagraphBlock> paragraphs = new ArrayList<>();
            for (ParagraphBlock paragraph : cell.paragraphs) {
                paragraphs.add(renderParagraph(paragraph, context));
            }
            cells.add(new TableCell(copyJson(cell.style), paragraphs));
        }
        return new TableRow(copyJson(row.style), cells);
    }

    private ParagraphBlock renderParagraph(ParagraphBlock paragraph, Map<String, Object> context) {
        String text = paragraph.plainText();
        Matcher matcher = EXPRESSION.matcher(text);
        List<TextRun> result = new ArrayList<>();
        int cursor = 0;
        while (matcher.find()) {
            appendOriginalRuns(result, paragraph.runs, cursor, matcher.start());
            JSONObject replacementStyle = styleAt(paragraph.runs, matcher.start());
            appendRun(result, contextService.renderText(matcher.group(), context), replacementStyle);
            cursor = matcher.end();
        }
        appendOriginalRuns(result, paragraph.runs, cursor, text.length());
        return new ParagraphBlock(result, copyJson(paragraph.style), copyJson(paragraph.bullet));
    }

    private void appendOriginalRuns(List<TextRun> target, List<TextRun> source, int start, int end) {
        if (end <= start) {
            return;
        }
        int offset = 0;
        for (TextRun run : source) {
            int runStart = offset;
            int runEnd = offset + run.text.length();
            int from = Math.max(start, runStart);
            int to = Math.min(end, runEnd);
            if (to > from) {
                appendRun(target, run.text.substring(from - runStart, to - runStart), copyJson(run.style));
            }
            offset = runEnd;
        }
    }

    private JSONObject styleAt(List<TextRun> runs, int index) {
        int offset = 0;
        for (TextRun run : runs) {
            if (index >= offset && index < offset + run.text.length()) {
                return copyJson(run.style);
            }
            offset += run.text.length();
        }
        return runs.isEmpty() ? new JSONObject() : copyJson(runs.get(runs.size() - 1).style);
    }

    private void appendRun(List<TextRun> target, String text, JSONObject style) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (!target.isEmpty()) {
            TextRun previous = target.get(target.size() - 1);
            if (Objects.equals(previous.style.toJSONString(), style.toJSONString())) {
                previous.text += text;
                return;
            }
        }
        target.add(new TextRun(text, copyJson(style)));
    }

    private Directive parseDirective(String text) {
        String content = StrUtil.trimToEmpty(text);
        if (END_DIRECTIVE.matcher(content).matches()) {
            return Directive.end();
        }
        Matcher matcher = FOR_DIRECTIVE.matcher(content);
        return matcher.matches() ? Directive.loop(matcher.group(1), matcher.group(2)) : null;
    }

    private Map<String, HeaderFooterBody> renderHeaderFooter(Map<String, HeaderFooterBody> source,
                                                             Map<String, Object> context) {
        Map<String, HeaderFooterBody> result = new LinkedHashMap<>();
        for (Map.Entry<String, HeaderFooterBody> entry : source.entrySet()) {
            HeaderFooterBody value = entry.getValue();
            result.put(entry.getKey(), new HeaderFooterBody(
                    value.id, value.idField, renderBlocks(value.blocks, context)
            ));
        }
        return result;
    }

    private JSONObject serialize(WordDocument document) {
        JSONObject root = copyJson(document.root);
        SerializedBody serializedBody = serializeBlocks(document.blocks, true);
        root.put("body", serializedBody.body);
        root.put("tableSource", serializedBody.tableSource);
        root.put("headers", serializeHeaderFooter(document.headers));
        root.put("footers", serializeHeaderFooter(document.footers));
        root.put("drawings", new JSONObject());
        root.put("drawingsOrder", new JSONArray());
        return root;
    }

    private JSONObject serializeHeaderFooter(Map<String, HeaderFooterBody> source) {
        JSONObject result = new JSONObject();
        for (Map.Entry<String, HeaderFooterBody> entry : source.entrySet()) {
            HeaderFooterBody value = entry.getValue();
            JSONObject data = new JSONObject();
            data.put(value.idField, value.id);
            data.put("body", serializeBlocks(value.blocks, false).body);
            result.put(entry.getKey(), data);
        }
        return result;
    }

    private SerializedBody serializeBlocks(List<WordBlock> blocks, boolean allowTables) {
        BodyBuilder builder = new BodyBuilder();
        for (WordBlock block : blocks) {
            if (block instanceof ParagraphBlock paragraph) {
                builder.appendParagraph(paragraph);
            } else if (block instanceof TableBlock table) {
                if (!allowTables) {
                    throw new ServiceException("Word模板第一期暂不支持页眉页脚中的表格");
                }
                builder.appendTable(table);
            }
        }
        builder.finish();
        return new SerializedBody(builder.toBody(), builder.tableSource);
    }

    private static JSONObject copyJson(JSONObject source) {
        return source == null ? new JSONObject() : JsonUtil.parseObject(source.toJSONString());
    }

    private static final class BodyBuilder {
        final StringBuilder stream = new StringBuilder();
        final JSONArray textRuns = new JSONArray();
        final JSONArray paragraphs = new JSONArray();
        final JSONArray sectionBreaks = new JSONArray();
        final JSONArray tables = new JSONArray();
        final JSONObject tableSource = new JSONObject();

        void appendParagraph(ParagraphBlock paragraph) {
            for (TextRun run : paragraph.runs) {
                appendTextRun(run);
            }
            stream.append(PARAGRAPH);
            JSONObject paragraphData = new JSONObject();
            paragraphData.put("startIndex", stream.length() - 1);
            if (!paragraph.style.isEmpty()) {
                paragraphData.put("paragraphStyle", copyJson(paragraph.style));
            }
            if (paragraph.bullet != null && !paragraph.bullet.isEmpty()) {
                paragraphData.put("bullet", copyJson(paragraph.bullet));
            }
            paragraphs.add(paragraphData);
        }

        void appendTable(TableBlock table) {
            int startIndex = stream.length();
            String tableId = "tbl-" + IdUtil.fastSimpleUUID().substring(0, 12);
            JSONObject tableData = copyJson(table.style);
            tableData.put("tableId", tableId);
            JSONArray rowDataList = new JSONArray();

            stream.append(TABLE_START);
            for (TableRow row : table.rows) {
                stream.append(TABLE_ROW_START);
                JSONObject rowData = copyJson(row.style);
                JSONArray cellDataList = new JSONArray();
                for (TableCell cell : row.cells) {
                    stream.append(TABLE_CELL_START);
                    if (cell.paragraphs.isEmpty()) {
                        appendParagraph(new ParagraphBlock(List.of(), new JSONObject(), null));
                    } else {
                        for (ParagraphBlock paragraph : cell.paragraphs) {
                            appendParagraph(paragraph);
                        }
                    }
                    stream.append(SECTION_BREAK);
                    JSONObject section = new JSONObject();
                    section.put("startIndex", stream.length() - 1);
                    sectionBreaks.add(section);
                    stream.append(TABLE_CELL_END);
                    cellDataList.add(copyJson(cell.style));
                }
                rowData.put("tableCells", cellDataList);
                rowDataList.add(rowData);
                stream.append(TABLE_ROW_END);
            }
            stream.append(TABLE_END);
            tableData.put("tableRows", rowDataList);
            tableSource.put(tableId, tableData);

            JSONObject tableRange = new JSONObject();
            tableRange.put("startIndex", startIndex);
            tableRange.put("endIndex", stream.length());
            tableRange.put("tableId", tableId);
            tables.add(tableRange);
        }

        void appendTextRun(TextRun run) {
            if (run.text == null || run.text.isEmpty()) {
                return;
            }
            int start = stream.length();
            stream.append(run.text);
            if (!run.style.isEmpty()) {
                JSONObject runData = new JSONObject();
                runData.put("st", start);
                runData.put("ed", stream.length() - 1);
                runData.put("ts", copyJson(run.style));
                textRuns.add(runData);
            }
        }

        void finish() {
            if (stream.isEmpty() || stream.charAt(stream.length() - 1) != PARAGRAPH) {
                appendParagraph(new ParagraphBlock(List.of(), new JSONObject(), null));
            }
            stream.append(SECTION_BREAK);
            JSONObject section = new JSONObject();
            section.put("startIndex", stream.length() - 1);
            sectionBreaks.add(section);
        }

        JSONObject toBody() {
            JSONObject body = new JSONObject();
            body.put("dataStream", stream.toString());
            body.put("textRuns", textRuns);
            body.put("paragraphs", paragraphs);
            body.put("sectionBreaks", sectionBreaks);
            body.put("customBlocks", new JSONArray());
            body.put("tables", tables);
            return body;
        }
    }

    private record SerializedBody(JSONObject body, JSONObject tableSource) {
    }

    private record ParsedTable(TableBlock table, int nextIndex) {
    }

    private record StyleSpan(int start, int end, JSONObject style) {
    }

    private record ParagraphMeta(JSONObject style, JSONObject bullet) {
    }

    private enum DirectiveType {
        FOR,
        END
    }

    private record Directive(DirectiveType type, String alias, String listExpression) {
        static Directive loop(String alias, String listExpression) {
            return new Directive(DirectiveType.FOR, alias, listExpression);
        }

        static Directive end() {
            return new Directive(DirectiveType.END, null, null);
        }

        boolean sameAs(Directive other) {
            return other != null && type == other.type
                    && Objects.equals(alias, other.alias)
                    && Objects.equals(listExpression, other.listExpression);
        }
    }
}
