package com.bank.limits.domain.repository;

import com.bank.limits.domain.model.ExpenseCategory;

import java.util.List;

public interface AvailableCategoriesRepository {

    public List<ExpenseCategory> getCategories();
}
