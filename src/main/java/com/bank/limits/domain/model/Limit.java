package com.bank.limits.domain.model;

import java.time.OffsetDateTime;

public record Limit(
        Money amount,
        Currency currency,
        ExpenseCategory expenseCategory,
        String accountFrom,
        OffsetDateTime setDatetime
) {

        public Limit {
                if (amount == null || amount.amount() == null || amount.amount().signum() <= 0) {
                        throw new IllegalArgumentException("Limit amount must be greater than zero");
                }
        }

}
