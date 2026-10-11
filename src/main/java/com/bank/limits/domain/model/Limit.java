package com.bank.limits.domain.model;

public record Limit(
        Money amount,
        Currency currency,
        ExpenseCategory expenseCategory
) {

        public Limit {
                if (amount == null || amount.amount() == null || amount.amount().signum() <= 0) {
                        throw new IllegalArgumentException("Limit amount must be greater than zero");
                }
        }

}
