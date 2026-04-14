package com.example.mangacatalog.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingAspect.class);

    @Pointcut("execution(* com.example.mangacatalog.service.*.*(..))")
    public void serviceMethods() {
    }

    @Around("serviceMethods()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        LOG.debug("AOР: Начало выполнения метода {}. {}()", className, methodName);

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            LOG.error("AOP: Метод {}. {}() выбросил исключение: {}", className, methodName, ex.getMessage());
            throw ex;
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        LOG.info("AOP: Метод {}.{}() выполнен за {} мс", className, methodName, executionTime);

        return result;
    }
}