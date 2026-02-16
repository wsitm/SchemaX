package org.wsitm.schemax.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.wsitm.schemax.exception.UtilException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
            throw new UtilException("JSON serialize failed", e);
        }
    }

    public static String toJSONStringPretty(Object object) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new UtilException("JSON serialize failed", e);
        }
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
        throw new UtilException("JSON is not object");
    }

    public static <T> T parseObject(String text, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(text, clazz);
        } catch (IOException e) {
            throw new UtilException("JSON parse failed", e);
        }
    }

    public static <T> T parseObject(String text, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(text, typeReference);
        } catch (IOException e) {
            throw new UtilException("JSON parse failed", e);
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
        throw new UtilException("JSON is not array");
    }

    public static <T> List<T> parseArray(String text, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(
                    text,
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz)
            );
        } catch (IOException e) {
            throw new UtilException("JSON parse failed", e);
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
            throw new UtilException("JSON convert failed", e);
        }
    }

    public static <T> T toJavaObject(Object value, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.convertValue(value, typeReference);
        } catch (IllegalArgumentException e) {
            throw new UtilException("JSON convert failed", e);
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
            throw new UtilException("JSON parse failed", e);
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

    public static class JSONObject extends LinkedHashMap<String, Object> {
        public JSONObject() {
            super();
        }

        public JSONObject(Map<String, Object> value) {
            super();
            if (value != null) {
                putAll(value);
            }
        }

        public JSONObject getJSONObject(String key) {
            Object value = get(key);
            if (value == null) {
                return null;
            }
            if (value instanceof JSONObject jsonObject) {
                return jsonObject;
            }
            return JsonUtil.toJavaObject(value, JSONObject.class);
        }

        public JSONArray getJSONArray(String key) {
            Object value = get(key);
            if (value == null) {
                return null;
            }
            if (value instanceof JSONArray jsonArray) {
                return jsonArray;
            }
            return JsonUtil.toJavaObject(value, JSONArray.class);
        }

        public String getString(String key) {
            Object value = get(key);
            return value == null ? null : String.valueOf(value);
        }

        public Integer getInteger(String key) {
            Object value = get(key);
            return value == null ? null : JsonUtil.toJavaObject(value, Integer.class);
        }

        public Long getLong(String key) {
            Object value = get(key);
            return value == null ? null : JsonUtil.toJavaObject(value, Long.class);
        }

        public Boolean getBoolean(String key) {
            Object value = get(key);
            return value == null ? null : JsonUtil.toJavaObject(value, Boolean.class);
        }

        public <T> T getObject(String key, Class<T> clazz) {
            Object value = get(key);
            return value == null ? null : JsonUtil.toJavaObject(value, clazz);
        }

        public String toJSONString() {
            return JsonUtil.toJSONString(this);
        }
    }

    public static class JSONArray extends ArrayList<Object> {
        public JSONArray() {
            super();
        }

        public JSONArray(List<Object> value) {
            super();
            if (value != null) {
                addAll(value);
            }
        }

        public JSONObject getJSONObject(int index) {
            Object value = getOrNull(index);
            if (value == null) {
                return null;
            }
            if (value instanceof JSONObject jsonObject) {
                return jsonObject;
            }
            return JsonUtil.toJavaObject(value, JSONObject.class);
        }

        public JSONArray getJSONArray(int index) {
            Object value = getOrNull(index);
            if (value == null) {
                return null;
            }
            if (value instanceof JSONArray jsonArray) {
                return jsonArray;
            }
            return JsonUtil.toJavaObject(value, JSONArray.class);
        }

        public String getString(int index) {
            Object value = getOrNull(index);
            return value == null ? null : String.valueOf(value);
        }

        public Integer getInteger(int index) {
            Object value = getOrNull(index);
            return value == null ? null : JsonUtil.toJavaObject(value, Integer.class);
        }

        public Long getLong(int index) {
            Object value = getOrNull(index);
            return value == null ? null : JsonUtil.toJavaObject(value, Long.class);
        }

        public Boolean getBoolean(int index) {
            Object value = getOrNull(index);
            return value == null ? null : JsonUtil.toJavaObject(value, Boolean.class);
        }

        public <T> T getObject(int index, Class<T> clazz) {
            Object value = getOrNull(index);
            return value == null ? null : JsonUtil.toJavaObject(value, clazz);
        }

        public String toJSONString() {
            return JsonUtil.toJSONString(this);
        }

        private Object getOrNull(int index) {
            if (index < 0 || index >= size()) {
                return null;
            }
            return get(index);
        }
    }
}
