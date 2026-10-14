package com.bank.limits.infrastructure.rest.dto.limit;

import com.bank.limits.infrastructure.rest.validation.Iso4217Currency;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public record LimitResponseDto(
        @NotNull @Positive @Digits(integer = 17, fraction = 2)
        BigDecimal amount,
        @NotBlank @Iso4217Currency
        String currencyCode,
        @NotBlank @Pattern(regexp = "product|service", message = "must be product or service")
        String expenseCategory,
        @NotNull
        Instant datetime
) {
}
