package org.wsitm.schemax.utils.json;

import org.wsitm.schemax.utils.JsonUtil;

import java.util.ArrayList;
import java.util.List;

public class JSONArray extends ArrayList<Object> {
    public JSONArray() {
        super();
    }

    public JSONArray(List<Object> value) {
        super(value);
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

    public <T> List<T> toList(Class<T> clazz) {
        return JsonUtil.toJavaObject(
                this,
                JsonUtil.getObjectMapper().getTypeFactory().constructCollectionType(List.class, clazz)
        );
    }

    public <T> List<T> toJavaList(Class<T> clazz) {
        return toList(clazz);
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
