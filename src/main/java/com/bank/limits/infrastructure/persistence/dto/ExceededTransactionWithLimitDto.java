package com.bank.limits.infrastructure.persistence.dto;

import com.bank.limits.infrastructure.persistence.entity.Currency;
import com.bank.limits.infrastructure.persistence.entity.ExpenseCategory;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ExceededTransactionWithLimitDto(
        Long id,
        OffsetDateTime datetime,
        String accountFrom,
        String accountTo,
        BigDecimal amount,
        Currency currencyCode,
        ExpenseCategory expenseCategory,
        Boolean limitExceeded,

        BigDecimal limit_amount,
        Currency limit_currencyCode,
        OffsetDateTime limit_setDatetime
) {
}
