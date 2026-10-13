package com.bank.limits.infrastructure.rest.external.config;

import com.bank.limits.infrastructure.rest.external.dto.ExchangeRatesProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ExchangeRatesProperties.class)
public class PropertiesConfig {
}
