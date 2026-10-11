package com.bank.limits.domain.service;

import com.bank.limits.domain.model.*;
import com.bank.limits.domain.repository.AvailableCategoriesRepository;
import com.bank.limits.domain.repository.PreviousTransactionRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@RequiredArgsConstructor
public class ExecuteTransactionService {

    private final CheckAvailableCategory checkAvailableCategory;
    private final PreviousTransactionRepository previousTransactionRepository;

    public LimitStatus execute(
            Transaction transaction,
            ExchangeRate exchangeRate,
            Limit limit
    ){

        if(transaction.transactionAmount().amount().compareTo(BigDecimal.ZERO)<=0){
            throw new IllegalArgumentException("Money can't be 0 or less then 0!");
        }

        if (!checkAvailableCategory.isAvailableCategory(transaction.expenseCategory())){
            throw new IllegalArgumentException("No such this Category!");
        }


        Money currentTransactionAmount =
                !isCurrencyEquals(transaction.transactionAmount(),limit.amount()) ?
                        convertCurrency(transaction.transactionAmount(),exchangeRate) :
                        transaction.transactionAmount();

        Optional<BigDecimal> previousTransactionsTotalAmount =
                previousTransactionRepository.getPreviousTransactions().stream()
                .map(PreviousTransaction::transactionAmount)
                .map(Money::amount)
                .reduce(BigDecimal::add);

        Money previousTransactionAmount = previousTransactionsTotalAmount.map(bigDecimal -> new Money(
                limit.currency(),
                bigDecimal.add(currentTransactionAmount.amount())
        )).orElse(currentTransactionAmount);

        boolean isLimitExceeded = previousTransactionAmount.amount().compareTo(limit.amount().amount()) > 0;

        if (isLimitExceeded) {
            Money excess = calculateLimitExcess(limit, previousTransactionAmount);
            return new LimitStatus(true, Optional.of(excess));
        }
        else {
            return new LimitStatus(false, Optional.empty());
        }
    }

    private Money calculateLimitExcess(Limit limit, Money money){
        return new Money(
                limit.currency(),
                money.amount().subtract(limit.amount().amount())
                );
    }

    private Money convertCurrency(Money money, ExchangeRate exchangeRate) {
        if (!money.currency().equals(exchangeRate.baseCurrency())) {
            throw new IllegalArgumentException("Money currency must match the exchange rate base currency");
        }

        BigDecimal convertedAmount = money.amount()
                .multiply(exchangeRate.rate())
                .setScale(2, RoundingMode.HALF_UP);

        return new Money(exchangeRate.targetCurrency(), convertedAmount);
    }

    private boolean isCurrencyEquals(Money base, Money target){
        return base.currency().code().equals(target.currency().code());
    }




}
