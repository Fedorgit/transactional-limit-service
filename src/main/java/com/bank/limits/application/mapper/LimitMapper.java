package com.bank.limits.application.mapper;

import com.bank.limits.domain.model.Currency;
import com.bank.limits.domain.model.ExpenseCategory;
import com.bank.limits.domain.model.Limit;
import com.bank.limits.domain.model.Money;
import com.bank.limits.infrastructure.persistence.entity.ExpenseLimit;
import com.bank.limits.infrastructure.rest.dto.limit.LimitResponseDto;
import com.bank.limits.infrastructure.rest.dto.limit.SetNewLimitRequestDto;
import com.bank.limits.infrastructure.persistence.repository.ExpenseCategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.OffsetDateTime;

@Mapper(componentModel = "spring")
public abstract class LimitMapper {

    @Autowired
    private ExpenseCategoryRepository expenseCategoryRepository;

    public Limit toDomain(SetNewLimitRequestDto dto) {
        Currency currency = new Currency(dto.currency_code());
        return new Limit(
                new Money(currency, dto.amount()),
                currency,
                new ExpenseCategory(dto.expense_category()),
                dto.account_from(),
                OffsetDateTime.now()
        );
    }

    @Mapping(target = "amount", source = ".")
    @Mapping(target = "currency", source = "currencyCode")
    @Mapping(target = "expenseCategory", source = "expenseCategory")
    public abstract Limit toDomain(ExpenseLimit entity);

    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "currencyCode", source = "currencyCode.code")
    @Mapping(target = "expenseCategory", source = "expenseCategory.name")
    @Mapping(target = "datetime", source = "setDatetime")
    public abstract LimitResponseDto toResponseDto(ExpenseLimit entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "setDatetime", source = "limit.setDatetime")
    @Mapping(target = "amount", source = "limit.amount.amount")
    @Mapping(target = "currencyCode", source = "limit.amount.currency")
    @Mapping(target = "accountFrom", source = "limit.accountFrom")
    @Mapping(target = "expenseCategory", source = "expenseCategoryEntity")
    protected abstract ExpenseLimit toEntity(
            Limit limit,
            com.bank.limits.infrastructure.persistence.entity.ExpenseCategory expenseCategoryEntity
    );

    public ExpenseLimit toEntity(Limit limit) {
        com.bank.limits.infrastructure.persistence.entity.ExpenseCategory categoryEntity =
                expenseCategoryRepository.findTopByName(limit.expenseCategory().category())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Expense category not found: " + limit.expenseCategory().category()
                        ));
        return toEntity(limit, categoryEntity);
    }

    public Instant toInstant(OffsetDateTime datetime) {
        return datetime == null ? null : datetime.toInstant();
    }

    public Money toMoney(com.bank.limits.infrastructure.persistence.entity.ExpenseLimit entity) {
        return new Money(toDomainCurrency(entity.getCurrencyCode()), entity.getAmount());
    }

    public Currency toDomainCurrency(
            com.bank.limits.infrastructure.persistence.entity.Currency currency
    ) {
        return new Currency(currency.getCode());
    }

    public com.bank.limits.infrastructure.persistence.entity.Currency toEntityCurrency(Currency currency) {
        return new com.bank.limits.infrastructure.persistence.entity.Currency(currency.code());
    }

    public ExpenseCategory toDomainExpenseCategory(
            com.bank.limits.infrastructure.persistence.entity.ExpenseCategory expenseCategory
    ) {
        return new ExpenseCategory(expenseCategory.getName());
    }

}