package org.wsitm.schemax.metainfo;

import cn.hutool.core.util.StrUtil;
import org.wsitm.schemax.entity.domain.ConnectInfo;
import org.wsitm.schemax.exception.ServiceException;
import org.wsitm.schemax.utils.CommonUtil;

import java.util.function.Function;
import java.util.regex.Pattern;

public class MetaInfoUtil {

    /**
     * 创建表名检查器函数
     *
     * @param connectInfo 连接信息对象，包含过滤类型和通配符配置
     * @return 返回一个函数，用于检查给定的表名是否符合过滤条件
     */
    public static Function<String, Boolean> createTableNameChecker(ConnectInfo connectInfo) {
        // 处理通配符/正则的表名
        Pattern pattern = null;
        String[] skipStrArr = new String[0];
        // 根据过滤类型初始化匹配模式和跳过字符串数组
        if (StrUtil.isNotEmpty(connectInfo.getWildcard())) {
            if (connectInfo.getFilterType() == 1) {
                skipStrArr = CommonUtil.dealStipStrArr(connectInfo.getWildcard().split(","));
            } else {
                try {
                    pattern = Pattern.compile(connectInfo.getWildcard());
                } catch (Exception e) {
                    throw new ServiceException("正则表达式错误，" + e.getMessage());
                }
            }
        }
        // 返回表名匹配函数
        String[] finalSkipStrArr = skipStrArr;
        Pattern finalPattern = pattern;
        return (tableName) -> {
            if (StrUtil.isEmpty(connectInfo.getWildcard())) {
                return true;
            }
            if (connectInfo.getFilterType() == 1) {
                return CommonUtil.matchAnyIgnoreCase(tableName, finalSkipStrArr);
            } else {
                return finalPattern != null && finalPattern.matcher(tableName).matches();
            }
        };
    }

}
