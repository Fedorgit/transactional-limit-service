package com.bank.limits.domain.model;

import com.bank.limits.domain.exception.DomainValidationException;

import java.math.BigDecimal;
import java.util.Optional;

public record LimitStatus(
        boolean isLimitExceeded,
        Optional<Money> excessAmount
) {
    public LimitStatus {
        if (excessAmount == null) {
            throw new DomainValidationException("Limit excess amount result is required");
        }
        if (isLimitExceeded != excessAmount.isPresent()) {
            throw new DomainValidationException("Limit excess amount must be present only when the limit is exceeded");
        }
        excessAmount.ifPresent(amount -> {
            if (amount.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new DomainValidationException("Limit excess amount must be greater than zero");
            }
        });
    }
}
