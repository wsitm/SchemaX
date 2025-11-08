package org.wsitm.schemax.metainfo;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.util.ClassUtil;
import org.wsitm.schemax.constant.RdbmsConstants;
import org.wsitm.schemax.metainfo.anno.JdbcType;
import org.wsitm.schemax.utils.SpringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MetaInfoFactory {

    private static final Map<String, Class<?>> TYPE = new HashMap<>();

    static {
        Set<Class<?>> clazzSet = ClassUtil.scanPackageByAnnotation(
                MetaInfoFactory.class.getPackage().getName(), JdbcType.class);
        for (Class<?> clazz : clazzSet) {
            JdbcType jdbcType = AnnotationUtil.getAnnotation(clazz, JdbcType.class);
            for (String jdbc : jdbcType.value()) {
                TYPE.put(jdbc, clazz);
            }
        }
    }

    public static IMetaInfoHandler getInstance(String type) {
        Class<?> clazz = TYPE.get(type);
        if (clazz == null) {
            clazz = TYPE.get(RdbmsConstants.JDBC_RDBMS);
        }
        return (IMetaInfoHandler) SpringUtils.getBean(clazz);
    }


}
