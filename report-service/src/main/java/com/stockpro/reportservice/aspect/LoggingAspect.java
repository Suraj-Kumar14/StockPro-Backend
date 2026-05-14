package com.stockpro.reportservice.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log =
            LoggerFactory.getLogger(LoggingAspect.class);

    @Around("execution(* com.stockpro.reportservice.service.*.*(..))")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
        boolean debugEnabled = log.isDebugEnabled();
        String signature = debugEnabled ? pjp.getSignature().toShortString() : null;
        if (debugEnabled) {
            log.debug("Entering: {}", signature);
        }
        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();
        } finally {
            if (debugEnabled) {
                log.debug("Exiting: {} ({}ms)", signature, System.currentTimeMillis() - start);
            }
        }
    }
}
