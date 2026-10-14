package com.bank.limits.infrastructure.rest.dto.limit;

import com.bank.limits.infrastructure.rest.validation.Iso4217Currency;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record SetNewLimitRequestDto(
        @NotNull
        @Positive
        @Digits(integer = 17, fraction = 2)
        BigDecimal amount,
        @NotBlank
        @Iso4217Currency
        String currency_code,
        @NotBlank
        @Pattern(regexp = "[0-9]{10}", message = "must contain exactly 10 digits")
        String account_from,
        @NotBlank
        @Pattern(regexp = "product|service", message = "must be product or service")
        String expense_category
) {
}
