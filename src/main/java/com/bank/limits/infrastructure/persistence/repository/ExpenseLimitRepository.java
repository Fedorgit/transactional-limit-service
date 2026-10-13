package com.bank.limits.infrastructure.persistence.repository;

import com.bank.limits.infrastructure.persistence.entity.ExpenseCategory;
import com.bank.limits.infrastructure.persistence.entity.ExpenseLimit;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExpenseLimitRepository extends JpaRepository<ExpenseLimit, Long> {
    Optional<ExpenseLimit> findTopByExpenseCategory_NameOrderBySetDatetimeDesc(
            @Size(max = 50) @NotNull String expenseCategory_name
    );

    Page<ExpenseLimit> findByAccountFrom(String accountFrom, Pageable pageable);
}
