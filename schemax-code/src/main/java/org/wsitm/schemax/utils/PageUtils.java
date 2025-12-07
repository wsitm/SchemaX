package org.wsitm.schemax.utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;

/**
 * 分页工具类
 *
 * @author ruoyi
 */
public class PageUtils extends PageHelper {
    /**
     * 当前记录起始索引
     */
    public static final String PAGE_NUM = "pageNum";
    /**
     * 每页显示记录数
     */
    public static final String PAGE_SIZE = "pageSize";
    /**
     * 排序列
     */
    public static final String ORDER_BY_COLUMN = "orderByColumn";
    /**
     * 排序的方向 "desc" 或者 "asc".
     */
    public static final String IS_ASC = "isAsc";
    /**
     * 分页参数合理化
     */
    public static final String REASONABLE = "reasonable";


    /**
     * 设置请求分页数据
     */
    public static void startPage() {
        Integer pageNum = Convert.toInt(ServletUtils.getParameter(PAGE_NUM), 1);
        Integer pageSize = Convert.toInt(ServletUtils.getParameter(PAGE_SIZE), 10);
        String orderBy = getOrderBy(ServletUtils.getParameter(ORDER_BY_COLUMN), ServletUtils.getParameter(IS_ASC));
        Boolean reasonable = ServletUtils.getParameterToBool(REASONABLE);
        PageHelper.startPage(pageNum, pageSize, orderBy).setReasonable(reasonable);
    }

    /**
     * 清理分页的线程变量
     */
    public static void clearPage() {
        PageHelper.clearPage();
    }

    private static String getOrderBy(String orderByColumn, String isAsc) {
        if (StrUtil.isEmpty(orderByColumn)) {
            return "";
        }
        return StrUtil.toUnderlineCase(orderByColumn) + " " + isAsc;
    }
}
