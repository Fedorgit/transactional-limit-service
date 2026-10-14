package com.bank.limits.domain.repository;

import com.bank.limits.domain.model.ExpenseCategory;

public interface AvailableCategoriesRepository {

    boolean contains(ExpenseCategory category);
}
