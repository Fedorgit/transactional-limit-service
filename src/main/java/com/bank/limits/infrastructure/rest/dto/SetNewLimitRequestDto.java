package com.bank.limits.infrastructure.rest.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record SetNewLimitRequestDto(
        BigDecimal amount,
        String currency_code,
        String account_from,
        String expense_category
) {
}
