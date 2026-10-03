package org.wsitm.schemax.utils;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.wsitm.schemax.utils.json.JSONArray;
import org.wsitm.schemax.utils.json.JSONObject;
import org.wsitm.schemax.utils.json.exception.JSONException;
import org.wsitm.schemax.utils.json.filter.*;

import java.io.IOException;
import java.util.List;

/**
 * Jackson JSON helper with fastjson-like static APIs.
 */
public final class JsonUtil {
    private static final ObjectMapper OBJECT_MAPPER = buildDefaultMapper();

    /**
     * 专用于带 {@link Filter} 序列化的 mapper：通过 Object.class 的 mixin 全局挂载
     * {@link JsonFilter}，使过滤在序列化过程中直接作用于原始 bean（类感知、单次遍历、
     * 保留 @JsonFormat/自定义序列化器等全部 Jackson 特性），避免旧的 POJO→Map→String 多次转换。
     */
    private static final ObjectMapper FILTER_MAPPER = buildFilterMapper();

    private JsonUtil() {
    }

    /**
     * 返回内部 {@link ObjectMapper} 的副本。
     * <p>
     * 不要直接暴露共享单例：调用方若对其 {@code activateDefaultTyping}、{@code registerModule}
     * 或 {@code enable/disable} 等重配置，会全局且跨线程地影响所有 JSON 行为，
     * 开启默认多态类型甚至会引入反序列化 RCE 风险。返回副本可将任何改动隔离在调用方本地。
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER.copy();
    }

    /**
     * 返回共享 {@link ObjectMapper} 的只读 {@link TypeFactory}，用于构造泛型 {@link JavaType}。
     */
    public static TypeFactory getTypeFactory() {
        return OBJECT_MAPPER.getTypeFactory();
    }

    public static String toJSONString(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JSONException("JSON serialize failed", e);
        }
    }

    public static String toJSONString(Object object, Filter filter) {
        if (filter == null) {
            return toJSONString(object);
        }
        return toJSONString(object, new Filter[]{filter});
    }

    /**
     * 使用一组 {@link Filter} 序列化（模仿 fastjson 的多过滤器组合）。
     * <p>
     * 各过滤器按顺序叠加：{@link PropertyPreFilter}/{@link PropertyFilter} 决定字段是否输出，
     * {@link NameFilter} 重命名字段，{@link ValueFilter} 转换字段值。
     */
    public static String toJSONString(Object object, Filter... filters) {
        if (filters == null || filters.length == 0) {
            return toJSONString(object);
        }
        try {
            // 每次序列化注入一个携带当前 Filter 链的 provider；FILTER_MAPPER 本身不可变、线程安全
            SimpleFilterProvider provider = new SimpleFilterProvider()
                    .setFailOnUnknownId(false)
                    .addFilter(Filter.DYNAMIC_FILTER_ID, new FilterBridge(filters));
            return FILTER_MAPPER.writer(provider).writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JSONException("JSON serialize failed", e);
        }
    }

    public static String toJSONString(Object object, Boolean pretty) {
        if (Boolean.TRUE.equals(pretty)) {
            try {
                return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
            } catch (JsonProcessingException e) {
                throw new JSONException("JSON serialize failed", e);
            }
        }
        return toJSONString(object);
    }

    /**
     * Parse text and return default JSON value:
     * object -> {@link JSONObject}, array -> {@link JSONArray}, primitive -> primitive.
     */
    public static Object parse(String text) {
        return fromJsonNode(readTree(text));
    }

    /**
     * Parse object text and return {@link JSONObject}.
     */
    public static JSONObject parseObject(String text) {
        Object value = parse(text);
        if (value == null) {
            return null;
        }
        if (value instanceof JSONObject jsonObject) {
            return jsonObject;
        }
        throw new JSONException("JSON is not object");
    }

    public static <T> T parseObject(String text, Class<T> clazz) {
        if (isEmpty(text)) {
            return null;
        }
        if (clazz == Object.class) {
            return clazz.cast(parse(text));
        }
        try {
            return OBJECT_MAPPER.readValue(text, clazz);
        } catch (IOException e) {
            throw new JSONException("JSON parse failed", e);
        }
    }

    public static <T> T parseObject(String text, TypeReference<T> typeReference) {
        if (isEmpty(text)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(text, typeReference);
        } catch (IOException e) {
            throw new JSONException("JSON parse failed", e);
        }
    }

    /**
     * Parse array text and return {@link JSONArray}.
     */
    public static JSONArray parseArray(String text) {
        Object value = parse(text);
        if (value == null) {
            return null;
        }
        if (value instanceof JSONArray jsonArray) {
            return jsonArray;
        }
        throw new JSONException("JSON is not array");
    }

    public static <T> List<T> parseArray(String text, Class<T> clazz) {
        if (isEmpty(text)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(
                    text,
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz)
            );
        } catch (IOException e) {
            throw new JSONException("JSON parse failed", e);
        }
    }

    /**
     * Convert value to default JSON value:
     * object -> {@link JSONObject}, array -> {@link JSONArray}, primitive -> primitive.
     */
    public static Object toJSON(Object value) {
        return toJavaObject(value);
    }

    /**
     * Convert value without explicit target type:
     * object -> {@link JSONObject}, array -> {@link JSONArray}, primitive -> primitive.
     */
    public static Object toJavaObject(Object value) {
        if (value == null) {
            return null;
        }
        JsonNode node = OBJECT_MAPPER.valueToTree(value);
        return fromJsonNode(node);
    }

    public static <T> T toJavaObject(Object value, Class<T> clazz) {
        if (value == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(value, clazz);
        } catch (IllegalArgumentException e) {
            throw new JSONException("JSON convert failed", e);
        }
    }

    public static <T> T toJavaObject(Object value, TypeReference<T> typeReference) {
        if (value == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(value, typeReference);
        } catch (IllegalArgumentException e) {
            throw new JSONException("JSON convert failed", e);
        }
    }

    public static <T> T toJavaObject(Object value, JavaType javaType) {
        if (value == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.convertValue(value, javaType);
        } catch (IllegalArgumentException e) {
            throw new JSONException("JSON convert failed", e);
        }
    }

    public static boolean isValid(String text) {
        if (isEmpty(text)) {
            return false;
        }
        try {
            JsonNode node = readTree(text);
            if (node == null) {
                return false;
            }
            return node.isContainerNode();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidObject(String text) {
        if (isEmpty(text)) {
            return false;
        }
        try {
            JsonNode node = readTree(text);
            if (node == null) {
                return false;
            }
            return node.isObject();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidArray(String text) {
        if (isEmpty(text)) {
            return false;
        }
        try {
            JsonNode node = readTree(text);
            if (node == null) {
                return false;
            }
            return node.isArray();
        } catch (Exception e) {
            return false;
        }
    }

    private static JsonNode readTree(String text) {
        if (isEmpty(text)) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readTree(text);
        } catch (IOException e) {
            throw new JSONException("JSON parse failed", e);
        }
    }

    private static Object fromJsonNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            JSONObject out = new JSONObject();
            node.fields().forEachRemaining(entry -> out.put(entry.getKey(), fromJsonNode(entry.getValue())));
            return out;
        }
        if (node.isArray()) {
            JSONArray out = new JSONArray();
            for (JsonNode item : node) {
                out.add(fromJsonNode(item));
            }
            return out;
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isNumber()) {
            // 浮点数返回 BigDecimal，对齐 fastjson 默认行为，避免 Double 精度丢失
            if (node.isFloatingPointNumber()) {
                return node.decimalValue();
            }
            return node.numberValue();
        }
        if (node.isTextual()) {
            return node.textValue();
        }
        return node.asText();
    }

    private static ObjectMapper buildDefaultMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // 拒绝尾部多余内容，使 isValidObject/isValidArray 等校验对 "123abc" 之类的畸形输入返回 false
        mapper.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        return mapper;
    }

    /**
     * 构建带全局 {@link JsonFilter} mixin 的 mapper：mixin 挂在 Object.class 上，
     * 会级联到所有 bean，因此嵌套对象也会被同一 {@link Filter} 过滤（与旧实现的全局按名过滤语义一致）。
     */
    private static ObjectMapper buildFilterMapper() {
        ObjectMapper mapper = buildDefaultMapper();
        mapper.addMixIn(Object.class, DynamicFilterMixin.class);
        return mapper;
    }

    private static boolean isEmpty(String str) {
        // 使用 trim 判空：与 parse(String) 对空白串返回 null 的行为保持一致，
        // 避免 parseObject("   ", clazz) 因 isEmpty 漏判而抛出 JSONException
        return str == null || str.trim().isEmpty();
    }
}
