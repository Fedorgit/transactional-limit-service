package com.bank.limits.application.util;

import com.bank.limits.infrastructure.persistence.entity.ExpenseCategory;
import com.bank.limits.infrastructure.persistence.entity.ExpenseLimit;
import com.bank.limits.infrastructure.persistence.repository.ExpenseCategoryRepository;
import com.bank.limits.infrastructure.persistence.repository.ExpenseLimitRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LimitUtilTest {

    private ExpenseCategoryRepository expenseCategoryRepository;
    private ExpenseLimitRepository expenseLimitRepository;
    private LimitUtil limitUtil;

    @BeforeEach
    void setUp() {
        expenseCategoryRepository = mock(ExpenseCategoryRepository.class);
        expenseLimitRepository = mock(ExpenseLimitRepository.class);
        limitUtil = new LimitUtil(expenseCategoryRepository, expenseLimitRepository);
    }

    @Test
    void createsLimitForExistingCategory() {
        String categoryName = "Food";
        String accountFrom = "1234567890";
        OffsetDateTime setDatetime = OffsetDateTime.parse("2026-10-14T00:00:00Z");
        ExpenseCategory category = new ExpenseCategory();
        category.setName(categoryName);
        ExpenseLimit savedLimit = new ExpenseLimit();

        when(expenseCategoryRepository.findTopByName(categoryName)).thenReturn(Optional.of(category));
        when(expenseLimitRepository.save(any(ExpenseLimit.class))).thenReturn(savedLimit);

        ExpenseLimit actualLimit = limitUtil.createLimit(accountFrom, categoryName, setDatetime);

        assertSame(savedLimit, actualLimit);
        verify(expenseCategoryRepository).findTopByName(categoryName);
        verify(expenseLimitRepository).save(org.mockito.ArgumentMatchers.argThat(limit ->
                accountFrom.equals(limit.getAccountFrom())
                        && category.equals(limit.getExpenseCategory())
                        && setDatetime.equals(limit.getSetDatetime())
                        && "USD".equals(limit.getCurrencyCode().getCode())
        ));
    }

    @Test
    void rejectsMissingCategoryWithItsNameAndDoesNotSaveLimit() {
        String categoryName = "Unknown";
        when(expenseCategoryRepository.findTopByName(categoryName)).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> limitUtil.createLimit(
                        "1234567890",
                        categoryName,
                        OffsetDateTime.parse("2026-10-14T00:00:00Z")
                )
        );

        assertEquals("Expense category not found: " + categoryName, exception.getMessage());
        verify(expenseLimitRepository, never()).save(any(ExpenseLimit.class));
    }
}
