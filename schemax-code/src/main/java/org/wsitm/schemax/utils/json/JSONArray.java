package org.wsitm.schemax.utils.json;

import org.wsitm.schemax.utils.JsonUtil;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
        Object value = this.get(index);
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

    public Double getDouble(int index) {
        Object value = this.get(index);
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

    public double getDoubleValue(int index) {
        Double value = this.getDouble(index);
        return value == null ? (double) 0.0F : value;
    }

    public Float getFloat(int index) {
        Object value = this.get(index);
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

    public float getFloatValue(int index) {
        Float value = this.getFloat(index);
        return value == null ? 0.0F : value;
    }


    public Long getLong(int index) {
        Object value = this.get(index);
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

    public long getLongValue(int index) {
        Long value = this.getLong(index);
        return value == null ? 0L : value;
    }


    public Integer getInteger(int index) {
        Object value = this.get(index);
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

    public int getIntValue(int index) {
        Integer value = this.getInteger(index);
        return value == null ? 0 : value;
    }

    public Short getShort(int index) {
        Object value = this.get(index);
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

    public short getShortValue(int index) {
        Short value = this.getShort(index);
        return value == null ? 0 : value;
    }

    public Boolean getBoolean(int index) {
        Object value = this.get(index);
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

    public boolean getBooleanValue(int index) {
        Boolean value = this.getBoolean(index);
        return value != null && value;
    }

    public BigDecimal getBigDecimal(int index) {
        Object value = this.get(index);
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
