package com.bank.limits.application.repositories;

import com.bank.limits.domain.model.ExpenseCategory;
import com.bank.limits.infrastructure.persistence.repository.ExpenseCategoryRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AvailableCategoriesRepositoryImplTest {

    private final ExpenseCategoryRepository expenseCategoryRepository = mock(ExpenseCategoryRepository.class);
    private final AvailableCategoriesRepositoryImpl repository =
            new AvailableCategoriesRepositoryImpl(expenseCategoryRepository);

    @Test
    void checksOnlyWhetherTheRequestedCategoryExists() {
        ExpenseCategory category = new ExpenseCategory("product");
        when(expenseCategoryRepository.existsByName("product")).thenReturn(true);

        assertTrue(repository.contains(category));
        verify(expenseCategoryRepository).existsByName("product");
    }

    @Test
    void nullCategoryIsNotAvailable() {
        assertFalse(repository.contains(null));
    }
}
