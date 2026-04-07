package com.example.mangacatalog.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
public class LoggingAspect {

    @Pointcut("execution(* com.example.mangacatalog.service.*.*(..))")
    public void serviceMethods() {
    }

    @Around("serviceMethods()")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        String className = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        log.debug("AOP: Начало выполнения метода {}.{}()", className, methodName);

        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            log.error("AOP: Метод {}.{}() выбросил исключение: {}", className, methodName, ex.getMessage());
            throw ex;
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        log.info("AOP: Метод {}.{}() выполнен за {} мс", className, methodName, executionTime);

        return result;
    }
}