package com.bank.limits.domain.service;

import com.bank.limits.domain.model.Limit;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LimitService {

    private final CheckAvailableCategory checkAvailableCategory;

    public void setLimit(Limit limit){

        if (!checkAvailableCategory.isAvailableCategory(limit.expenseCategory())){
            throw new IllegalArgumentException("No such this Category!");
        }

    }

}
