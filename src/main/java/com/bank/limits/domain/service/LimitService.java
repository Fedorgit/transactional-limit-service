package com.bank.limits.domain.service;

import com.bank.limits.domain.model.Limit;
import com.bank.limits.domain.exception.BusinessRuleException;
import com.bank.limits.domain.exception.DomainValidationException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LimitService {

    private final CheckAvailableCategory checkAvailableCategory;

    public Limit setLimit(Limit limit){
        if (limit == null) {
            throw new DomainValidationException("Limit is required");
        }

        if (!checkAvailableCategory.isAvailableCategory(limit.expenseCategory())){
            throw new BusinessRuleException(
                    "Unknown expense category: " + limit.expenseCategory().category()
            );
        }

        if (limit.currency() == null || !"USD".equals(limit.currency().code())) {
            throw new BusinessRuleException("Expense limits must be denominated in USD");
        }

        return limit;
    }

}
