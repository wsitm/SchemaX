package org.wsitm.schemax.utils;


import cn.hutool.core.convert.Convert;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 客户端工具类
 *
 * @author wsitm
 */
public class ServletUtils {
    /**
     * 获取String参数
     */
    public static String getParameter(String name) {
        return getRequest().getParameter(name);
    }

    /**
     * 获取String参数
     */
    public static String getParameter(String name, String defaultValue) {
        return Convert.toStr(getRequest().getParameter(name), defaultValue);
    }

    /**
     * 获取Integer参数
     */
    public static Integer getParameterToInt(String name) {
        return Convert.toInt(getRequest().getParameter(name));
    }

    /**
     * 获取Integer参数
     */
    public static Integer getParameterToInt(String name, Integer defaultValue) {
        return Convert.toInt(getRequest().getParameter(name), defaultValue);
    }

    /**
     * 获取Boolean参数
     */
    public static Boolean getParameterToBool(String name) {
        return Convert.toBool(getRequest().getParameter(name));
    }

    /**
     * 获取Boolean参数
     */
    public static Boolean getParameterToBool(String name, Boolean defaultValue) {
        return Convert.toBool(getRequest().getParameter(name), defaultValue);
    }

    /**
     * 获得所有请求参数
     *
     * @param request 请求对象{@link ServletRequest}
     * @return Map
     */
    public static Map<String, String[]> getParams(ServletRequest request) {
        final Map<String, String[]> map = request.getParameterMap();
        return Collections.unmodifiableMap(map);
    }

    /**
     * 获得所有请求参数
     *
     * @param request 请求对象{@link ServletRequest}
     * @return Map
     */
    public static Map<String, String> getParamMap(ServletRequest request) {
        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, String[]> entry : getParams(request).entrySet()) {
            params.put(entry.getKey(), ArrayUtil.join(entry.getValue(), ","));
        }
        return params;
    }

    /**
     * 获取request
     */
    public static HttpServletRequest getRequest() {
        return getRequestAttributes().getRequest();
    }

    /**
     * 获取response
     */
    public static HttpServletResponse getResponse() {
        return getRequestAttributes().getResponse();
    }

    /**
     * 获取session
     */
    public static HttpSession getSession() {
        return getRequest().getSession();
    }

    public static ServletRequestAttributes getRequestAttributes() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return (ServletRequestAttributes) attributes;
    }

    /**
     * 将字符串渲染到客户端
     *
     * @param response 渲染对象
     * @param string   待渲染的字符串
     */
    public static void renderString(HttpServletResponse response, String string) {
        try {
            response.setStatus(200);
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            response.getWriter().print(string);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 是否是Ajax异步请求
     *
     * @param request
     */
    public static boolean isAjaxRequest(HttpServletRequest request) {
        String accept = request.getHeader("accept");
        if (accept != null && accept.contains("application/json")) {
            return true;
        }

        String xRequestedWith = request.getHeader("X-Requested-With");
        if (xRequestedWith != null && xRequestedWith.contains("XMLHttpRequest")) {
            return true;
        }

        String uri = request.getRequestURI();
        if (StrUtil.containsAnyIgnoreCase(uri, ".json", ".xml")) {
            return true;
        }

        String ajax = request.getParameter("__ajax");
        return StrUtil.containsAnyIgnoreCase(ajax, "json", "xml");
    }

    /**
     * 内容编码
     *
     * @param str 内容
     * @return 编码后的内容
     */
    public static String urlEncode(String str) {
        return URLEncoder.encode(str, StandardCharsets.UTF_8);
    }

    /**
     * 获取客户端IP地址
     * 该方法首先检查一组预定义的HTTP头，以找到可能包含客户端IP地址的头
     * 如果未找到，且提供了其他头名称参数，则也会检查这些头
     *
     * @param request          HttpServletRequest对象，用于获取请求头信息
     * @param otherHeaderNames 可变长度的字符串参数，表示其他可能包含客户端IP地址的头名称
     * @return 返回客户端的IP地址如果无法找到，则返回null或空字符串（具体行为取决于实现）
     */
    public static String getClientIP(HttpServletRequest request, String... otherHeaderNames) {
        // 预定义的HTTP头数组，包含常见的携带客户端IP地址的头
        String[] headers = new String[]{"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP", "WL-Proxy-Client-IP", "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR"};

        // 如果提供了其他头名称参数，并且这些参数不为空，则将它们添加到头数组中
        if (ArrayUtil.isNotEmpty(otherHeaderNames)) {
            headers = (String[]) ArrayUtil.addAll(new String[][]{headers, otherHeaderNames});
        }

        // 调用另一个方法，根据请求和头数组来获取客户端的IP地址
        return getClientIPByHeader(request, headers);
    }

    /**
     * 通过请求头获取客户端IP地址
     * 该方法用于处理通过多个代理的情况，以确保获取到真实的客户端IP
     *
     * @param request     HttpServletRequest对象，用于获取请求头信息
     * @param headerNames 可变参数，包含可能包含客户端IP的请求头名称
     * @return 返回客户端的真实IP地址
     */
    public static String getClientIPByHeader(HttpServletRequest request, String... headerNames) {
        // 遍历请求头名称数组，尝试获取客户端IP
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            // 如果获取到的IP不是未知，则处理可能的多级代理情况并返回IP
            if (!NetUtil.isUnknown(ip)) {
                return NetUtil.getMultistageReverseProxyIp(ip);
            }
        }

        // 如果所有指定的请求头都没有有效IP，则退回到直接获取远程地址，并处理可能的多级代理情况
        String ip = request.getRemoteAddr();
        return NetUtil.getMultistageReverseProxyIp(ip);
    }
}
