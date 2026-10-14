package com.bank.limits.domain.service;

import com.bank.limits.domain.exception.BusinessRuleException;
import com.bank.limits.domain.model.Currency;
import com.bank.limits.domain.model.ExpenseCategory;
import com.bank.limits.domain.model.Limit;
import com.bank.limits.domain.model.Money;
import com.bank.limits.domain.repository.AvailableCategoriesRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LimitServiceTest {

    private final AvailableCategoriesRepository categoriesRepository = mock(AvailableCategoriesRepository.class);
    private final LimitService service = new LimitService(new CheckAvailableCategory(categoriesRepository));

    @Test
    void acceptsUsdLimitForAnAvailableCategory() {
        ExpenseCategory category = new ExpenseCategory("product");
        Limit limit = limit(new Currency("USD"), category);
        when(categoriesRepository.contains(category)).thenReturn(true);

        assertEquals(limit, service.setLimit(limit));
    }

    @Test
    void rejectsNonUsdLimit() {
        ExpenseCategory category = new ExpenseCategory("product");
        Limit limit = limit(new Currency("EUR"), category);
        when(categoriesRepository.contains(category)).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> service.setLimit(limit));
    }

    @Test
    void rejectsUnavailableCategoryWithBusinessException() {
        ExpenseCategory category = new ExpenseCategory("unknown");
        Limit limit = limit(new Currency("USD"), category);
        when(categoriesRepository.contains(category)).thenReturn(false);

        assertThrows(BusinessRuleException.class, () -> service.setLimit(limit));
    }

    private Limit limit(Currency currency, ExpenseCategory category) {
        return new Limit(
                new Money(currency, new BigDecimal("100.00")),
                currency,
                category,
                "1234567890",
                OffsetDateTime.parse("2026-03-01T00:00:00Z")
        );
    }
}
