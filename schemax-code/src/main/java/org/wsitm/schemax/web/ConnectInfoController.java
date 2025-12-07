package org.wsitm.schemax.web;

import cn.hutool.core.lang.Dict;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.wsitm.schemax.constant.DialectEnum;
import org.wsitm.schemax.entity.core.R;
import org.wsitm.schemax.entity.core.TableDataInfo;
import org.wsitm.schemax.entity.domain.ConnectInfo;
import org.wsitm.schemax.entity.vo.ConnectInfoVO;
import org.wsitm.schemax.entity.vo.TableVO;
import org.wsitm.schemax.service.IConnectInfoService;
import org.wsitm.schemax.utils.PageUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 连接配置Controller
 *
 * @author wsitm
 * @date 2025-01-11
 */
@RestController
@RequestMapping("/rdbms/connect")
public class ConnectInfoController {
    @Autowired
    private IConnectInfoService connectInfoService;

    /**
     * 查询连接配置列表
     */
    @GetMapping("/list")
    public TableDataInfo<ConnectInfoVO> list(String connectName, String jdbcId) {
        PageUtils.startPage();
        return TableDataInfo.getDataTable(connectInfoService.selectConnectInfoList(connectName, jdbcId));
    }

    /**
     * 导出连接配置列表
     */
//    @PostMapping("/export")
//    public void export(HttpServletResponse response, ConnectInfo connectInfo) {
//        List<ConnectInfoVO> list = connectInfoService.selectConnectInfoList(connectInfo);
//        ExcelUtil<ConnectInfoVO> util = new ExcelUtil<ConnectInfoVO>(ConnectInfoVO.class);
//        util.exportExcel(response, list, "连接配置数据");
//    }

    /**
     * 获取连接配置详细信息
     */
    @GetMapping(value = "/{connectId}")
    public R<ConnectInfoVO> getInfo(@PathVariable("connectId") String connectId) {
        return R.ok(connectInfoService.selectConnectInfoByConnectId(connectId));
    }

    /**
     * 新增连接配置
     */
    @PostMapping
    public R<Integer> add(@RequestBody ConnectInfo connectInfo) {
        return R.ok(connectInfoService.insertConnectInfo(connectInfo));
    }

    /**
     * 修改连接配置
     */
    @PutMapping
    public R<Integer> edit(@RequestBody ConnectInfo connectInfo) {
        return R.ok(connectInfoService.updateConnectInfo(connectInfo));
    }

    /**
     * 删除连接配置
     */
    @DeleteMapping("/{connectIds}")
    public R<Integer> remove(@PathVariable String[] connectIds) {
        return R.ok(connectInfoService.deleteConnectInfoByConnectIds(connectIds));
    }

    /**
     * 查询连接配置列表
     */
    @PostMapping("/check")
    public R<Boolean> check(@RequestBody ConnectInfo connectInfo) {
        return R.ok(connectInfoService.checkConnectInfo(connectInfo));
    }

    /**
     * 获取连接所有表格详细信息
     */
    @GetMapping(value = "/tables/{connectId}")
    public R<List<TableVO>> getTableInfo(@PathVariable("connectId") String connectId) {
        return R.ok(connectInfoService.getTableInfo(connectId));
    }


    /**
     * 获取所有的方言列表
     */
    @GetMapping("/dialects")
    public R<List<Dict>> dialects() {
        return R.ok(DialectEnum.getList());
    }

    /**
     * 刷新缓存
     */
    @PostMapping("/flush/{connectId}")
    public R<Boolean> flush(@PathVariable("connectId") String connectId) {
        return R.ok(connectInfoService.flushCahce(connectId));
    }


    /**
     * 获取连接所有表DDL语句
     */
    @GetMapping(value = "/ddl/{connectId}")
    public R<Map<String, String[]>> getDDLInfo(@PathVariable("connectId") String connectId, String database) {
        return R.ok(connectInfoService.genTableDDL(connectId, database));
    }

    /**
     * 导出表结构信息
     *
     * @param connectId 连接ID
     * @param skipStrs  通配符匹配，包含，? 任何单个，* 任何多个，! 剔除
     */
    @PostMapping("/export/{connectId}/tableInfo")
    public void exportTableInfo(HttpServletResponse response,
                                @PathVariable("connectId") String connectId, String[] skipStrs) throws IOException {
        connectInfoService.exportTableInfo(response, connectId, skipStrs);
    }
}
