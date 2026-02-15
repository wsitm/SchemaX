package org.wsitm.schemax.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.meta.JdbcType;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.wsitm.schemax.constant.RdbmsConstants;
import org.wsitm.schemax.entity.domain.ConnectInfo;
import org.wsitm.schemax.entity.vo.ColumnVO;
import org.wsitm.schemax.entity.vo.ConnectInfoVO;
import org.wsitm.schemax.entity.vo.ConnectTemplateLinkVO;
import org.wsitm.schemax.entity.vo.TableVO;
import org.wsitm.schemax.exception.ServiceException;
import org.wsitm.schemax.mapper.ConnectInfoMapper;
import org.wsitm.schemax.mapper.ConnectTemplateLinkMapper;
import org.wsitm.schemax.mapper.TableMetaMapper;
import org.wsitm.schemax.metainfo.MetaInfoTask;
import org.wsitm.schemax.metainfo.MetaInfoUtil;
import org.wsitm.schemax.service.IConnectInfoService;
import org.wsitm.schemax.utils.CommonUtil;
import org.wsitm.schemax.utils.DDLUtil;
import org.wsitm.schemax.utils.PoiUtil;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * 连接配置 Service
 */
@Service
public class ConnectInfoServiceImpl implements IConnectInfoService {
    private static final Logger log = LoggerFactory.getLogger(ConnectInfoServiceImpl.class);

    @Autowired
    private ConnectInfoMapper connectInfoMapper;
    @Autowired
    private TableMetaMapper tableMetaMapper;
    @Autowired
    private ConnectTemplateLinkMapper connectTemplateLinkMapper;
    @Autowired
    private TemplateRenderService templateRenderService;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    private final Map<Integer, Future<?>> taskMap = new ConcurrentHashMap<>();

    @Override
    public ConnectInfoVO selectConnectInfoByConnectId(Integer connectId) {
        return connectInfoMapper.selectConnectInfoByConnectId(connectId);
    }

    @Override
    public List<ConnectInfoVO> selectConnectInfoList(ConnectInfo connectInfo) {
        return connectInfoMapper.selectConnectInfoList(connectInfo);
    }

    @Override
    public int insertConnectInfo(ConnectInfo connectInfo) {
        checkWildcard(connectInfo);
        connectInfo.setCreateTime(LocalDateTime.now());
        int insert = connectInfoMapper.insertConnectInfo(connectInfo);
        flushCahce(connectInfo.getConnectId());
        return insert;
    }

    @Override
    public int updateConnectInfo(ConnectInfo connectInfo) {
        checkWildcard(connectInfo);
        int update = connectInfoMapper.updateConnectInfo(connectInfo);
        flushCahce(connectInfo.getConnectId());
        return update;
    }

    private void checkWildcard(ConnectInfo connectInfo) {
        if (Integer.valueOf(2).equals(connectInfo.getFilterType()) && StrUtil.isNotEmpty(connectInfo.getWildcard())) {
            try {
                Pattern.compile(connectInfo.getWildcard());
            } catch (Exception e) {
                throw new ServiceException("正则表达式错误，" + e.getMessage());
            }
        }
    }

    @Override
    public int deleteConnectInfoByConnectIds(Integer[] connectIds) {
        if (connectIds != null) {
            for (Integer connectId : connectIds) {
                connectTemplateLinkMapper.deleteByConnectId(connectId);
            }
        }
        return connectInfoMapper.deleteConnectInfoByConnectIds(connectIds);
    }

    @Override
    public int deleteConnectInfoByConnectId(Integer connectId) {
        connectTemplateLinkMapper.deleteByConnectId(connectId);
        return connectInfoMapper.deleteConnectInfoByConnectId(connectId);
    }

    @Override
    public boolean checkConnectInfo(ConnectInfo connectInfo) {
        Connection conn = null;
        try {
            String url = connectInfo.getJdbcUrl();
            String username = connectInfo.getUsername();
            String password = connectInfo.getPassword();
            conn = DriverManager.getConnection(url, username, password);
            return true;
        } catch (Exception e) {
            log.error("测试连接异常", e);
            throw new ServiceException(e.getMessage());
        } finally {
            IoUtil.close(conn);
        }
    }

    @Override
    public List<TableVO> getTableInfo(Integer connectId) {
        return tableMetaMapper.findByConnectId(connectId);
    }

    @Override
    public List<ConnectTemplateLinkVO> selectConnectTemplateList(Integer connectId) {
        return connectTemplateLinkMapper.selectByConnectId(connectId);
    }

    @Override
    public int saveConnectTemplate(Integer connectId, List<Integer> tpIdList, Integer defTpId) {
        connectTemplateLinkMapper.deleteByConnectId(connectId);
        if (tpIdList == null || tpIdList.isEmpty()) {
            return 0;
        }
        if (defTpId != null && !tpIdList.contains(defTpId)) {
            throw new ServiceException("默认模板必须在已关联模板中");
        }
        return connectTemplateLinkMapper.insertBatch(connectId, tpIdList, defTpId);
    }

    @Override
    public synchronized boolean flushCahce(Integer connectId) {
        Future<?> future = taskMap.get(connectId);
        if (future != null) {
            future.cancel(true);
            taskMap.remove(connectId);
        }

        MetaInfoTask metaInfoTask = new MetaInfoTask(connectId);
        future = threadPoolExecutor.submit(metaInfoTask);
        taskMap.put(connectId, future);

        return true;
    }

    @Override
    public Map<String, String[]> genTableDDL(Integer connectId, String database) {
        List<TableVO> tableVOList = tableMetaMapper.findByConnectId(connectId);
        return DDLUtil.genDDL(tableVOList, database);
    }

    public static final String[] ARR_COL = new String[]{"序号", "字段", "类型", "长度", "小数", "可空", "自增", "主键", "默认", "注释"};

    @Override
    public void exportTableInfo(HttpServletResponse response, Integer connectId,
                                Integer filterType, String wildcard, Integer tpId) throws IOException {
        List<TableVO> tableVOList = listFilteredTable(connectId, filterType, wildcard);
        ConnectTemplateLinkVO templateLinkVO = selectExportTemplate(connectId, tpId);
        if (templateLinkVO == null) {
            exportDefaultTableInfo(response, connectId, tableVOList);
            return;
        }

        if (templateLinkVO.getTpType() == 1) {
            exportTemplateExcel(response, connectId, tableVOList, templateLinkVO);
            return;
        }

        if (templateLinkVO.getTpType() == 3) {
            exportTemplateMarkdown(response, connectId, tableVOList, templateLinkVO);
            return;
        }

        throw new ServiceException("当前模板类型暂不支持导出");
    }

    private List<TableVO> listFilteredTable(Integer connectId, Integer filterType, String wildcard) {
        ConnectInfo connectInfo = new ConnectInfo();
        connectInfo.setFilterType(filterType);
        connectInfo.setWildcard(wildcard);
        Function<String, Boolean> filter = MetaInfoUtil.createTableNameChecker(connectInfo);
        List<TableVO> tableVOList = tableMetaMapper.findByConnectId(connectId);
        return tableVOList.stream().filter(item -> filter.apply(item.getTableName())).toList();
    }

    private ConnectTemplateLinkVO selectExportTemplate(Integer connectId, Integer tpId) {
        if (tpId != null) {
            ConnectTemplateLinkVO bindTemplate = connectTemplateLinkMapper.selectByConnectIdAndTpId(connectId, tpId);
            if (bindTemplate != null) {
                return bindTemplate;
            }
        }
        return connectTemplateLinkMapper.selectDefOrFirstByConnectId(connectId);
    }

    private void exportTemplateExcel(HttpServletResponse response,
                                     Integer connectId,
                                     List<TableVO> tableVOList,
                                     ConnectTemplateLinkVO templateLinkVO) throws IOException {
        JSONObject workbookData = templateRenderService.renderWorkbook(tableVOList, templateLinkVO.getTpContent());
        if (workbookData == null) {
            exportDefaultTableInfo(response, connectId, tableVOList);
            return;
        }

        String path = RdbmsConstants.FILE_PATH + File.separator + "connect/";
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = "表格信息-" + connectId + "-" + DateUtil.format(new Date(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
        File file = new File(path, fileName);
        templateRenderService.writeWorkbookToExcel(file, workbookData);
        CommonUtil.renderFile(response, file);
    }

    private void exportTemplateMarkdown(HttpServletResponse response,
                                        Integer connectId,
                                        List<TableVO> tableVOList,
                                        ConnectTemplateLinkVO templateLinkVO) throws IOException {
        String path = RdbmsConstants.FILE_PATH + File.separator + "connect/";
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = "表格信息-" + connectId + "-" + DateUtil.format(new Date(), DatePattern.PURE_DATETIME_PATTERN) + ".md";
        File file = new File(path, fileName);
        String content = templateRenderService.renderMarkdown(tableVOList, templateLinkVO.getTpContent());
        FileUtil.writeUtf8String(content, file);
        CommonUtil.renderFile(response, file);
    }

    private void exportDefaultTableInfo(HttpServletResponse response, Integer connectId,
                                        List<TableVO> tableVOList) throws IOException {
        String fileName = "表格信息-" + connectId + "-" + DateUtil.format(new Date(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
        String path = RdbmsConstants.FILE_PATH + File.separator + "connect/";
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(path, fileName);
        try (ExcelWriter excelWriter = ExcelUtil.getBigWriter(file)) {
            Font font = excelWriter.getWorkbook().createFont();
            font.setBold(true);

            int tableNum = 1;
            for (TableVO tableVO : tableVOList) {
                String tableName = StrUtil.nullToEmpty(tableVO.getTableName());
                String title = (tableNum++) + ". " + tableName.toLowerCase() + ", " + StrUtil.nullToEmpty(tableVO.getComment());
                CellStyle cellStyle = PoiUtil.createDefaultCellStyle(excelWriter.getWorkbook());
                cellStyle.setAlignment(HorizontalAlignment.LEFT);
                cellStyle.setFont(font);

                excelWriter.merge(
                        excelWriter.getCurrentRow(),
                        excelWriter.getCurrentRow(),
                        0,
                        ARR_COL.length - 1,
                        title,
                        cellStyle
                );
                excelWriter.passCurrentRow();

                excelWriter.writeHeadRow(Arrays.asList(ARR_COL));
                CellStyle headCellStyle = excelWriter.getHeadCellStyle();
                headCellStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
                headCellStyle.setFont(font);
                headCellStyle.setAlignment(HorizontalAlignment.LEFT);

                List<ColumnVO> columns = tableVO.getColumnList();
                if (columns == null) {
                    columns = List.of();
                }
                int num = 1;
                for (ColumnVO columnVO : columns) {
                    String size = StrUtil.toString(columnVO.getSize());
                    if (columnVO.getType() == JdbcType.FLOAT.typeCode
                            || columnVO.getType() == JdbcType.CLOB.typeCode
                            || StrUtil.equalsAnyIgnoreCase(columnVO.getTypeName(), "text", "longtext")) {
                        size = "";
                    }

                    String defVal = columnVO.getColumnDef();
                    if (StrUtil.isNotEmpty(defVal) && !StrUtil.contains(defVal, "nextval")) {
                        if (StrUtil.contains(defVal, "::")) {
                            defVal = defVal.split("::")[0];
                        }
                        if (StrUtil.startWith(defVal, "'")) {
                            defVal = StrUtil.subAfter(defVal, "'", false);
                        }
                        if (StrUtil.endWith(defVal, "'")) {
                            defVal = StrUtil.subBefore(defVal, "'", true);
                        }
                    }

                    excelWriter.writeRow(
                            ListUtil.toList(
                                    num++,
                                    StrUtil.nullToEmpty(columnVO.getName()).toLowerCase(),
                                    StrUtil.nullToEmpty(columnVO.getTypeName()).toLowerCase(),
                                    size,
                                    columnVO.getDigit(),
                                    columnVO.isNullable() ? "YES" : "NO",
                                    columnVO.isAutoIncrement() ? "YES" : null,
                                    columnVO.isPk() ? "YES" : null,
                                    defVal,
                                    columnVO.getComment()
                            )
                    );
                }
                excelWriter.passCurrentRow();
            }
            excelWriter.setColumnWidth(1, 25);
            excelWriter.setColumnWidth(2, 15);
            excelWriter.setColumnWidth(9, 50);
            excelWriter.getCellStyle().setAlignment(HorizontalAlignment.LEFT);
            excelWriter.flush();

            CommonUtil.renderFile(response, file);
        }
    }
}
