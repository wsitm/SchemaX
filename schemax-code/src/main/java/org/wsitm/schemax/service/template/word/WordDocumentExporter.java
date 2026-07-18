package org.wsitm.schemax.service.template.word;

import java.io.File;
import java.io.IOException;

/**
 * 将逻辑 Word 文档输出为具体文件格式，隔离第三方文档库。
 */
public interface WordDocumentExporter {

    void export(File target, WordTemplateService.WordDocument document) throws IOException;
}
