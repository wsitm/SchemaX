package org.wsitm.schemax.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.wsitm.schemax.utils.json.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Jackson JSON helper with fastjson-like static APIs.
 */
public final class JsonUtil {
    private static final ObjectMapper OBJECT_MAPPER = buildDefaultMapper();

    private JsonUtil() {
    }

    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
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
        Object normalized = toJavaObject(object);
        Object filtered = applyFilter(normalized, filter);
        return toJSONString(filtered);
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
            return new JSONObject();
        }
        if (value instanceof JSONObject jsonObject) {
            return jsonObject;
        }
        throw new JSONException("JSON is not object");
    }

    public static <T> T parseObject(String text, Class<T> clazz) {
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
            return new JSONArray();
        }
        if (value instanceof JSONArray jsonArray) {
            return jsonArray;
        }
        throw new JSONException("JSON is not array");
    }

    public static <T> List<T> parseArray(String text, Class<T> clazz) {
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
        JsonNode node = OBJECT_MAPPER.valueToTree(value);
        return fromJsonNode(node);
    }

    public static <T> T toJavaObject(Object value, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.convertValue(value, clazz);
        } catch (IllegalArgumentException e) {
            throw new JSONException("JSON convert failed", e);
        }
    }

    public static <T> T toJavaObject(Object value, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.convertValue(value, typeReference);
        } catch (IllegalArgumentException e) {
            throw new JSONException("JSON convert failed", e);
        }
    }

    public static <T> T toJavaObject(Object value, JavaType javaType) {
        try {
            return OBJECT_MAPPER.convertValue(value, javaType);
        } catch (IllegalArgumentException e) {
            throw new JSONException("JSON convert failed", e);
        }
    }

    public static boolean isValidObject(String text) {
        try {
            return readTree(text).isObject();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isValidArray(String text) {
        try {
            return readTree(text).isArray();
        } catch (Exception e) {
            return false;
        }
    }

    private static JsonNode readTree(String text) {
        try {
            return OBJECT_MAPPER.readTree(text);
        } catch (IOException e) {
            throw new JSONException("JSON parse failed", e);
        }
    }

    private static Object applyFilter(Object current, Filter filter) {
        if (current == null) {
            return null;
        }
        if (current instanceof JSONObject jsonObject) {
            JSONObject out = new JSONObject();
            for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                String fieldName = entry.getKey();
                Object fieldValue = entry.getValue();
                if (!includeField(filter, jsonObject, fieldName, fieldValue)) {
                    continue;
                }
                out.put(fieldName, applyFilter(fieldValue, filter));
            }
            return out;
        }
        if (current instanceof JSONArray jsonArray) {
            JSONArray out = new JSONArray();
            for (Object item : jsonArray) {
                out.add(applyFilter(item, filter));
            }
            return out;
        }
        return current;
    }

    private static boolean includeField(Filter filter, Object source, String name, Object value) {
        if (filter instanceof PropertyPreFilter propertyPreFilter
                && !propertyPreFilter.process(source, name)) {
            return false;
        }
        if (filter instanceof PropertyFilter propertyFilter
                && !propertyFilter.apply(source, name, value)) {
            return false;
        }
        return true;
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
        return mapper;
    }

}
