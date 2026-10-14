package com.bank.limits.infrastructure.persistence.repository;

import com.bank.limits.infrastructure.persistence.entity.ExpenseLimit;
import jakarta.persistence.LockModeType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface ExpenseLimitRepository extends JpaRepository<ExpenseLimit, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ExpenseLimit> findTopByExpenseCategory_NameOrderBySetDatetimeDesc(
            @Size(max = 50) @NotNull String expenseCategory_name
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ExpenseLimit> findTopByAccountFromAndExpenseCategory_NameAndSetDatetimeLessThanEqualOrderBySetDatetimeDesc(
            @NotNull String accountFrom,
            @Size(max = 50) @NotNull String expenseCategory_name,
            @NotNull OffsetDateTime setDatetime
    );

    Page<ExpenseLimit> findByAccountFrom(String accountFrom, Pageable pageable);
}
