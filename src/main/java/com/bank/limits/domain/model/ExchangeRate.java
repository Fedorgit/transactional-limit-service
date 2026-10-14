package com.bank.limits.domain.model;

import java.math.BigDecimal;
import com.bank.limits.domain.exception.DomainValidationException;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRate(
    Currency baseCurrency,
    Currency targetCurrency,
    BigDecimal rate,
    LocalDate rateDate
)
{
    public ExchangeRate {
        if (baseCurrency == null || targetCurrency == null) {
            throw new DomainValidationException("Exchange rate currencies are required");
        }
        if (baseCurrency.equals(targetCurrency)) {
            throw new DomainValidationException("Exchange rate currencies must be different");
        }
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainValidationException("Exchange rate must be greater than zero");
        }
        if (rateDate == null) {
            throw new DomainValidationException("Exchange rate date is required");
        }
    }
}
