package org.wsitm.schemax.web;

import cn.hutool.core.lang.Dict;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.wsitm.schemax.constant.DialectEnum;
import org.wsitm.schemax.entity.core.R;
import org.wsitm.schemax.entity.core.TableDataInfo;
import org.wsitm.schemax.entity.domain.TypeMappingRule;
import org.wsitm.schemax.entity.vo.TypeMappingResult;
import org.wsitm.schemax.entity.vo.TypeMappingTestVO;
import org.wsitm.schemax.service.ITypeMappingRuleService;
import org.wsitm.schemax.utils.PageUtils;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/rdbms/type-mapping")
public class TypeMappingRuleController {
    @Autowired
    private ITypeMappingRuleService typeMappingRuleService;

    @GetMapping("/list")
    public TableDataInfo<TypeMappingRule> list(TypeMappingRule rule) {
        PageUtils.startPage();
        return TableDataInfo.getDataTable(typeMappingRuleService.selectTypeMappingRuleList(rule));
    }

    @GetMapping("/{ruleId}")
    public R<TypeMappingRule> getInfo(@PathVariable("ruleId") Integer ruleId) {
        return R.ok(typeMappingRuleService.selectTypeMappingRuleByRuleId(ruleId));
    }

    @PostMapping
    public R<Integer> add(@RequestBody TypeMappingRule rule) {
        return R.ok(typeMappingRuleService.insertTypeMappingRule(rule));
    }

    @PutMapping
    public R<Integer> edit(@RequestBody TypeMappingRule rule) {
        return R.ok(typeMappingRuleService.updateTypeMappingRule(rule));
    }

    @DeleteMapping("/{ruleIds}")
    public R<Integer> remove(@PathVariable Integer[] ruleIds) {
        return R.ok(typeMappingRuleService.deleteTypeMappingRuleByRuleIds(ruleIds));
    }

    @PostMapping("/test")
    public R<TypeMappingResult> test(@RequestBody TypeMappingTestVO testVO) {
        return R.ok(typeMappingRuleService.test(testVO));
    }

    @GetMapping("/databases")
    public R<List<Dict>> databases() {
        List<Dict> list = new ArrayList<>();
        list.add(Dict.create().set("database", "*").set("label", "通用"));
        list.addAll(DialectEnum.getList());
        return R.ok(list);
    }
}
