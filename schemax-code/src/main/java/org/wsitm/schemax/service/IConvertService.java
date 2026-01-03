package org.wsitm.schemax.service;

import org.wsitm.schemax.entity.core.R;
import org.wsitm.schemax.entity.vo.ConvertVO;
import org.wsitm.schemax.entity.vo.UniverWorkbookVO;
import org.springframework.web.multipart.MultipartFile;


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
    R<UniverWorkbookVO> upload(MultipartFile file);

    /**
     * 转换DDL语句，可指定{database}类型
     *
     * @param convertVO DDL语句信息
     * @return 结果
     */
    R<Object> convertDDL(ConvertVO convertVO);

}
