package com.bank.limits.infrastructure.rest.dto.transaction;

import com.bank.limits.domain.model.AccountNumber;
import com.bank.limits.domain.model.Currency;
import com.bank.limits.domain.model.ExpenseCategory;
import com.bank.limits.domain.model.Money;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record TransactionResponseDto(
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
        @NotNull
        Boolean limitExceeded
) {
}
