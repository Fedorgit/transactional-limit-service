package com.bank.limits.infrastructure.logging;

import com.bank.limits.domain.exception.DomainException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class OperationLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLoggingAspect.class);

    @Around("""
            (
                execution(* com.bank.limits.infrastructure.rest.conrtoller..*(..)) ||
                execution(* com.bank.limits.infrastructure.rest.external..*(..)) ||
                execution(* com.bank.limits.infrastructure.persistence..*(..)) ||
                execution(* com.bank.limits.application.service..*(..)) ||
                execution(* com.bank.limits.application.repositories..*(..)) ||
                execution(* com.bank.limits.application.util..*(..)) ||
                execution(* com.bank.limits.domain.service..*(..))
            ) && !within(com.bank.limits.infrastructure.rest.external.dto.ExchangeRatesProperties)
            """)
    public Object logOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String operation = joinPoint.getSignature().toShortString();
        long startedAt = System.nanoTime();
        log.debug("Starting {}", operation);

        try {
            Object result = joinPoint.proceed();
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            log.info("Completed {} in {} ms", operation, elapsedMillis);
            return result;
        } catch (DomainException exception) {
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            log.warn(
                    "Business operation {} failed with {} after {} ms",
                    operation,
                    exception.getClass().getSimpleName(),
                    elapsedMillis
            );
            throw exception;
        } catch (Throwable exception) {
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            log.error(
                    "Operation {} failed with {} after {} ms",
                    operation,
                    exception.getClass().getSimpleName(),
                    elapsedMillis
            );
            throw exception;
        }
    }
}
