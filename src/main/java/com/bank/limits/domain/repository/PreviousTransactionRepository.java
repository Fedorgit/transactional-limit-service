package com.bank.limits.domain.repository;

import com.bank.limits.domain.model.PreviousTransaction;
import com.bank.limits.domain.model.ExpenseCategory;

import java.time.Instant;
import java.util.List;

public interface PreviousTransactionRepository {

    /**
     * Returns only prior transactions for the account and category, within the transaction month
     * and on or after the effective limit timestamp. Returned amounts are converted to USD.
     */
    List<PreviousTransaction> getPreviousTransactions(
            String accountFrom,
            ExpenseCategory expenseCategory,
            Instant transactionDate,
            Instant limitSetDatetime
    );

}
