package org.wsitm.rdbms.web;


import org.wsitm.rdbms.entity.core.R;
import org.wsitm.rdbms.entity.vo.ConvertVO;
import org.wsitm.rdbms.entity.vo.UniverSheetVO;
import org.wsitm.rdbms.service.IConvertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * DDL转换Controller
 *
 * @author wsitm
 * @date 2025-01-27
 */
@RestController
@RequestMapping("/rdbms/convert")
public class ConvertController {

    @Autowired
    private IConvertService ddlConvertService;


    /**
     * excel 文件上传转 univer 数据格式
     */
//    @PreAuthorize("@ss.hasPermi('rdbms:convert:upload')")
    @PostMapping(value = "/upload")
    public R<Map<String, UniverSheetVO>> upload(MultipartFile file) {
        return ddlConvertService.upload(file);
    }

    /**
     * 转换DDL语句，可指定{database}类型
     */
//    @PreAuthorize("@ss.hasPermi('rdbms:convert:toDDL')")
    @PostMapping(value = "/toDDL")
    public R<Object> convertDDL(@RequestBody ConvertVO convertVO) {
        return ddlConvertService.convertDDL(convertVO);
    }


}
