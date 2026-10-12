package com.bank.limits.infrastructure.persistence.repository;

import com.bank.limits.infrastructure.persistence.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Set;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("""
    SELECT t
    FROM Transaction t
    JOIN ExpenseLimit el ON t.accountFrom = el.accountFrom
                        AND t.expenseCategory = el.expenseCategory
    WHERE t.limitExceeded = true
      AND t.currencyCode.code = 'USD'
      AND t.accountFrom = :accountFrom
      AND el.setDatetime = (
          SELECT MAX(sub.setDatetime)
          FROM ExpenseLimit sub
          WHERE sub.accountFrom = t.accountFrom
            AND sub.expenseCategory = t.expenseCategory
            AND sub.setDatetime <= t.datetime
      )
""")
    public Set<Transaction> findExpenseLimitTransaction(String accountFrom);

}
