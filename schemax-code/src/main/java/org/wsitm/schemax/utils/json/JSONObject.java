package org.wsitm.schemax.utils.json;

import com.fasterxml.jackson.core.type.TypeReference;
import org.wsitm.schemax.utils.JsonUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JSONObject extends LinkedHashMap<String, Object> {
    public JSONObject() {
        super();
    }

    public JSONObject(Map<String, Object> value) {
        super(value);
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

    public <T> List<T> getList(String key, Class<T> clazz) {
        JSONArray array = getJSONArray(key);
        return array == null ? null : array.toJavaList(clazz);
    }

    @Override
    public JSONObject clone() {
        return JsonUtil.parseObject(this.toJSONString());
    }

    public Object to() {
        return JsonUtil.toJavaObject(this);
    }

    public <T> T to(Class<T> clazz) {
        return JsonUtil.toJavaObject(this, clazz);
    }

    public <T> T to(TypeReference<T> typeReference) {
        return JsonUtil.toJavaObject(this, typeReference);
    }

    public Object toJavaObject() {
        return to();
    }

    public <T> T toJavaObject(Class<T> clazz) {
        return to(clazz);
    }

    public <T> T toJavaObject(TypeReference<T> typeReference) {
        return to(typeReference);
    }

    public String toJSONString() {
        return JsonUtil.toJSONString(this);
    }

}
