package org.wsitm.schemax.utils.json;

import com.fasterxml.jackson.core.type.TypeReference;
import org.wsitm.schemax.utils.JsonUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.temporal.TemporalAccessor;
import java.util.*;

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
        if (value == null) {
            return null;
        } else if (value instanceof String) {
            return (String) value;
        } else if (value instanceof Date) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            return sdf.format((Date) value);
        } else {
            return !(value instanceof Boolean)
                    && !(value instanceof Character)
                    && !(value instanceof Number)
                    && !(value instanceof UUID)
                    && !(value instanceof Enum)
                    && !(value instanceof TemporalAccessor) ? JsonUtil.toJSONString(value) : value.toString();
        }
    }

    public Double getDouble(String key) {
        Object value = get(key);
        if (value == null) {
            return null;
        } else if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof Number) {
            return ((Number) value).doubleValue();
        } else if (value instanceof String) {
            String str = ((String) value).trim();
            return !str.isEmpty() && !"null".equalsIgnoreCase(str) ? Double.parseDouble(str) : null;
        } else {
            throw new JSONException("Can not cast '" + value.getClass() + "' to double");
        }
    }

    public double getDoubleValue(String key) {
        Double value = this.getDouble(key);
        return value == null ? (double) 0.0F : value;
    }

    public Float getFloat(String key) {
        Object value = super.get(key);
        if (value == null) {
            return null;
        } else if (value instanceof Float) {
            return (Float) value;
        } else if (value instanceof Number) {
            return ((Number) value).floatValue();
        } else if (value instanceof String) {
            String str = ((String) value).trim();
            return !str.isEmpty() && !"null".equalsIgnoreCase(str) ? Float.parseFloat(str) : null;
        } else {
            throw new JSONException("Can not cast '" + value.getClass() + "' to float");
        }
    }

    public float getFloatValue(String key) {
        Float value = this.getFloat(key);
        return value == null ? 0.0F : value;
    }

    public Long getLong(String key) {
        Object value = super.get(key);
        if (value == null) {
            return null;
        } else if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            String str = ((String) value).trim();
            if (!str.isEmpty() && !"null".equalsIgnoreCase(str)) {
                return str.indexOf(46) != -1 ? (long) Double.parseDouble(str) : Long.parseLong(str);
            } else {
                return null;
            }
        } else if (value instanceof Boolean) {
            return (Boolean) value ? 1L : 0L;
        } else {
            throw new JSONException("Can not cast '" + value.getClass() + "' to Long");
        }
    }

    public long getLongValue(String key) {
        return this.getLongValue(key, 0L);
    }

    public long getLongValue(String key, long defaultValue) {
        Long value = getLong(key);
        return value == null ? defaultValue : value;
    }

    public Integer getInteger(String key) {
        Object value = super.get(key);
        if (value == null) {
            return null;
        } else if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            String str = ((String) value).trim();
            if (!str.isEmpty() && !"null".equalsIgnoreCase(str)) {
                return str.indexOf(46) != -1 ? (int) Double.parseDouble(str) : Integer.parseInt(str);
            } else {
                return null;
            }
        } else if (value instanceof Boolean) {
            return (Boolean) value ? 1 : 0;
        } else {
            throw new JSONException("Can not cast '" + value.getClass() + "' to Integer");
        }
    }

    public int getIntValue(String key) {
        return this.getIntValue(key, 0);
    }

    public int getIntValue(String key, int defaultValue) {
        Integer value = getInteger(key);
        return value == null ? defaultValue : value;
    }

    public Short getShort(String key) {
        Object value = super.get(key);
        if (value == null) {
            return null;
        } else if (value instanceof Short) {
            return (Short) value;
        } else if (value instanceof Number) {
            return ((Number) value).shortValue();
        } else if (value instanceof String) {
            String str = ((String) value).trim();
            return !str.isEmpty() && !"null".equalsIgnoreCase(str) ? Short.parseShort(str) : null;
        } else {
            throw new JSONException("Can not cast '" + value.getClass() + "' to short");
        }
    }

    public short getShortValue(String key) {
        Short value = this.getShort(key);
        return value == null ? 0 : value;
    }

    public Boolean getBoolean(String key) {
        Object value = super.get(key);
        if (value == null) {
            return null;
        } else if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof Number) {
            return ((Number) value).intValue() == 1;
        } else if (!(value instanceof String)) {
            throw new JSONException("Can not cast '" + value.getClass() + "' to boolean");
        } else {
            String str = (String) value;
            return !str.isEmpty() && !"null".equalsIgnoreCase(str) ? "true".equalsIgnoreCase(str) || "1".equals(str) : null;
        }
    }

    public boolean getBooleanValue(String key) {
        Boolean value = this.getBoolean(key);
        return value != null && value;
    }

    public boolean getBooleanValue(String key, boolean defaultValue) {
        Boolean value = this.getBoolean(key);
        return value == null ? defaultValue : value;
    }

    public BigDecimal getBigDecimal(String key) {
        Object value = super.get(key);
        if (value == null) {
            return null;
        } else if (value instanceof Number) {
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            } else if (value instanceof BigInteger) {
                return new BigDecimal((BigInteger) value);
            } else if (value instanceof Float) {
                float floatValue = (Float) value;
                return new BigDecimal(floatValue);
            } else if (value instanceof Double) {
                double doubleValue = (Double) value;
                return new BigDecimal(doubleValue);
            } else {
                long longValue = ((Number) value).longValue();
                return BigDecimal.valueOf(longValue);
            }
        } else if (value instanceof String) {
            return new BigDecimal(((String) value).trim());
        } else if (value instanceof Boolean) {
            return (Boolean) value ? BigDecimal.ONE : BigDecimal.ZERO;
        } else {
            throw new JSONException("Can not cast '" + value.getClass() + "' to BigDecimal");
        }
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
        return new JSONObject(this);
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
