package com.bank.limits.domain.model;

import java.util.Optional;

public record LimitStatus(
        boolean isLimitExceeded,
        Optional<Money> excessAmount
) {
}
