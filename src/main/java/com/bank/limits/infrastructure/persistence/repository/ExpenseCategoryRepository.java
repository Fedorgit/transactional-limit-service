package com.bank.limits.infrastructure.persistence.repository;

import com.bank.limits.infrastructure.persistence.entity.ExpenseCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Integer> {
    public Optional<ExpenseCategory> findTopByName(@Size(max = 50) @NotNull String name);

    boolean existsByName(String name);
}
