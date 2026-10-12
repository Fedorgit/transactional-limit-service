package com.bank.limits.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionRequestDto(
        String accountFrom,
        String accountTo,
        String currencyShortname,
        BigDecimal sum,
        String expenseCategory,
        Instant datetime
) {
}
