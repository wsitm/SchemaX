package org.wsitm.schemax.utils.json.filter;

public interface Filter {

    /**
     * 动态过滤器 id，配合 {@link DynamicFilterMixin} 使用。
     */
    public static final String DYNAMIC_FILTER_ID = "__dynamicJsonFilter__";

}
