package org.wsitm.schemax.web;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.wsitm.schemax.constant.RdbmsConstants;
import org.wsitm.schemax.entity.core.R;
import org.wsitm.schemax.entity.core.TableDataInfo;
import org.wsitm.schemax.entity.domain.JdbcInfo;
import org.wsitm.schemax.entity.vo.JdbcInfoVo;
import org.wsitm.schemax.exception.ServiceException;
import org.wsitm.schemax.service.IJdbcInfoService;
import org.wsitm.schemax.utils.PageUtils;

import java.io.File;
import java.util.Objects;

/**
 * 驱动管理Controller
 *
 * @author wsitm
 * @date 2025-01-11
 */
@RestController
@RequestMapping("/rdbms/jdbc")
public class JdbcInfoController {
    @Autowired
    private IJdbcInfoService jdbcInfoService;

    /**
     * 查询驱动管理列表
     */
    @GetMapping("/list")
    public TableDataInfo<JdbcInfoVo> list(String jdbcName) {
        PageUtils.startPage();
        return TableDataInfo.getDataTable(jdbcInfoService.selectJdbcInfoList(jdbcName));
    }

    /**
     * 导出驱动管理列表
     */
//    @PostMapping("/export")
//    public void export(HttpServletResponse response, JdbcInfo jdbcInfo) {
//        List<JdbcInfoVo> list = jdbcInfoService.selectJdbcInfoList(jdbcInfo);
//        ExcelUtil<JdbcInfoVo> util = new ExcelUtil<JdbcInfoVo>(JdbcInfoVo.class);
//        util.exportExcel(response, list, "驱动管理数据");
//    }

    /**
     * 获取驱动管理详细信息
     */
    @GetMapping(value = "/{jdbcId}")
    public R<JdbcInfo> getInfo(@PathVariable("jdbcId") Integer jdbcId) {
        return R.ok(jdbcInfoService.selectJdbcInfoByJdbcId(jdbcId));
    }

    /**
     * 新增驱动管理
     */
    @PostMapping
    public R<Integer> add(@RequestBody JdbcInfo jdbcInfo) {
        return R.ok(jdbcInfoService.insertJdbcInfo(jdbcInfo));
    }

    /**
     * 修改驱动管理
     */
    @PutMapping
    public R<Integer> edit(@RequestBody JdbcInfo jdbcInfo) {
        return R.ok(jdbcInfoService.updateJdbcInfo(jdbcInfo));
    }

    /**
     * 删除驱动管理
     */
    @DeleteMapping("/{jdbcIds}")
    public R<Integer> remove(@PathVariable Integer[] jdbcIds) {
        return R.ok(jdbcInfoService.deleteJdbcInfoByJdbcIds(jdbcIds));
    }


    /**
     * 安装或卸载驱动
     *
     * @param jdbcId 驱动ID
     * @param action load/unload 安装/卸载
     */
    @PostMapping("/load/{jdbcId}/{action}")
    public R<Integer> load(@PathVariable Integer jdbcId, @PathVariable String action) {
        return R.ok(jdbcInfoService.load(jdbcId, action));
    }


    /**
     * 通用上传请求（单个）
     */
    @PostMapping("/upload")
    public R<String> uploadFile(MultipartFile file) throws Exception {
        try {
            // 上传文件路径
            String fileName = Objects.requireNonNull(file.getOriginalFilename());
            // 上传并返回新文件名称
            File newFile = new File(RdbmsConstants.LIB_PATH, fileName);
            if (newFile.exists()) {
                throw new ServiceException("驱动文件已经存在！");
            }
            file.transferTo(new File(newFile.getAbsolutePath()));
            return R.ok(fileName);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

}
