package com.bank.limits.domain.model;

import com.bank.limits.domain.exception.DomainValidationException;

import java.math.BigDecimal;
import java.time.Instant;

public record PreviousTransaction(
        Money transactionAmount,
        Instant transactionDate
) {
    public PreviousTransaction {
        if (transactionAmount == null) {
            throw new DomainValidationException("Previous transaction amount is required");
        }
        if (transactionAmount.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainValidationException("Previous transaction amount must be greater than zero");
        }
        if (transactionDate == null) {
            throw new DomainValidationException("Previous transaction date is required");
        }
    }
}
