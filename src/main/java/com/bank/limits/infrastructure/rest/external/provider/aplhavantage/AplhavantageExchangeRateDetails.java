package com.bank.limits.infrastructure.rest.external.provider.aplhavantage;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AplhavantageExchangeRateDetails(
        @JsonProperty("1. From_Currency Code")
        String fromCurrencyCode,

        @JsonProperty("3. To_Currency Code")
        String toCurrencyCode,

        @JsonProperty("5. Exchange Rate")
        BigDecimal exchangeRate,

        @JsonProperty("6. Last Refreshed")
        @JsonFormat(
                pattern = "yyyy-MM-dd HH:mm:ss",
                timezone = "UTC"
        )
        LocalDateTime lastRefreshed
) {
}
