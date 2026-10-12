package com.bank.limits.infrastructure.persistence.repository;

import com.bank.limits.infrastructure.persistence.entity.ExpenseCategory;
import com.bank.limits.infrastructure.persistence.entity.ExpenseLimit;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

public interface ExpenseLimitRepository extends JpaRepository<ExpenseLimit, Long> {
    public Optional<ExpenseLimit> findTopByExpenseCategory_NameOrderBySetDatetimeDesc(
            @Size(max = 50) @NotNull String expenseCategory_name
    );


}
