package org.wsitm.schemax.utils.json.filter;

/**
 * 名称过滤器（模仿 fastjson {@code NameFilter}）。
 * <p>
 * 序列化时对每个字段调用 {@link #process}，返回值作为最终写出的字段名。
 * 返回 {@code null} 或与原名相同则保持原名不变。
 * <p>
 * 注意：仅对 bean 属性生效；Map/JSONObject 的条目改名不支持（Jackson 在 Map 过滤时不提供该能力）。
 */
public interface NameFilter extends Filter {

    /**
     * @param object 字段所属的源对象（真实 bean）
     * @param name   原字段名
     * @param value  字段值
     * @return 新的字段名；返回 {@code null} 表示保持原名
     */
    String process(Object object, String name, Object value);
}
