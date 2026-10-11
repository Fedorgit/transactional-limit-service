package com.bank.limits.domain.model;

public record Limit(
        Money amount,
        Currency currency,
        String expenseCategory
) {




}
