package com.bank.limits.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExchangeRate(
    Currency baseCurrency,
    Currency targetCurrency,
    BigDecimal rate,
    LocalDate rateDate
)
{

}
