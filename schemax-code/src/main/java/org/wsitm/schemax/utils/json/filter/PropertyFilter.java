package org.wsitm.schemax.utils.json.filter;

public interface PropertyFilter extends Filter {
    boolean apply(Object object, String name, Object value);
}
