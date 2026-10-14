package com.bank.limits.infrastructure.rest.dto.transaction;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionRequestDto(
        @JsonAlias("accountFrom")
        String accountFrom,
        @JsonAlias("accountTo")
        String accountTo,
        @JsonAlias("currencyShortname")
        String currencyShortname,
        BigDecimal sum,
        @JsonAlias("expenseCategory")
        String expenseCategory,
        Instant datetime
) {
}
