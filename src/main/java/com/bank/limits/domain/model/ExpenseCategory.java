package com.bank.limits.domain.model;

import com.bank.limits.domain.exception.DomainValidationException;

public record ExpenseCategory(
        String category
) {

    public ExpenseCategory {
        if (category == null || category.isBlank()) {
            throw new DomainValidationException("Expense category must not be blank");
        }
        if (category.length() > 50) {
            throw new DomainValidationException("Expense category must not exceed 50 characters");
        }
    }

}
