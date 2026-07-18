package org.wsitm.schemax.web;


import org.wsitm.schemax.entity.core.R;
import org.wsitm.schemax.entity.vo.DdlCheckRequestVO;
import org.wsitm.schemax.entity.vo.DdlCheckResultVO;
import org.wsitm.schemax.entity.vo.ConvertVO;
import org.wsitm.schemax.entity.vo.UniverWorkbookVO;
import org.wsitm.schemax.service.IConvertService;
import org.wsitm.schemax.service.IDdlDiagnosisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


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

    @Autowired
    private IDdlDiagnosisService ddlDiagnosisService;


    /**
     * excel 文件上传转 univer 数据格式
     */
    @PostMapping(value = "/upload")
    public R<UniverWorkbookVO> upload(MultipartFile file) {
        return ddlConvertService.upload(file);
    }

    /**
     * 转换DDL语句，可指定{database}类型
     */
    @PostMapping(value = "/toDDL")
    public R<Object> convertDDL(@RequestBody ConvertVO convertVO) {
        return ddlConvertService.convertDDL(convertVO);
    }

    /**
     * 预检DDL语句并返回问题位置、原因、建议及按表统计。
     */
    @PostMapping(value = "/precheck")
    public R<DdlCheckResultVO> precheck(@RequestBody DdlCheckRequestVO request) {
        return R.ok(ddlDiagnosisService.precheck(request));
    }


}
