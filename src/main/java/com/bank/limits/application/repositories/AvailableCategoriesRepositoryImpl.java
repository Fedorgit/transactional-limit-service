package com.bank.limits.application.repositories;

import com.bank.limits.application.mapper.ExpenseCategoryMapper;
import com.bank.limits.domain.model.ExpenseCategory;
import com.bank.limits.domain.repository.AvailableCategoriesRepository;
import com.bank.limits.infrastructure.persistence.repository.ExpenseCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class AvailableCategoriesRepositoryImpl implements AvailableCategoriesRepository {

    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final ExpenseCategoryMapper expenseCategoryMapper;

    @Override
    public List<ExpenseCategory> getCategories() {
        return expenseCategoryRepository
                .findAll().stream()
                .map(expenseCategoryMapper::toDomain)
                .toList();
    }
}
