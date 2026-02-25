package org.wsitm.schemax.utils.json;

public interface PropertyPreFilter extends Filter {
    boolean process(Object object, String name);
}
