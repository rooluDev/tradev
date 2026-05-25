package com.tradev.common.aspect;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Around("execution(* com.tradev.domain.*.controller..*(..))")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        String method = "";
        String uri = "";
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            method = request.getMethod();
            uri = request.getRequestURI();
        }

        String handler = joinPoint.getSignature().toShortString();

        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;
            log.info("[{}] {} {} - {}ms", method, uri, handler, elapsed);
            return result;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn("[{}] {} {} - {}ms - ERROR: {}", method, uri, handler, elapsed, e.getMessage());
            throw e;
        }
    }
}
