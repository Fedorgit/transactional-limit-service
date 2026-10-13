package com.bank.limits.infrastructure.rest.external.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ExchangesFromApiDto(
        String base_currency,
        String target_currency,
        BigDecimal rate,
        OffsetDateTime rate_date
        )
{}
