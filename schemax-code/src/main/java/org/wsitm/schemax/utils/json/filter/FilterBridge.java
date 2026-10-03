package org.wsitm.schemax.utils.json.filter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;

/**
 * 将项目的 {@link Filter}（fastjson 风格）桥接到 Jackson 原生
 * {@link com.fasterxml.jackson.databind.ser.PropertyFilter}。
 * <p>
 * 与旧实现相比，{@code source} 现在是真实的 bean（而非转换后的 Map），实现了类感知过滤。
 * 支持链式叠加多个 {@link Filter}：{@link PropertyPreFilter}/{@link PropertyFilter} 控制包含性，
 * {@link NameFilter} 改名、{@link ValueFilter} 改值。
 * <p>
 * 限制：对于 Map/JSONObject 的条目，Jackson 不提供 value 访问，故 {@link PropertyFilter#apply} 收到的
 * value 为 {@code null}，且 {@link NameFilter}/{@link ValueFilter} 对 Map 条目不生效（会回退到原生序列化）；
 * 基于字段名的 {@link PropertyPreFilter}/{@code SimplePropertyPreFilter} 不受影响。
 */
public class FilterBridge implements com.fasterxml.jackson.databind.ser.PropertyFilter {
    private final Filter[] delegates;

    public FilterBridge(Filter... filters) {
        this.delegates = filters;
    }

    @Override
    public void serializeAsField(Object pojo, JsonGenerator jgen, SerializerProvider prov, PropertyWriter writer)
            throws Exception {
        String name = writer.getName();
        Object value = null;
        boolean hasValue = false;
        if (writer instanceof BeanPropertyWriter beanPropertyWriter) {
            value = beanPropertyWriter.get(pojo);
            hasValue = true;
        }
        // 1) 包含性过滤：PropertyPreFilter / PropertyFilter
        if (!accept(pojo, name, value)) {
            if (!jgen.canOmitFields()) {
                writer.serializeAsOmittedField(pojo, jgen, prov);
            }
            return;
        }
        // 2) 名称/值过滤：NameFilter 改名、ValueFilter 改值，按顺序链式叠加
        String outName = name;
        Object outValue = value;
        boolean changed = false;
        for (Filter filter : delegates) {
            if (filter == null) {
                continue;
            }
            if (filter instanceof NameFilter nameFilter) {
                String renamed = nameFilter.process(pojo, outName, outValue);
                if (renamed != null && !renamed.equals(outName)) {
                    outName = renamed;
                    changed = true;
                }
            }
            if (filter instanceof ValueFilter valueFilter) {
                Object newValue = valueFilter.process(pojo, outName, outValue);
                if (newValue != outValue) {
                    outValue = newValue;
                    changed = true;
                }
            }
        }
        // 3) 未改动或无法取值（如 Map 条目）时走 writer 原生序列化，保留全部 Jackson 语义；
        //    发生改名/改值时手动写出 name 与 value
        if (!hasValue || !changed) {
            writer.serializeAsField(pojo, jgen, prov);
        } else {
            jgen.writeFieldName(outName);
            if (outValue == null) {
                prov.defaultSerializeNull(jgen);
            } else {
                prov.defaultSerializeValue(outValue, jgen);
            }
        }
    }

    @Override
    public void serializeAsElement(Object elementValue, JsonGenerator jgen, SerializerProvider prov,
                                   PropertyWriter writer) throws Exception {
        writer.serializeAsElement(elementValue, jgen, prov);
    }

    @Override
    public void depositSchemaProperty(PropertyWriter writer, JsonObjectFormatVisitor objectVisitor,
                                      SerializerProvider provider) {
        // 过滤仅影响运行时输出，不改变 schema
    }

    @Override
    @Deprecated
    public void depositSchemaProperty(PropertyWriter writer, ObjectNode propertiesNode,
                                      SerializerProvider provider) {
        // 过滤仅影响运行时输出，不改变 schema
    }

    private boolean accept(Object source, String name, Object value) {
        for (Filter filter : delegates) {
            if (filter == null) {
                continue;
            }
            if (filter instanceof PropertyPreFilter propertyPreFilter
                    && !propertyPreFilter.process(source, name)) {
                return false;
            }
            if (filter instanceof PropertyFilter propertyFilter
                    && !propertyFilter.apply(source, name, value)) {
                return false;
            }
        }
        return true;
    }
}

