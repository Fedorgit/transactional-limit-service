package com.bank.limits.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionRequestDto(
        @JsonProperty("account_from")
        String accountFrom,
        @JsonProperty("account_to")
        String accountTo,
        @JsonProperty("currency_shortname")
        String currencyShortname,
        BigDecimal sum,
        @JsonProperty("expense_category")
        String expenseCategory,
        Instant datetime
) {
}
