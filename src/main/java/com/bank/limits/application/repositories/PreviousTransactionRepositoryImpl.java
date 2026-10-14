package com.bank.limits.application.repositories;

import com.bank.limits.domain.model.Currency;
import com.bank.limits.domain.model.ExpenseCategory;
import com.bank.limits.domain.model.Money;
import com.bank.limits.domain.model.PreviousTransaction;
import com.bank.limits.domain.exception.BusinessRuleException;
import com.bank.limits.domain.repository.PreviousTransactionRepository;
import com.bank.limits.infrastructure.persistence.entity.ExchangeRate;
import com.bank.limits.infrastructure.persistence.entity.Transaction;
import com.bank.limits.infrastructure.persistence.repository.ExchangeRateRepository;
import com.bank.limits.infrastructure.persistence.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class PreviousTransactionRepositoryImpl implements PreviousTransactionRepository {

    private static final String USD = "USD";
    private static final MathContext DIVISION_CONTEXT = MathContext.DECIMAL128;

    private final TransactionRepository transactionRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    @Override
    public List<PreviousTransaction> getPreviousTransactions(
            String accountFrom,
            ExpenseCategory expenseCategory,
            Instant transactionDate,
            Instant limitSetDatetime
    ) {
        LocalDate transactionLocalDate = transactionDate.atOffset(ZoneOffset.UTC).toLocalDate();
        Instant monthStart = transactionLocalDate.withDayOfMonth(1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
        Instant fromDate = limitSetDatetime.isAfter(monthStart) ? limitSetDatetime : monthStart;

        if (!fromDate.isBefore(transactionDate)) {
            return List.of();
        }

        return transactionRepository.findPreviousTransactions(
                        accountFrom,
                        expenseCategory.category(),
                        OffsetDateTime.ofInstant(fromDate, ZoneOffset.UTC),
                        OffsetDateTime.ofInstant(transactionDate, ZoneOffset.UTC)
                ).stream()
                .map(this::toPreviousTransactionInUsd)
                .toList();
    }

    private PreviousTransaction toPreviousTransactionInUsd(Transaction transaction) {
        String sourceCurrency = transaction.getCurrencyCode().getCode();
        BigDecimal amount = transaction.getAmount();

        if (!USD.equals(sourceCurrency)) {
            LocalDate transactionDate = transaction.getDatetime().withOffsetSameInstant(ZoneOffset.UTC).toLocalDate();
            ExchangeRate rate = exchangeRateRepository
                    .findLatestOnOrBefore(sourceCurrency, USD, transactionDate)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "No exchange rate available for " + sourceCurrency + " to USD on or before " + transactionDate
                    ));

            if (sourceCurrency.equals(rate.getBaseCurrency().getCode())
                    && USD.equals(rate.getTargetCurrency().getCode())) {
                amount = amount.multiply(rate.getRate());
            } else if (USD.equals(rate.getBaseCurrency().getCode())
                    && sourceCurrency.equals(rate.getTargetCurrency().getCode())) {
                amount = amount.divide(rate.getRate(), DIVISION_CONTEXT);
            } else {
                throw new BusinessRuleException("Exchange rate does not match the requested currency pair");
            }
        }

        return new PreviousTransaction(
                new Money(new Currency(USD), amount.setScale(2, RoundingMode.HALF_UP)),
                transaction.getDatetime().toInstant()
        );
    }
}
