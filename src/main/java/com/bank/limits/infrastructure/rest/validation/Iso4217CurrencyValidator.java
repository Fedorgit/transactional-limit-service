package com.bank.limits.infrastructure.rest.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Currency;

public class Iso4217CurrencyValidator implements ConstraintValidator<Iso4217Currency, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            Currency.getInstance(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
