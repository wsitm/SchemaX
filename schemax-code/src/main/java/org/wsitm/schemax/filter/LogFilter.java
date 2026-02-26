package org.wsitm.schemax.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.wsitm.schemax.utils.ServletUtils;

import java.io.IOException;
import java.util.Enumeration;

/**
 * 请求日志过滤器
 * <p>
 * Created by lzy on 2026/2/26 10:45
 */
@Order(1)
@Component
public class LogFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(LogFilter.class);
    private final static int MAX_OUTPUT_LENGTH = 256;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("LogFilter 初始化完成");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        long startTime = System.currentTimeMillis();

        try {
            // 只处理HTTP请求
            if (!(request instanceof HttpServletRequest httpRequest) || !(response instanceof HttpServletResponse)) {
                chain.doFilter(request, response);
                return;
            }

            // 记录请求信息
            logRequestInfo(httpRequest);

            // 继续执行过滤器链
            chain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Filter处理异常: {}", e.getMessage(), e);
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            log.debug("请求处理耗时: {} ms", duration);
        }
    }

    /**
     * 记录请求信息
     */
    private void logRequestInfo(HttpServletRequest request) {
        try {
            String reqMethod = request.getMethod();
            String uri = request.getRequestURI();
            String queryString = request.getQueryString();

            StringBuilder sb = new StringBuilder();
            Enumeration<String> paramNames = request.getParameterNames();

            if (paramNames.hasMoreElements()) {
                while (paramNames.hasMoreElements()) {
                    String name = paramNames.nextElement();
                    String[] values = request.getParameterValues(name);

                    if (values != null && values.length > 0) {
                        if (values.length == 1) {
                            sb.append(name).append("=");
                            if (values[0] != null && values[0].length() > MAX_OUTPUT_LENGTH) {
                                sb.append(values[0], 0, MAX_OUTPUT_LENGTH).append("...");
                            } else {
                                sb.append(values[0]);
                            }
                        } else {
                            sb.append(name).append("[]={");
                            for (int i = 0; i < values.length; i++) {
                                if (i > 0) {
                                    sb.append(",");
                                }
                                sb.append(values[i]);
                            }
                            sb.append("}");
                        }
                        sb.append("  ");
                    }
                }
            }

            // 打印请求日志
            log.info("\n" +
                            "Url       : {} {} {}{}\n" +
                            "Parameter : {}\n",
                    ServletUtils.getClientIP(request),
                    reqMethod,
                    uri,
                    queryString != null ? "?" + queryString : "",
                    sb.toString());

        } catch (Exception e) {
            log.error("记录请求日志异常: {}", e.getMessage());
        }
    }

    @Override
    public void destroy() {
        log.info("LogFilter 销毁");
    }
}
