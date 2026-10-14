package com.bank.limits.application.util;

import com.bank.limits.infrastructure.persistence.entity.Currency;
import com.bank.limits.infrastructure.persistence.entity.ExpenseLimit;
import com.bank.limits.infrastructure.persistence.repository.ExpenseCategoryRepository;
import com.bank.limits.infrastructure.persistence.repository.ExpenseLimitRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
public class LimitUtil {

    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final ExpenseLimitRepository expenseLimitRepository;

    public ExpenseLimit createLimit(String accountFrom, String expenseCategory, OffsetDateTime setDatetime) {
        ExpenseLimit limit = new ExpenseLimit();
        limit.setAccountFrom(accountFrom);
        limit.setAmount(BigDecimal.valueOf(1000));
        limit.setCurrencyCode(new Currency("USD"));
        limit.setSetDatetime(setDatetime);
        limit.setExpenseCategory(
                expenseCategoryRepository.findTopByName(expenseCategory).orElseThrow(
                        () -> new EntityNotFoundException(
                                "Expense category not found: " + expenseCategory
                        )
                )
        );

        return expenseLimitRepository.save(limit);
    }


}
