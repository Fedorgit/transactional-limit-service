package com.bank.limits.infrastructure.rest.dto.transaction;

import com.bank.limits.domain.model.AccountNumber;
import com.bank.limits.domain.model.Currency;
import com.bank.limits.domain.model.ExpenseCategory;
import com.bank.limits.domain.model.Money;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record ExecutedTransactionResponseDto(
        @NotNull @Valid
        AccountNumber accountFrom,
        @NotNull @Valid
        AccountNumber accountTo,
        @NotNull @Valid
        ExpenseCategory expenseCategory,
        @NotNull @Valid
        Money transactionAmount,
        @NotNull
        Instant transactionDate,
        @NotNull @Valid
        Currency currency,
        @JsonProperty("limit_exceeded")
        boolean isLimitExceeded
) {
}
