package com.bank.limits.domain.service;

import com.bank.limits.domain.model.ExpenseCategory;
import com.bank.limits.domain.repository.AvailableCategoriesRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CheckAvailableCategory {

    private final AvailableCategoriesRepository availableCategoriesRepository;

    public boolean isAvailableCategory(ExpenseCategory category){
        return availableCategoriesRepository.getCategories().contains(category);
    }

}
