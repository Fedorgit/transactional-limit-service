package com.bank.limits.domain.model;

import com.bank.limits.domain.exception.DomainValidationException;

public record AccountNumber(String number) {

    public AccountNumber {

        if (number == null || number.isBlank()) {
            throw new DomainValidationException("Account number must not be blank");
        }

        if (number.length() != 10) {
            throw new DomainValidationException("Account number must contain exactly 10 digits");
        }

        if (!number.matches("[0-9]{10}")) {
            throw new DomainValidationException("Account number must contain only digits");
        }
    }

}
