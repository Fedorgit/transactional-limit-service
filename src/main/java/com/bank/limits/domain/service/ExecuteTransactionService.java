package com.bank.limits.domain.service;

import com.bank.limits.domain.exception.BusinessRuleException;
import com.bank.limits.domain.exception.DomainValidationException;
import com.bank.limits.domain.model.*;
import com.bank.limits.domain.repository.PreviousTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ExecuteTransactionService {

    private static final Logger log = LoggerFactory.getLogger(ExecuteTransactionService.class);
    private static final MathContext DIVISION_CONTEXT = MathContext.DECIMAL128;

    private final CheckAvailableCategory checkAvailableCategory;
    private final PreviousTransactionRepository previousTransactionRepository;

    public LimitStatus execute(
            Transaction transaction,
            ExchangeRate exchangeRate,
            Limit limit
    ) {
        if (transaction == null) {
            throw new DomainValidationException("Transaction is required");
        }
        if (limit == null) {
            throw new DomainValidationException("Limit is required");
        }
        if (!transaction.accountFrom().number().equals(limit.accountFrom())
                || !transaction.expenseCategory().equals(limit.expenseCategory())) {
            throw new BusinessRuleException("Transaction must be evaluated against its account and category limit");
        }
        if (transaction.transactionDate().isBefore(limit.setDatetime().toInstant())) {
            throw new BusinessRuleException("Transaction date must not be earlier than the applicable limit");
        }

        if (!checkAvailableCategory.isAvailableCategory(transaction.expenseCategory())) {
            throw new BusinessRuleException(
                    "Unknown expense category: " + transaction.expenseCategory().category()
            );
        }


        Money currentTransactionAmount = convertCurrency(
                transaction.transactionAmount(),
                limit.currency(),
                exchangeRate,
                transaction.transactionDate()
        );

        List<PreviousTransaction> previousTransactions = previousTransactionRepository.getPreviousTransactions(
                transaction.accountFrom().number(),
                transaction.expenseCategory(),
                transaction.transactionDate(),
                limit.setDatetime().toInstant()
        );

        BigDecimal previousTransactionsTotalAmount = previousTransactions.stream()
                .map(PreviousTransaction::transactionAmount)
                .map(previousAmount -> {
                    if (!limit.currency().equals(previousAmount.currency())) {
                        throw new BusinessRuleException(
                                "Previous transaction amounts must be in " + limit.currency().code()
                        );
                    }
                    return previousAmount.amount();
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Money totalTransactionAmount = new Money(
                limit.currency(),
                previousTransactionsTotalAmount.add(currentTransactionAmount.amount())
        );

        boolean isLimitExceeded = totalTransactionAmount.amount().compareTo(limit.amount().amount()) > 0;
        log.info(
                "Transaction limit evaluated: currency={}, limitExceeded={}",
                limit.currency().code(),
                isLimitExceeded
        );

        if (isLimitExceeded) {
            Money excess = calculateLimitExcess(limit, totalTransactionAmount);
            return new LimitStatus(true, Optional.of(excess));
        } else {
            return new LimitStatus(false, Optional.empty());
        }
    }

    private Money calculateLimitExcess(Limit limit, Money money) {
        return new Money(
                limit.currency(),
                money.amount().subtract(limit.amount().amount())
        );
    }

    private Money convertCurrency(
            Money money,
            Currency targetCurrency,
            ExchangeRate exchangeRate,
            Instant transactionDate
    ) {
        if (money.currency().equals(targetCurrency)) {
            return new Money(targetCurrency, money.amount());
        }

        if (exchangeRate == null) {
            throw new BusinessRuleException(
                    "Exchange rate does not match the transaction and limit currencies"
            );
        }
        if (exchangeRate.rateDate().isAfter(transactionDate.atOffset(ZoneOffset.UTC).toLocalDate())) {
            throw new BusinessRuleException("Exchange rate must not be later than the transaction date");
        }

        BigDecimal convertedAmount;
        if (money.currency().equals(exchangeRate.baseCurrency())
                && targetCurrency.equals(exchangeRate.targetCurrency())) {
            convertedAmount = money.amount().multiply(exchangeRate.rate());
        } else if (targetCurrency.equals(exchangeRate.baseCurrency())
                && money.currency().equals(exchangeRate.targetCurrency())) {
            convertedAmount = money.amount().divide(exchangeRate.rate(), DIVISION_CONTEXT);
        } else {
            throw new BusinessRuleException(
                    "Exchange rate does not match the transaction and limit currencies"
            );
        }

        Money converted = new Money(targetCurrency, convertedAmount.setScale(2, RoundingMode.HALF_UP));
        if (!converted.currency().equals(targetCurrency)) {
            throw new BusinessRuleException("Converted transaction currency must match the limit currency");
        }
        return converted;
    }
}
