package com.bank.limits.domain.model;

public record ExpenseCategory(
        String category
) {

    public ExpenseCategory{
        if (category==null || category.isBlank()){
            throw new IllegalArgumentException("Expense Category can't be null");
        }
    }

}
