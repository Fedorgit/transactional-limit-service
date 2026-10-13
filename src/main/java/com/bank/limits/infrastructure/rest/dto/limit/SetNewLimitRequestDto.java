package com.bank.limits.infrastructure.rest.dto.limit;

import java.math.BigDecimal;

public record SetNewLimitRequestDto(
        BigDecimal amount,
        String currency_code,
        String account_from,
        String expense_category
) {
}
