package com.bank.limits.domain.repository;

import com.bank.limits.domain.model.ExpenseCategory;

import java.util.Set;

public interface AvailableCategoriesRepository {

    public Set<ExpenseCategory> getCategories();
}
