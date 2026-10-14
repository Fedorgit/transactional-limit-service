package com.bank.limits.application.repositories;

import com.bank.limits.domain.model.ExpenseCategory;
import com.bank.limits.domain.repository.AvailableCategoriesRepository;
import com.bank.limits.infrastructure.persistence.repository.ExpenseCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AvailableCategoriesRepositoryImpl implements AvailableCategoriesRepository {

    private final ExpenseCategoryRepository expenseCategoryRepository;

    @Override
    public boolean contains(ExpenseCategory category) {
        return category != null
                && expenseCategoryRepository.existsByName(category.category());
    }
}
