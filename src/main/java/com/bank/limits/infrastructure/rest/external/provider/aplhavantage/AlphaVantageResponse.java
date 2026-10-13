package com.bank.limits.infrastructure.rest.external.provider.aplhavantage;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AlphaVantageResponse(
        @JsonProperty("Realtime Currency Exchange Rate") AplhavantageExchangeRateDetails exchangeRateDetails
) {
}
