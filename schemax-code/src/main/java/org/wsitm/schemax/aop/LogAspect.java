package org.wsitm.schemax.aop;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.wsitm.schemax.utils.ServletUtils;

import java.lang.reflect.Method;
import java.util.Enumeration;

/**
 * 日志切面
 * <p>
 * Created by lzy on 2026/2/2 16:40
 */
@Aspect
@Component
public class LogAspect {
    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);
    private final static int MAX_OUTPUT_LENGTH = 256;

    @Pointcut("@within(org.springframework.stereotype.Controller) || " +
            "@within(org.springframework.web.bind.annotation.RestController)")
    public void controllerMethod() {
        // 切入的定义方法
    }

    @Before("controllerMethod()")
    public void logRequestInfo(JoinPoint joinPoint) {

        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            HttpServletRequest request = ServletUtils.getRequest();

            String reqMethod = request.getMethod();
            String uri = request.getRequestURI();

            String className = signature.getDeclaringTypeName();
            String methodName = method.getName();

            StringBuilder sb = new StringBuilder();
            Enumeration<String> e = request.getParameterNames();
            if (e.hasMoreElements()) {
                while (e.hasMoreElements()) {
                    String name = e.nextElement();
                    String[] values = request.getParameterValues(name);
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

            //打印请求日志
            log.info("\n" +
                            "Url       : {} {} {}\n" +
                            "Method    : {}\n" +
                            "Parameter : {}\n",
                    ServletUtils.getClientIP(request), reqMethod, uri, className + "." + methodName, sb.toString());

        } catch (Exception e) {
            log.error("Exception  : {}", e.getMessage());
        }

    }

}
