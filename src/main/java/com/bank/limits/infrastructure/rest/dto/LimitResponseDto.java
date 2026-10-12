package com.bank.limits.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

public record LimitResponseDto(
    BigDecimal amount,
    String currencyCode,
    String expenseCategory,
    Instant datetime
) {
}
