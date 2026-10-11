package com.bank.limits.domain.model;

import java.time.Instant;

public record PreviousTransaction(
        Money transactionAmount,
        Instant transactionDate
) {
}
