package com.bank.limits.infrastructure.logging;

import com.bank.limits.infrastructure.rest.external.dto.ExchangeRatesProperties;
import com.bank.limits.infrastructure.rest.external.provider.aplhavantage.AlphavantageService;
import org.aspectj.lang.annotation.Around;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.AspectJExpressionPointcut;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationLoggingAspectTest {

    @Test
    void logsExternalOperationsWithoutProxyingConfigurationPropertiesRecord() throws NoSuchMethodException {
        String expression = OperationLoggingAspect.class
                .getMethod("logOperation", org.aspectj.lang.ProceedingJoinPoint.class)
                .getAnnotation(Around.class)
                .value();
        AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
        pointcut.setExpression(expression);

        Method configurationAccessor = ExchangeRatesProperties.class.getMethod("baseCurrency");
        Method externalOperation = AlphavantageService.class.getMethod(
                "doRequest",
                String.class,
                List.class
        );

        assertFalse(pointcut.matches(configurationAccessor, ExchangeRatesProperties.class));
        assertTrue(pointcut.matches(externalOperation, AlphavantageService.class));
    }
}
