package org.wsitm.schemax.utils.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fastjson-like reader holder for auto-type filter creation.
 */
public final class JSONReader {

    private JSONReader() {
    }

    public interface AutoTypeBeforeHandler extends Filter {
        default Class<?> apply(long typeNameHash, Class<?> expectClass, long features) {
            return null;
        }

        Class<?> apply(String typeName, Class<?> expectClass, long features);
    }

    public static AutoTypeBeforeHandler autoTypeFilter(String... names) {
        return autoTypeFilter(false, names);
    }

    public static AutoTypeBeforeHandler autoTypeFilter(boolean includeBasic, String... names) {
        return new ContextAutoTypeBeforeHandler(includeBasic, names);
    }

    public static AutoTypeBeforeHandler autoTypeFilter(Class<?>... classes) {
        return autoTypeFilter(false, classes);
    }

    public static AutoTypeBeforeHandler autoTypeFilter(boolean includeBasic, Class<?>... classes) {
        Set<String> names = new LinkedHashSet<>();
        if (classes != null) {
            for (Class<?> clazz : classes) {
                if (clazz != null) {
                    names.add(clazz.getName());
                }
            }
        }
        return new ContextAutoTypeBeforeHandler(includeBasic, names.toArray(new String[0]));
    }

    private static final class ContextAutoTypeBeforeHandler implements AutoTypeBeforeHandler {
        private final Set<String> allowNames = new LinkedHashSet<>();

        private ContextAutoTypeBeforeHandler(boolean includeBasic, String... names) {
            if (includeBasic) {
                registerBasicTypes();
            }

            if (names != null) {
                for (String name : names) {
                    registerTypeName(name);
                }
            }
        }

        @Override
        public Class<?> apply(String typeName, Class<?> expectClass, long features) {
            String normalizedTypeName = normalizeTypeName(typeName);
            if (normalizedTypeName == null || !allowNames.contains(normalizedTypeName)) {
                return null;
            }

            Class<?> actualClass = loadClass(typeName);
            if (actualClass == null) {
                return null;
            }

            if (expectClass == null || expectClass == Object.class) {
                return actualClass;
            }

            Class<?> normalizedExpectClass = normalizePrimitive(expectClass);
            Class<?> normalizedActualClass = normalizePrimitive(actualClass);
            if (!normalizedExpectClass.isAssignableFrom(normalizedActualClass)) {
                return null;
            }
            return normalizedActualClass;
        }

        private void registerBasicTypes() {
            registerClass(Object.class);
            registerClass(String.class);
            registerClass(Boolean.class);
            registerClass(Byte.class);
            registerClass(Short.class);
            registerClass(Integer.class);
            registerClass(Long.class);
            registerClass(Float.class);
            registerClass(Double.class);
            registerClass(BigDecimal.class);
            registerClass(BigInteger.class);
            registerClass(Date.class);
            registerClass(Instant.class);
            registerClass(LocalDate.class);
            registerClass(LocalDateTime.class);
            registerClass(LocalTime.class);

            registerClass(Map.class);
            registerClass(List.class);
            registerClass(Set.class);
            registerClass(Collection.class);

            registerClass(ArrayList.class);
            registerClass(LinkedList.class);
            registerClass(HashSet.class);
            registerClass(LinkedHashSet.class);

            registerClass(JSONObject.class);
            registerClass(JSONArray.class);
        }

        private void registerClass(Class<?> clazz) {
            if (clazz == null) {
                return;
            }
            registerTypeName(clazz.getName());
            String canonicalName = clazz.getCanonicalName();
            if (canonicalName != null) {
                registerTypeName(canonicalName);
            }
        }

        private void registerTypeName(String typeName) {
            String normalized = normalizeTypeName(typeName);
            if (normalized != null) {
                allowNames.add(normalized);
            }
        }
    }

    private static String normalizeTypeName(String typeName) {
        if (typeName == null) {
            return null;
        }
        String normalized = typeName.trim();
        return normalized.isEmpty() ? null : normalized.replace('$', '.');
    }

    private static Class<?> loadClass(String className) {
        if (className == null || className.trim().isEmpty()) {
            return null;
        }

        String trimmed = className.trim();
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            try {
                return Class.forName(trimmed, false, contextClassLoader);
            } catch (ClassNotFoundException ignored) {
            }
        }

        try {
            return Class.forName(trimmed);
        } catch (ClassNotFoundException ignored) {
            return null;
        }
    }

    private static Class<?> normalizePrimitive(Class<?> clazz) {
        if (clazz == null || !clazz.isPrimitive()) {
            return clazz;
        }
        if (clazz == boolean.class) {
            return Boolean.class;
        }
        if (clazz == byte.class) {
            return Byte.class;
        }
        if (clazz == short.class) {
            return Short.class;
        }
        if (clazz == int.class) {
            return Integer.class;
        }
        if (clazz == long.class) {
            return Long.class;
        }
        if (clazz == float.class) {
            return Float.class;
        }
        if (clazz == double.class) {
            return Double.class;
        }
        if (clazz == char.class) {
            return Character.class;
        }
        return Void.class;
    }
}

