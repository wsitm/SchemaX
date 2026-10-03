package org.wsitm.schemax.utils.json;

import org.wsitm.schemax.utils.json.exception.JSONException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * JSONObject / JSONArray 共用的类型转换助手，行为对齐 fastjson 的 TypeUtils。
 */
final class Casts {
    private static final String[] DATE_FORMATS = {
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss.SSS",
            "yyyy-MM-dd"
    };

    /**
     * 线程安全的日期格式化器，替代每次 new SimpleDateFormat；时区与原 SimpleDateFormat（系统默认）一致
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private Casts() {
    }

    static String formatDate(Date value) {
        return DATE_TIME_FORMATTER.format(value.toInstant());
    }

    static Date toDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof Number) {
            return new Date(((Number) value).longValue());
        }
        if (value instanceof Instant) {
            return Date.from((Instant) value);
        }
        if (value instanceof String) {
            String str = ((String) value).trim();
            if (str.isEmpty() || "null".equalsIgnoreCase(str)) {
                return null;
            }
            try {
                return new Date(Long.parseLong(str));
            } catch (NumberFormatException ignored) {
            }
            for (String format : DATE_FORMATS) {
                try {
                    return new SimpleDateFormat(format).parse(str);
                } catch (ParseException ignored) {
                }
            }
            try {
                return Date.from(Instant.parse(str));
            } catch (Exception ignored) {
            }
            throw new JSONException("Can not cast '" + str + "' to Date");
        }
        throw new JSONException("Can not cast '" + value.getClass() + "' to Date");
    }

    static BigInteger toBigInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigInteger) {
            return (BigInteger) value;
        }
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).toBigInteger();
        }
        if (value instanceof Number) {
            return new BigInteger(value.toString());
        }
        if (value instanceof Boolean) {
            return (Boolean) value ? BigInteger.ONE : BigInteger.ZERO;
        }
        if (value instanceof String) {
            String str = ((String) value).trim();
            if (str.isEmpty() || "null".equalsIgnoreCase(str)) {
                return null;
            }
            return str.indexOf('.') != -1 ? new BigDecimal(str).toBigInteger() : new BigInteger(str);
        }
        throw new JSONException("Can not cast '" + value.getClass() + "' to BigInteger");
    }

    static Byte toByte(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Byte) {
            return (Byte) value;
        }
        if (value instanceof Number) {
            return ((Number) value).byteValue();
        }
        if (value instanceof Boolean) {
            return (Boolean) value ? (byte) 1 : (byte) 0;
        }
        if (value instanceof String) {
            String str = ((String) value).trim();
            return !str.isEmpty() && !"null".equalsIgnoreCase(str) ? Byte.parseByte(str) : null;
        }
        throw new JSONException("Can not cast '" + value.getClass() + "' to byte");
    }
}
