package com.bank.limits.application.mapper;

import com.bank.limits.domain.model.Currency;
import com.bank.limits.domain.model.ExpenseCategory;
import com.bank.limits.domain.model.Limit;
import com.bank.limits.domain.model.Money;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.OffsetDateTime;

@Mapper(componentModel = "spring")
public interface LimitMapper {

    @Mapping(target = "amount", source = ".")
    @Mapping(target = "currency", source = "currencyCode")
    @Mapping(target = "expenseCategory", source = "expenseCategory")
    Limit toDomain(com.bank.limits.infrastructure.persistence.entity.ExpenseLimit entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "setDatetime", source = "setDatetime")
    @Mapping(target = "amount", source = "limit.amount.amount")
    @Mapping(target = "currencyCode", source = "limit.currency")
    @Mapping(target = "accountFrom", source = "accountFrom")
        @Mapping(target = "expenseCategory", source = "expenseCategoryEntity")
    com.bank.limits.infrastructure.persistence.entity.ExpenseLimit toEntity(
            Limit limit,
            String accountFrom,
            OffsetDateTime setDatetime,
            com.bank.limits.infrastructure.persistence.entity.ExpenseCategory expenseCategoryEntity
    );

    default Money toMoney(com.bank.limits.infrastructure.persistence.entity.ExpenseLimit entity) {
        return new Money(toDomainCurrency(entity.getCurrencyCode()), entity.getAmount());
    }

    default Currency toDomainCurrency(com.bank.limits.infrastructure.persistence.entity.Currency currency) {
        return new Currency(currency.getCode());
    }

    default com.bank.limits.infrastructure.persistence.entity.Currency toEntityCurrency(Currency currency) {
        return new com.bank.limits.infrastructure.persistence.entity.Currency(currency.code());
    }

    default ExpenseCategory toDomainExpenseCategory(
            com.bank.limits.infrastructure.persistence.entity.ExpenseCategory expenseCategory
    ) {
        return new ExpenseCategory(expenseCategory.getName());
    }

}