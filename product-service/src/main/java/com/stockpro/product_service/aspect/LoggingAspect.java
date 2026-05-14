package com.stockpro.product_service.aspect;

import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Around("execution(* com.stockpro.product_service.service..*(..))")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        log.debug("Entering {}.{} with arguments: {}", className, methodName, Arrays.toString(joinPoint.getArgs()));
        long start = System.currentTimeMillis();
        Object result = joinPoint.proceed();
        long duration = System.currentTimeMillis() - start;
        log.debug("Exiting {}.{} - duration={}ms", className, methodName, duration);
        return result;
    }

    @AfterThrowing(
            pointcut = "execution(* com.stockpro.product_service..*(..))",
            throwing = "exception")
    public void logException(JoinPoint joinPoint, Throwable exception) {
        log.error(
                "Exception in {}.{}: {}",
                joinPoint.getTarget().getClass().getSimpleName(),
                joinPoint.getSignature().getName(),
                exception.getMessage());
    }
}
