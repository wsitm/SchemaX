package org.wsitm.rdbms.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.db.meta.JdbcType;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.wsitm.rdbms.constant.RdbmsConstants;
import org.wsitm.rdbms.entity.domain.ConnectInfo;
import org.wsitm.rdbms.entity.domain.JdbcInfo;
import org.wsitm.rdbms.entity.vo.ColumnVO;
import org.wsitm.rdbms.entity.vo.ConnectInfoVO;
import org.wsitm.rdbms.entity.vo.TableVO;
import org.wsitm.rdbms.exception.ServiceException;
import org.wsitm.rdbms.metainfo.IMetaInfoHandler;
import org.wsitm.rdbms.metainfo.MetaInfoFactory;
import org.wsitm.rdbms.service.IConnectInfoService;
import org.wsitm.rdbms.utils.CacheUtil;
import org.wsitm.rdbms.utils.CommonUtil;
import org.wsitm.rdbms.utils.DDLUtil;
import org.wsitm.rdbms.utils.PoiUtil;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

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


    /**
     * 查询连接配置
     *
     * @param connectId 连接配置主键
     * @return 连接配置
     */
    @Override
    public ConnectInfoVO selectConnectInfoByConnectId(String connectId) {
        ConnectInfoVO connectInfoVO = CacheUtil.getConnectInfo(connectId);
        connectInfoVO.setCacheType(CacheUtil.cacheType(connectInfoVO.getConnectId()));
        return connectInfoVO;
    }

    /**
     * 查询连接配置列表
     *
     * @return 连接配置
     */
    @Override
    public List<ConnectInfoVO> selectConnectInfoList() {
        List<ConnectInfoVO> connectInfoVOList = CacheUtil.getConnectInfoList();
        for (ConnectInfoVO connectInfoVO : connectInfoVOList) {
            connectInfoVO.setCacheType(CacheUtil.cacheType(connectInfoVO.getConnectId()));
        }
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
        connectInfo.setConnectId(IdUtil.nanoId());
        connectInfo.setCreateTime(LocalDateTime.now());
        JdbcInfo jdbcInfo = CacheUtil.getJdbcInfo(connectInfo.getJdbcId());

        ConnectInfoVO connectInfoVO = new ConnectInfoVO();
        BeanUtil.copyProperties(connectInfo, connectInfoVO);
        connectInfoVO.setJdbcName(jdbcInfo.getJdbcName());
        connectInfoVO.setDriverClass(jdbcInfo.getDriverClass());
        connectInfoVO.setJdbcFile(jdbcInfo.getJdbcFile());
        CacheUtil.saveItemToConnectInfo(connectInfoVO);

        flushCahce(connectInfo.getConnectId());

        return 1;
    }

    /**
     * 修改连接配置
     *
     * @param connectInfo 连接配置
     * @return 结果
     */
    @Override
    public int updateConnectInfo(ConnectInfo connectInfo) {

        JdbcInfo jdbcInfo = CacheUtil.getJdbcInfo(connectInfo.getJdbcId());

        ConnectInfoVO connectInfoVO = new ConnectInfoVO();
        BeanUtil.copyProperties(connectInfo, connectInfoVO);
        connectInfoVO.setJdbcName(jdbcInfo.getJdbcName());
        connectInfoVO.setDriverClass(jdbcInfo.getDriverClass());
        connectInfoVO.setJdbcFile(jdbcInfo.getJdbcFile());
        CacheUtil.saveItemToConnectInfo(connectInfoVO);

        flushCahce(connectInfo.getConnectId());

        return 1;
    }

    /**
     * 批量删除连接配置
     *
     * @param connectIds 需要删除的连接配置主键
     * @return 结果
     */
    @Override
    public int deleteConnectInfoByConnectIds(String[] connectIds) {
        CacheUtil.removeConnectInfoByIds(connectIds);
        return 1;
    }

    /**
     * 删除连接配置信息
     *
     * @param connectId 连接配置主键
     * @return 结果
     */
    @Override
    public int deleteConnectInfoByConnectId(String connectId) {
        CacheUtil.removeConnectInfoByIds(new String[]{connectId});
        return 1;
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
    public List<TableVO> getTableInfo(String connectId) {
        return CacheUtil.getTableMetaList(connectId);
    }

    /**
     * 刷新缓存
     *
     * @param connectId 连接ID
     * @return 布尔
     */
    @Override
    public boolean flushCahce(String connectId) {
        ConnectInfoVO connectInfoVO = CacheUtil.getConnectInfo(connectId);
        IMetaInfoHandler metaInfoHandler = MetaInfoFactory.getInstance(connectInfoVO.getDriverClass());
        ThreadUtil.execute(() -> metaInfoHandler.loadDataToCache(connectId));
        return true;
    }

    /**
     * 生成表格DDL
     *
     * @param connectId 连接ID
     * @param database  数据库类型
     * @return DDL
     */
    public Map<String, String[]> genTableDDL(String connectId, String database) {
        List<TableVO> tableVOList = CacheUtil.getTableMetaList(connectId);
        return DDLUtil.genDDL(tableVOList, database);
    }


    public static final String[] ARR_COL = new String[]{"序号", "字段", "类型", "长度", "小数", "为空", "自增", "主键", "默认", "注释"};

    public void exportTableInfo(HttpServletResponse response, String connectId, String[] skipStrArr) throws IOException {

        String fileName = "表格信息-" + connectId + "-" + DateUtil.format(new Date(), DatePattern.PURE_DATETIME_PATTERN) + ".xlsx";
        String path = RdbmsConstants.FILE_PATH + File.separator + "connect/";
        File dir = new File(path);
        // 判断路径是否存在
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(path, fileName);
        try (ExcelWriter excelWriter = ExcelUtil.getBigWriter(file)) {

            skipStrArr = ArrayUtil.removeEmpty(skipStrArr);
            if (ArrayUtil.isEmpty(skipStrArr)) {
                skipStrArr = new String[]{"*"};
            }
            Arrays.sort(skipStrArr, (s1, s2) -> {
                boolean b1 = StrUtil.startWith(s1, "!");
                boolean b2 = StrUtil.startWith(s2, "!");
                if (b1 && b2) return 0;
                if (b1) return -1;
                if (b2) return 1;
                return 0;
            });

            int tableNum = 1;

            Font font = excelWriter.getWorkbook().createFont();
            font.setBold(true);
            int currRow = 1;

            List<TableVO> tableVOList = CacheUtil.getTableMetaList(connectId);
            for (TableVO tableVO : tableVOList) {

                String tableName = tableVO.getTableName();
                if (!CommonUtil.matchAnyIgnoreCase(tableName, skipStrArr)) {
                    continue;
                }

                String title = (tableNum++) + ". " + tableName.toLowerCase() + ", " + tableVO.getComment();
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
                int num = 1;
                for (ColumnVO columnVO : columns) {

                    String size = StrUtil.toString(columnVO.getSize());
//                    if (columnVO.getType() == JdbcType.FLOAT.typeCode
//                            || columnVO.getType() == JdbcType.DOUBLE.typeCode
//                            || columnVO.getType() == JdbcType.NUMERIC.typeCode
//                            || columnVO.getType() == JdbcType.DECIMAL.typeCode) {
//                        size += "," + columnVO.getDigit();
//                    }
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
