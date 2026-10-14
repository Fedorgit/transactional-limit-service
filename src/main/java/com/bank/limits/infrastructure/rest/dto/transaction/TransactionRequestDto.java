package com.bank.limits.infrastructure.rest.dto.transaction;

import com.bank.limits.infrastructure.rest.validation.Iso4217Currency;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionRequestDto(
        @JsonAlias("accountFrom")
        @NotBlank
        @Pattern(regexp = "[0-9]{10}", message = "must contain exactly 10 digits")
        String accountFrom,
        @JsonAlias("accountTo")
        @NotBlank
        @Pattern(regexp = "[0-9]{10}", message = "must contain exactly 10 digits")
        String accountTo,
        @JsonAlias("currencyShortname")
        @NotBlank
        @Iso4217Currency
        String currencyShortname,
        @NotNull
        @Positive
        @Digits(integer = 17, fraction = 2)
        BigDecimal sum,
        @JsonAlias("expenseCategory")
        @NotBlank
        @Pattern(regexp = "product|service", message = "must be product or service")
        String expenseCategory,
        @NotNull
        Instant datetime
) {
}
