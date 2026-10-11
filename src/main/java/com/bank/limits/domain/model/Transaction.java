package com.bank.limits.domain.model;

import java.time.Instant;

public record Transaction(
        AccountNumber accountFrom,
        AccountNumber accountTo,
        ExpenseCategory expenseCategory,
        Money transactionAmount,
        Instant transactionDate,
        Currency currency
){

}
