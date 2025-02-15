package org.wsitm.rdbms.service;

import org.wsitm.rdbms.entity.core.R;
import org.wsitm.rdbms.entity.vo.ConvertVO;
import org.wsitm.rdbms.entity.vo.UniverSheetVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * DDL转换Service接口
 *
 * @author wsitm
 * @date 2025-01-27
 */
public interface IConvertService {

    /**
     * excel文件上传
     *
     * @param file 文件
     * @return univer数据
     */
    R<Map<String, UniverSheetVO>> upload(MultipartFile file);

    /**
     * 转换DDL语句，可指定{database}类型
     *
     * @param convertVO DDL语句信息
     * @return 结果
     */
    R<Object> convertDDL(ConvertVO convertVO);

}
