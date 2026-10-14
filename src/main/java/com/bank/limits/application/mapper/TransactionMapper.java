package com.bank.limits.application.mapper;

import com.bank.limits.domain.model.*;
import com.bank.limits.infrastructure.rest.dto.transaction.ExecutedTransactionResponseDto;
import com.bank.limits.infrastructure.rest.dto.transaction.TransactionRequestDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(
        componentModel = "spring"
)
public interface TransactionMapper {

    @Mapping(target = "accountFrom", source = "accountFrom")
    @Mapping(target = "accountTo", source = "accountTo")
    @Mapping(target = "expenseCategory", source = "expenseCategory")
    @Mapping(target = "transactionAmount", source = ".")
    @Mapping(target = "transactionDate", source = "datetime")
    @Mapping(target = "currency", source = "currencyShortname")
    Transaction toTransaction(TransactionRequestDto dto);

    @Mapping(target = "accountFrom", source = "dto.accountFrom")
    @Mapping(target = "accountTo", source = "dto.accountTo")
    @Mapping(target = "expenseCategory", source = "dto.expenseCategory")
    @Mapping(target = "transactionAmount", source = "dto")
    @Mapping(target = "transactionDate", source = "dto.datetime")
    @Mapping(target = "currency", source = "dto.currencyShortname")
    @Mapping(target = "isLimitExceeded", source = "limitExceeded")
    ExecutedTransactionResponseDto toExecutedTransactionResponseDto(
            TransactionRequestDto dto,
            boolean limitExceeded
    );

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "datetime", source = "dto.datetime")
    @Mapping(target = "accountFrom", source = "dto.accountFrom")
    @Mapping(target = "accountTo", source = "dto.accountTo")
    @Mapping(target = "amount", source = "dto.sum")
    @Mapping(target = "currencyCode", source = "currency")
    @Mapping(target = "expenseCategory", source = "category")
    @Mapping(target = "limitExceeded", source = "limitExceeded")
    com.bank.limits.infrastructure.persistence.entity.Transaction toEntity(
            TransactionRequestDto dto,
            com.bank.limits.infrastructure.persistence.entity.Currency currency,
            com.bank.limits.infrastructure.persistence.entity.ExpenseCategory category,
            boolean limitExceeded
    );

    default OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }

    default AccountNumber toAccountNumber(String number) {
        return new AccountNumber(number);
    }

    default Currency toCurrency(String code) {
        return new Currency(code);
    }

    default ExpenseCategory toExpenseCategory(String category) {
        return new ExpenseCategory(category);
    }

    default Money toMoney(TransactionRequestDto dto) {
        return new Money(toCurrency(dto.currencyShortname()), dto.sum());
    }
}