package org.wsitm.schemax.service.template.word;

import org.wsitm.schemax.utils.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Word 模板的中立文档模型，隔离前端编辑器格式和 Apache POI 实现。
 */
public final class WordDocumentModel {

    public static final char PAGE_BREAK = '\f';
    public static final char LINE_BREAK = '\u000B';

    private WordDocumentModel() {
    }

    public static final class WordDocument {
        final JSONObject root;
        final List<WordBlock> blocks;
        final Map<String, HeaderFooterBody> headers;
        final Map<String, HeaderFooterBody> footers;

        WordDocument(JSONObject root, List<WordBlock> blocks,
                     Map<String, HeaderFooterBody> headers, Map<String, HeaderFooterBody> footers) {
            this.root = root;
            this.blocks = new ArrayList<>(blocks);
            this.headers = headers;
            this.footers = footers;
        }

        public JSONObject getRoot() { return root; }
        public JSONObject getDocumentStyle() { return root.getJSONObject("documentStyle"); }
        public List<WordBlock> getBlocks() { return blocks; }
        public Map<String, HeaderFooterBody> getHeaders() { return headers; }
        public Map<String, HeaderFooterBody> getFooters() { return footers; }
    }

    public interface WordBlock {
        String plainText();
    }

    public static final class ParagraphBlock implements WordBlock {
        final List<TextRun> runs;
        final JSONObject style;
        final JSONObject bullet;

        ParagraphBlock(List<TextRun> runs, JSONObject style, JSONObject bullet) {
            this.runs = new ArrayList<>(runs);
            this.style = style == null ? new JSONObject() : style;
            this.bullet = bullet;
        }

        static ParagraphBlock pageBreak() {
            return new ParagraphBlock(
                    List.of(new TextRun(String.valueOf(PAGE_BREAK), new JSONObject())),
                    new JSONObject(), null
            );
        }

        @Override
        public String plainText() {
            StringBuilder text = new StringBuilder();
            for (TextRun run : runs) {
                text.append(run.text);
            }
            return text.toString();
        }

        public List<TextRun> getRuns() { return runs; }
        public JSONObject getStyle() { return style; }
        public JSONObject getBullet() { return bullet; }
    }

    public static final class TextRun {
        String text;
        final JSONObject style;

        TextRun(String text, JSONObject style) {
            this.text = text;
            this.style = style == null ? new JSONObject() : style;
        }

        public String getText() { return text; }
        public JSONObject getStyle() { return style; }
    }

    public static final class TableBlock implements WordBlock {
        final JSONObject style;
        final List<TableRow> rows;

        TableBlock(JSONObject style, List<TableRow> rows) {
            this.style = style == null ? new JSONObject() : style;
            this.rows = new ArrayList<>(rows);
        }

        @Override
        public String plainText() {
            StringBuilder text = new StringBuilder();
            for (TableRow row : rows) {
                text.append(row.plainText());
            }
            return text.toString();
        }

        public JSONObject getStyle() { return style; }
        public List<TableRow> getRows() { return rows; }
    }

    public static final class TableRow {
        final JSONObject style;
        final List<TableCell> cells;

        TableRow(JSONObject style, List<TableCell> cells) {
            this.style = style == null ? new JSONObject() : style;
            this.cells = new ArrayList<>(cells);
        }

        String plainText() {
            StringBuilder text = new StringBuilder();
            for (TableCell cell : cells) {
                if (!text.isEmpty()) {
                    text.append('\t');
                }
                text.append(cell.plainText());
            }
            return text.toString();
        }

        public JSONObject getStyle() { return style; }
        public List<TableCell> getCells() { return cells; }
    }

    public static final class TableCell {
        final JSONObject style;
        final List<ParagraphBlock> paragraphs;

        TableCell(JSONObject style, List<ParagraphBlock> paragraphs) {
            this.style = style == null ? new JSONObject() : style;
            this.paragraphs = new ArrayList<>(paragraphs);
        }

        String plainText() {
            StringBuilder text = new StringBuilder();
            for (ParagraphBlock paragraph : paragraphs) {
                if (!text.isEmpty()) {
                    text.append('\n');
                }
                text.append(paragraph.plainText());
            }
            return text.toString();
        }

        public JSONObject getStyle() { return style; }
        public List<ParagraphBlock> getParagraphs() { return paragraphs; }
    }

    public static final class HeaderFooterBody {
        final String id;
        final String idField;
        final List<WordBlock> blocks;

        HeaderFooterBody(String id, String idField, List<WordBlock> blocks) {
            this.id = id;
            this.idField = idField;
            this.blocks = new ArrayList<>(blocks);
        }

        public String getId() { return id; }
        public String getIdField() { return idField; }
        public List<WordBlock> getBlocks() { return blocks; }
    }
}
