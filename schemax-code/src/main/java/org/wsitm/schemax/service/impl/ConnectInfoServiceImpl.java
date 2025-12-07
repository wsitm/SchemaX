package org.wsitm.schemax.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.meta.JdbcType;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
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
import org.wsitm.schemax.entity.vo.TableVO;
import org.wsitm.schemax.exception.ServiceException;
import org.wsitm.schemax.mapper.ConnectInfoMapper;
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
 * 连接配置Service业务层处理
 *
 * @author wsitm
 * @date 2025-01-11
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@Service
public class ConnectInfoServiceImpl implements IConnectInfoService {
    private static final Logger log = LoggerFactory.getLogger(ConnectInfoServiceImpl.class);

    @Autowired
    private ConnectInfoMapper connectInfoMapper;
    @Autowired
    private TableMetaMapper tableMetaMapper;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;
    // 存储任务ID与Future的映射关系（线程安全）
    private final Map<Integer, Future<?>> taskMap = new ConcurrentHashMap<>();


    /**
     * 查询连接配置
     *
     * @param connectId 连接配置主键
     * @return 连接配置
     */
    @Override
    public ConnectInfoVO selectConnectInfoByConnectId(Integer connectId) {
        ConnectInfoVO connectInfoVO = connectInfoMapper.selectConnectInfoByConnectId(connectId);
//        connectInfoVO.setCacheType(CacheUtil.cacheType(connectInfoVO.getConnectId()));
        return connectInfoVO;
    }

    /**
     * 查询连接配置列表
     *
     * @return 连接配置
     */
    @Override
    public List<ConnectInfoVO> selectConnectInfoList(ConnectInfo connectInfo) {
        List<ConnectInfoVO> connectInfoVOList = connectInfoMapper.selectConnectInfoList(connectInfo);
//        for (ConnectInfoVO connectInfoVo : connectInfoVOList) {
//            connectInfoVo.setCacheType(CacheUtil.cacheType(connectInfoVo.getConnectId()));
//        }
        return connectInfoVOList;
    }

    /**
     * 新增连接配置
     *
     * @param connectInfo 连接配置
     * @return 结果
     */
    @Override
    public int insertConnectInfo(ConnectInfo connectInfo) {
        checkWildcard(connectInfo);

//        connectInfo.setConnectId(IdUtil.getSnowflakeNextIdStr());
        connectInfo.setCreateTime(LocalDateTime.now());
        int insert = connectInfoMapper.insertConnectInfo(connectInfo);

        flushCahce(connectInfo.getConnectId());

        return insert;
    }

    /**
     * 修改连接配置
     *
     * @param connectInfo 连接配置
     * @return 结果
     */
    @Override
    public int updateConnectInfo(ConnectInfo connectInfo) {
        checkWildcard(connectInfo);
        int update = connectInfoMapper.updateConnectInfo(connectInfo);

        flushCahce(connectInfo.getConnectId());

        return update;
    }

    /**
     * 检查通配符表达式的有效性
     * 当过滤类型为2且通配符不为空时，验证通配符是否为有效的正则表达式
     *
     * @param connectInfo 连接信息对象，包含过滤类型和通配符信息
     * @throws ServiceException 当正则表达式格式错误时抛出业务异常
     */
    private void checkWildcard(ConnectInfo connectInfo) {
        // 当过滤类型为2且通配符不为空时，验证正则表达式有效性
        if (connectInfo.getFilterType() == 2 && StrUtil.isNotEmpty(connectInfo.getWildcard())) {
            try {
                Pattern.compile(connectInfo.getWildcard());
            } catch (Exception e) {
                throw new ServiceException("正则表达式错误，" + e.getMessage());
            }
        }
    }


    /**
     * 批量删除连接配置
     *
     * @param connectIds 需要删除的连接配置主键
     * @return 结果
     */
    @Override
    public int deleteConnectInfoByConnectIds(Integer[] connectIds) {
//        CacheUtil.removeConnectInfoByIds(connectIds);
        return connectInfoMapper.deleteConnectInfoByConnectIds(connectIds);
    }

    /**
     * 删除连接配置信息
     *
     * @param connectId 连接配置主键
     * @return 结果
     */
    @Override
    public int deleteConnectInfoByConnectId(Integer connectId) {
        return connectInfoMapper.deleteConnectInfoByConnectId(connectId);
    }

    /**
     * @param connectInfo 连接信息
     * @return 布尔
     */
    @Override
    public boolean checkConnectInfo(ConnectInfo connectInfo) {
//        JdbcInfo jdbcInfo = jdbcInfoMapper.selectJdbcInfoByJdbcId(connectInfo.getJdbcId());
        Connection conn = null;
        try {
            String url = connectInfo.getJdbcUrl();
            String username = connectInfo.getUsername();
            String password = connectInfo.getPassword();

//            Class.forName(jdbcInfo.getDriverClass());
//            DriverManager.setLoginTimeout(1);

            conn = DriverManager.getConnection(url, username, password);
            return true;
        } catch (Exception e) {
            log.error("测试连接异常", e);
            throw new ServiceException(e.getMessage());
        } finally {
            IoUtil.close(conn);
        }
    }

    /**
     * 获取连接所有表格详细信息
     *
     * @param connectId 连接ID
     * @return 表格信息
     */
    @Override
    public List<TableVO> getTableInfo(Integer connectId) {
        return tableMetaMapper.findByConnectId(connectId);
    }

    /**
     * 刷新缓存
     *
     * @param connectId 连接ID
     * @return 布尔
     */
    @Override
    public synchronized boolean flushCahce(Integer connectId) {
        Future<?> future = taskMap.get(connectId);
        if (future != null) {
            future.cancel(true);
            // 无论是否成功都移除
            taskMap.remove(connectId);
        }

        MetaInfoTask metaInfoTask = new MetaInfoTask(connectId);
        future = threadPoolExecutor.submit(metaInfoTask);
        taskMap.put(connectId, future);

        return true;
    }

    /**
     * 生成表格DDL
     *
     * @param connectId 连接ID
     * @param database  数据库类型
     * @return DDL
     */
    public Map<String, String[]> genTableDDL(Integer connectId, String database) {
        List<TableVO> tableVOList = tableMetaMapper.findByConnectId(connectId);
        return DDLUtil.genDDL(tableVOList, database);
    }


    public static final String[] ARR_COL = new String[]{"序号", "字段", "类型", "长度", "小数", "为空", "自增", "主键", "默认", "注释"};

    /**
     * 导出表格信息到Excel
     *
     * @param response   HTTP响应对象，用于输出导出的文件
     * @param connectId  数据库连接标识符
     * @param filterType 过滤类型，1、通配符匹配，2、正则匹配
     * @param wildcard   通配符/正则匹配
     * @throws IOException 当文件写入或读取发生错误时抛出
     */
    public void exportTableInfo(HttpServletResponse response, Integer connectId,
                                Integer filterType, String wildcard) throws IOException {
        // 生成文件名，包含连接标识符和当前时间
        String fileName = "表格信息-" + connectId + "-" + DateUtil.format(new Date(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
        // 定义文件保存路径
        String path = RdbmsConstants.FILE_PATH + File.separator + "connect/";
        // 创建目录对象
        File dir = new File(path);
        // 判断路径是否存在
        if (!dir.exists()) {
            // 如果不存在则创建目录
            dir.mkdirs();
        }

        ConnectInfo connectInfo = new ConnectInfo();
        connectInfo.setFilterType(filterType);
        connectInfo.setWildcard(wildcard);
        Function<String, Boolean> filter = MetaInfoUtil.createTableNameChecker(connectInfo);

        // 创建文件对象
        File file = new File(path, fileName);
        // 使用try-with-resources确保ExcelWriter在使用后能被正确关闭
        try (ExcelWriter excelWriter = ExcelUtil.getBigWriter(file)) {
            // 创建字体对象并设置为粗体
            Font font = excelWriter.getWorkbook().createFont();
            font.setBold(true);

            // 初始化表格编号
            int tableNum = 1;
            // 获取缓存中的表元数据列表
            List<TableVO> tableVOList = tableMetaMapper.findByConnectId(connectId);
            // 遍历表元数据列表
            for (TableVO tableVO : tableVOList) {
                // 获取表名
                String tableName = tableVO.getTableName();
                // 检查表名是否匹配忽略关键字，不匹配则跳过
                if (!filter.apply(tableName)) {
                    continue;
                }

                // 构造标题字符串
                String title = (tableNum++) + ". " + tableName.toLowerCase() + ", " + tableVO.getComment();
                // 创建单元格样式对象
                CellStyle cellStyle = PoiUtil.createDefaultCellStyle(excelWriter.getWorkbook());
                cellStyle.setAlignment(HorizontalAlignment.LEFT);
                cellStyle.setFont(font);

                // 合并单元格并写入标题
                excelWriter.merge(
                        excelWriter.getCurrentRow(),
                        excelWriter.getCurrentRow(),
                        0,
                        ARR_COL.length - 1,
                        title,
                        cellStyle
                );
                // 跳过当前行
                excelWriter.passCurrentRow();

                // 写入表头行
                excelWriter.writeHeadRow(Arrays.asList(ARR_COL));
                // 获取表头单元格样式并设置样式
                CellStyle headCellStyle = excelWriter.getHeadCellStyle();
                headCellStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
                headCellStyle.setFont(font);
                headCellStyle.setAlignment(HorizontalAlignment.LEFT);

                // 获取列信息列表
                List<ColumnVO> columns = tableVO.getColumnList();
                // 初始化列编号
                int num = 1;
                // 遍历列信息列表
                for (ColumnVO columnVO : columns) {
                    // 处理列大小，某些数据类型不显示大小
                    String size = StrUtil.toString(columnVO.getSize());
                    if (columnVO.getType() == JdbcType.FLOAT.typeCode
                            || columnVO.getType() == JdbcType.CLOB.typeCode
                            || StrUtil.equalsAnyIgnoreCase(columnVO.getTypeName(), "text", "longtext")) {
                        size = "";
                    }

                    // 处理默认值字符串
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

                    // 写入列信息行
                    excelWriter.writeRow(
                            ListUtil.toList(
                                    num++,
                                    columnVO.getName().toLowerCase(),
                                    columnVO.getTypeName().toLowerCase(),
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
                // 跳过当前行
                excelWriter.passCurrentRow();
            }
            // 设置列宽
            excelWriter.setColumnWidth(1, 25);
            excelWriter.setColumnWidth(2, 15);
            excelWriter.setColumnWidth(9, 50);
            // 设置单元格对齐方式
            excelWriter.getCellStyle().setAlignment(HorizontalAlignment.LEFT);
            // 刷新ExcelWriter以确保数据写入
            excelWriter.flush();

            // 输出文件到HTTP响应
            CommonUtil.renderFile(response, file);
        }
    }

}
