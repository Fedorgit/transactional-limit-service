package com.bank.limits.infrastructure.rest.external.dto;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "exchange-rates")
public record ExchangeRatesProperties(String baseCurrency, List<String> targetCurrency) {
}
