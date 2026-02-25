package org.wsitm.schemax.utils.json;


import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class SimplePropertyPreFilter implements PropertyPreFilter {
    private final Set<String> includes = new LinkedHashSet<>();
    private final Set<String> excludes = new LinkedHashSet<>();

    public SimplePropertyPreFilter includes(String... fieldNames) {
        if (fieldNames != null && fieldNames.length > 0) {
            includes.addAll(Arrays.asList(fieldNames));
        }
        return this;
    }

    public SimplePropertyPreFilter excludes(String... fieldNames) {
        if (fieldNames != null && fieldNames.length > 0) {
            excludes.addAll(Arrays.asList(fieldNames));
        }
        return this;
    }

    @Override
    public boolean process(Object object, String name) {
        if (!includes.isEmpty() && !includes.contains(name)) {
            return false;
        }
        return !excludes.contains(name);
    }
}
