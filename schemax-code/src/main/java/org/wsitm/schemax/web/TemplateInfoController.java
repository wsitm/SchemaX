package org.wsitm.schemax.web;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Dict;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.wsitm.schemax.entity.core.R;
import org.wsitm.schemax.entity.core.TableDataInfo;
import org.wsitm.schemax.entity.domain.TemplateInfo;
import org.wsitm.schemax.entity.vo.TemplateInfoVO;
import org.wsitm.schemax.exception.ServiceException;
import org.wsitm.schemax.service.template.word.WordTemplateService;
import org.wsitm.schemax.utils.json.JSONObject;
import org.wsitm.schemax.service.ITemplateInfoService;
import org.wsitm.schemax.utils.PageUtils;

import java.util.List;

/**
 * 模板管理Controller
 *
 * @author wsitm
 * @date 2025-12-27
 */
@RestController
@RequestMapping("/rdbms/template")
public class TemplateInfoController {

    @Autowired
    private ITemplateInfoService templateInfoService;
    @Autowired
    private WordTemplateService wordTemplateService;

    /**
     * 查询模板管理列表
     */
    @GetMapping("/list")
    public TableDataInfo<TemplateInfoVO> list(TemplateInfo templateInfo) {
        PageUtils.startPage();
        return TableDataInfo.getDataTable(templateInfoService.selectTemplateInfoList(templateInfo));
    }

    /**
     * 获取模板管理详细信息
     */
    @GetMapping(value = "/{tpId}")
    public R<TemplateInfoVO> getInfo(@PathVariable("tpId") Integer tpId) {
        return R.ok(templateInfoService.selectTemplateInfoByTpId(tpId));
    }

    /**
     * 获取TinyMCE可编辑的Word模板内容，旧Univer快照会在内存中转换。
     */
    @GetMapping(value = "/{tpId}/word-editor")
    public R<JSONObject> getWordEditorContent(@PathVariable("tpId") Integer tpId) {
        TemplateInfoVO template = templateInfoService.selectTemplateInfoByTpId(tpId);
        if (template == null) {
            throw new ServiceException("Word模板不存在");
        }
        if (template.getTpType() != 2) {
            throw new ServiceException("所选模板不是Word模板");
        }
        return R.ok(wordTemplateService.toEditorContent(template.getTpContent()));
    }

    /**
     * 新增模板管理
     */
    @PostMapping
    public R<Integer> add(@RequestBody TemplateInfo templateInfo) {
        return R.ok(templateInfoService.insertTemplateInfo(templateInfo));
    }

    /**
     * 修改模板管理
     */
    @PutMapping
    public R<Integer> edit(@RequestBody TemplateInfo templateInfo) {
        return R.ok(templateInfoService.updateTemplateInfo(templateInfo));
    }

    /**
     * 删除模板管理
     */
    @DeleteMapping("/{tpIds}")
    public R<Integer> remove(@PathVariable Integer[] tpIds) {
        return R.ok(templateInfoService.deleteTemplateInfoByTpIds(tpIds));
    }

    /**
     * 模板类型
     */
    @GetMapping("/types")
    public R<List<Dict>> types() {
        return R.ok(ListUtil.toList(
                Dict.create().set("value", 1).set("label", "excel"),
                Dict.create().set("value", 2).set("label", "word"),
                Dict.create().set("value", 3).set("label", "markdown")
        ));
    }
}

