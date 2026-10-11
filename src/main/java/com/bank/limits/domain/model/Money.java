package com.bank.limits.domain.model;

import java.math.BigDecimal;

public record Money(
        Currency currency,
        BigDecimal amount
) {

    public Money{

        if (amount==null){
            throw new IllegalArgumentException("Money can't be null!");
        }

        if (amount.signum() == -1){
            throw new IllegalArgumentException("Money can't be negative!");
        }
    }


}
