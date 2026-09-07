package com.personal.urlshort.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {
    @Around("execution(* com.personal.urlshort.controller..*(..)) || " +
            "execution(* com.personal.urlshort.service..*(..))"
    )
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {

        String method = joinPoint.getSignature().toShortString();

        log.info("Request started: {}", method);

        long start = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();

            long executionTime = System.currentTimeMillis() - start;

            log.info("Request completed: {} in {} ms",
                    method, executionTime);

            return result;

        } catch (Exception e) {

            long executionTime = System.currentTimeMillis() - start;

            log.error("Request failed: {} in {} ms",
                    method, executionTime, e);

            throw e;
        }
    }
}
