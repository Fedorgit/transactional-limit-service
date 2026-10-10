package com.bank.limits.infrastructure.persistence.repository;

import com.bank.limits.infrastructure.persistence.entity.ExpenseLimit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseLimitRepository extends JpaRepository<ExpenseLimit, Long> {
}
