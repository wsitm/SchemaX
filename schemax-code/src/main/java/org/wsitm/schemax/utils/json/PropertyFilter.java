package org.wsitm.schemax.utils.json;

public interface PropertyFilter extends Filter {
    boolean apply(Object object, String name, Object value);
}
