package org.wsitm.schemax.utils.json.filter;

/**
 * 值过滤器（模仿 fastjson {@code ValueFilter}）。
 * <p>
 * 序列化时对每个字段调用 {@link #process}，返回值作为最终写出的字段值（返回 {@code null} 表示置空）。
 * <p>
 * 注意：值被改写后，该字段将按其新值的运行时类型序列化，原字段上的 {@code @JsonFormat}/自定义序列化器
 * 等属性级配置不再生效；仅对 bean 属性生效，Map/JSONObject 的条目取值不支持。
 */
public interface ValueFilter extends Filter {

    /**
     * @param object 字段所属的源对象（真实 bean）
     * @param name   字段名
     * @param value  原字段值
     * @return 新的字段值
     */
    Object process(Object object, String name, Object value);
}
