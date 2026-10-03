package org.wsitm.schemax.utils.json.filter;

public interface PropertyPreFilter extends Filter {
    boolean process(Object object, String name);
}
