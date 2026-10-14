package com.bank.limits.domain.model;

import com.bank.limits.domain.exception.DomainValidationException;

import java.math.BigDecimal;

public record Money(
        Currency currency,
        BigDecimal amount
) {

    public Money{

        if (currency == null) {
            throw new DomainValidationException("Money currency is required");
        }

        if (amount == null) {
            throw new DomainValidationException("Money amount is required");
        }

        if (amount.signum() < 0) {
            throw new DomainValidationException("Money amount must not be negative");
        }
    }


}
