package com.bank.limits.infrastructure.rest.dto.transaction;

import com.bank.limits.domain.model.AccountNumber;
import com.bank.limits.domain.model.Currency;
import com.bank.limits.domain.model.ExpenseCategory;
import com.bank.limits.domain.model.Money;

import java.time.Instant;

public record ExecutedTransactionResponseDto(
        AccountNumber accountFrom,
        AccountNumber accountTo,
        ExpenseCategory expenseCategory,
        Money transactionAmount,
        Instant transactionDate,
        Currency currency,
        boolean isLimitExceeded
) {
}
