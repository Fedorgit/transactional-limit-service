package com.bank.limits.infrastructure.persistence.repository;

import com.bank.limits.infrastructure.persistence.dto.ExceededTransactionWithLimitDto;
import com.bank.limits.infrastructure.persistence.entity.Transaction;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("""
            SELECT t
            FROM Transaction t
            JOIN FETCH t.currencyCode
            WHERE t.accountFrom = :accountFrom
              AND t.expenseCategory.name = :categoryName
              AND t.datetime >= :fromDate
              AND t.datetime < :toDate
            ORDER BY t.datetime
            """)
    List<Transaction> findPreviousTransactions(
            @Param("accountFrom") String accountFrom,
            @Param("categoryName") String categoryName,
            @Param("fromDate") OffsetDateTime fromDate,
            @Param("toDate") OffsetDateTime toDate
    );

    @Query("""
    SELECT new com.bank.limits.infrastructure.persistence.dto.ExceededTransactionWithLimitDto(
        t.id,
        t.datetime,
        t.accountFrom,
        t.accountTo,
        t.amount,
        t.currencyCode,
        t.expenseCategory,
        t.limitExceeded,
        el.amount,
        el.currencyCode,
        el.setDatetime
    )
    FROM Transaction t
    JOIN ExpenseLimit el
      ON el.accountFrom = t.accountFrom
     AND el.expenseCategory = t.expenseCategory
    WHERE t.limitExceeded = true
      AND t.accountFrom = :accountFrom
      AND el.setDatetime = (
          SELECT MAX(sub.setDatetime)
          FROM ExpenseLimit sub
          WHERE sub.accountFrom = t.accountFrom
            AND sub.expenseCategory = t.expenseCategory
            AND sub.setDatetime <= t.datetime
      )
    ORDER BY t.datetime
    """)
    Page<ExceededTransactionWithLimitDto> findExceededTransactions(String accountFrom, Pageable pageable);


}
