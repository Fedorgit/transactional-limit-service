package com.bank.limits.domain.model;

import com.bank.limits.domain.exception.DomainValidationException;

import java.math.BigDecimal;
import java.util.Objects;
import java.time.Instant;

public record Transaction(
        AccountNumber accountFrom,
        AccountNumber accountTo,
        ExpenseCategory expenseCategory,
        Money transactionAmount,
        Instant transactionDate,
        Currency currency
){
    public Transaction {
        if (accountFrom == null || accountTo == null) {
            throw new DomainValidationException("Transaction account numbers are required");
        }
        if (expenseCategory == null) {
            throw new DomainValidationException("Transaction expense category is required");
        }
        if (transactionAmount == null) {
            throw new DomainValidationException("Transaction amount is required");
        }
        if (transactionAmount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainValidationException("Transaction amount must be greater than zero");
        }
        if (transactionDate == null) {
            throw new DomainValidationException("Transaction date is required");
        }
        if (currency == null) {
            throw new DomainValidationException("Transaction currency is required");
        }
        if (!Objects.equals(currency, transactionAmount.currency())) {
            throw new DomainValidationException("Transaction amount currency must match transaction currency");
        }
    }
}
