package com.bank.limits.application.mapper;

import com.bank.limits.domain.model.AccountNumber;
import com.bank.limits.domain.model.Currency;
import com.bank.limits.domain.model.ExpenseCategory;
import com.bank.limits.domain.model.Money;
import com.bank.limits.domain.model.Transaction;
import com.bank.limits.infrastructure.rest.dto.transaction.ExecutedTransactionResponseDto;
import com.bank.limits.infrastructure.rest.dto.transaction.TransactionRequestDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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