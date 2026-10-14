package com.bank.limits.domain.model;

import com.bank.limits.domain.exception.DomainValidationException;

import java.util.Locale;

public record Currency(
        String code
) {

    public Currency{
        if (code == null || code.isBlank()) {
            throw new DomainValidationException("Currency code must not be blank");
        }

        code = code.toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z]{3}")) {
            throw new DomainValidationException("Currency code must contain exactly 3 letters");
        }
    }
}
