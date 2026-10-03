package org.wsitm.schemax.utils.json.filter;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

/**
 * 动态过滤 mixin：为所有类型挂载固定的 {@link JsonFilter} id，
 * 具体过滤逻辑在每次序列化时通过 {@link SimpleFilterProvider} 注入。
 */
@JsonFilter(Filter.DYNAMIC_FILTER_ID)
public interface DynamicFilterMixin {

}
