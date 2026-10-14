package com.bank.limits.domain.model;

import com.bank.limits.domain.exception.DomainValidationException;

import java.time.OffsetDateTime;

public record Limit(
        Money amount,
        Currency currency,
        ExpenseCategory expenseCategory,
        String accountFrom,
        OffsetDateTime setDatetime
) {

    public Limit {
        if (amount == null) {
            throw new DomainValidationException("Limit amount is required");
        }
        if (amount.amount().signum() <= 0) {
            throw new DomainValidationException("Limit amount must be greater than zero");
        }
        if (currency == null) {
            throw new DomainValidationException("Limit currency is required");
        }
        if (!currency.equals(amount.currency())) {
            throw new DomainValidationException("Limit amount currency must match limit currency");
        }
        if (expenseCategory == null) {
            throw new DomainValidationException("Limit expense category is required");
        }
        new AccountNumber(accountFrom);
        if (setDatetime == null) {
            throw new DomainValidationException("Limit set date is required");
        }
    }

}
