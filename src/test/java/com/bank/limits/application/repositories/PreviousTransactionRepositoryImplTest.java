package com.bank.limits.application.repositories;

import com.bank.limits.domain.model.ExpenseCategory;
import com.bank.limits.domain.model.PreviousTransaction;
import com.bank.limits.infrastructure.persistence.entity.Currency;
import com.bank.limits.infrastructure.persistence.entity.ExchangeRate;
import com.bank.limits.infrastructure.persistence.entity.Transaction;
import com.bank.limits.infrastructure.persistence.repository.ExchangeRateRepository;
import com.bank.limits.infrastructure.persistence.repository.TransactionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PreviousTransactionRepositoryImplTest {

    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final ExchangeRateRepository exchangeRateRepository = mock(ExchangeRateRepository.class);
    private final PreviousTransactionRepositoryImpl repository =
            new PreviousTransactionRepositoryImpl(transactionRepository, exchangeRateRepository);

    @Test
    void queriesOnlyTransactionsSinceTheLaterOfMonthStartAndLimitStart() {
        Instant transactionDate = Instant.parse("2026-03-15T12:00:00Z");
        when(transactionRepository.findPreviousTransactions(
                "1234567890",
                "product",
                OffsetDateTime.ofInstant(Instant.parse("2026-03-10T08:00:00Z"), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(transactionDate, ZoneOffset.UTC)
        )).thenReturn(List.of());

        repository.getPreviousTransactions(
                "1234567890",
                new ExpenseCategory("product"),
                transactionDate,
                Instant.parse("2026-03-10T08:00:00Z")
        );

        verify(transactionRepository).findPreviousTransactions(
                "1234567890",
                "product",
                OffsetDateTime.ofInstant(Instant.parse("2026-03-10T08:00:00Z"), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(transactionDate, ZoneOffset.UTC)
        );
    }

    @Test
    void startsNewMonthAtFirstDayEvenWhenLimitWasSetInPreviousMonth() {
        Instant transactionDate = Instant.parse("2026-02-01T12:00:00Z");
        OffsetDateTime februaryStart = OffsetDateTime.parse("2026-02-01T00:00:00Z");
        when(transactionRepository.findPreviousTransactions(
                "1234567890",
                "product",
                februaryStart,
                OffsetDateTime.ofInstant(transactionDate, ZoneOffset.UTC)
        )).thenReturn(List.of());

        repository.getPreviousTransactions(
                "1234567890",
                new ExpenseCategory("product"),
                transactionDate,
                Instant.parse("2026-01-01T00:00:00Z")
        );

        verify(transactionRepository).findPreviousTransactions(
                "1234567890",
                "product",
                februaryStart,
                OffsetDateTime.ofInstant(transactionDate, ZoneOffset.UTC)
        );
    }

    @Test
    void doesNotQueryWhenLimitWasSetAtOrAfterCurrentTransaction() {
        Instant transactionDate = Instant.parse("2026-03-15T12:00:00Z");

        repository.getPreviousTransactions(
                "1234567890",
                new ExpenseCategory("product"),
                transactionDate,
                transactionDate
        );

        verify(transactionRepository, org.mockito.Mockito.never()).findPreviousTransactions(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void convertsPreviousTransactionToUsdUsingLatestRateOnOrBeforeItsDate() {
        Instant transactionDate = Instant.parse("2026-03-15T12:00:00Z");
        OffsetDateTime previousDatetime = OffsetDateTime.parse("2026-03-10T20:30:00+03:00");
        Transaction previousTransaction = new Transaction();
        previousTransaction.setDatetime(previousDatetime);
        previousTransaction.setAmount(new BigDecimal("100.00"));
        Currency sourceCurrency = new Currency();
        sourceCurrency.setCode("EUR");
        previousTransaction.setCurrencyCode(sourceCurrency);

        ExchangeRate fallbackRate = new ExchangeRate();
        fallbackRate.setBaseCurrency(sourceCurrency);
        Currency usdCurrency = new Currency();
        usdCurrency.setCode("USD");
        fallbackRate.setTargetCurrency(usdCurrency);
        fallbackRate.setRate(new BigDecimal("1.1000"));
        fallbackRate.setRateDate(LocalDate.parse("2026-03-09"));

        when(transactionRepository.findPreviousTransactions(
                "1234567890",
                "product",
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                OffsetDateTime.ofInstant(transactionDate, ZoneOffset.UTC)
        )).thenReturn(List.of(previousTransaction));
        when(exchangeRateRepository.findLatestOnOrBefore("EUR", "USD", LocalDate.parse("2026-03-10")))
                .thenReturn(java.util.Optional.of(fallbackRate));

        List<PreviousTransaction> previousTransactions = repository.getPreviousTransactions(
                "1234567890",
                new ExpenseCategory("product"),
                transactionDate,
                Instant.parse("2026-02-01T00:00:00Z")
        );

        assertEquals(1, previousTransactions.size());
        assertEquals("USD", previousTransactions.getFirst().transactionAmount().currency().code());
        assertEquals(new BigDecimal("110.00"), previousTransactions.getFirst().transactionAmount().amount());
        verify(exchangeRateRepository).findLatestOnOrBefore(
                "EUR", "USD", LocalDate.parse("2026-03-10")
        );
    }

    @Test
    void convertsAndRoundsPreviousTransactionsInDifferentCurrenciesBeforeSumming() {
        Instant transactionDate = Instant.parse("2026-03-15T12:00:00Z");
        OffsetDateTime fromDate = OffsetDateTime.parse("2026-03-01T00:00:00Z");
        Transaction euroTransaction = previousTransaction("EUR", "100.00", "2026-03-10T10:00:00Z");
        Transaction poundTransaction = previousTransaction("GBP", "80.00", "2026-03-11T10:00:00Z");
        ExchangeRate eurToUsd = exchangeRate("EUR", "USD", "1.1000", "2026-03-10");
        ExchangeRate usdToGbp = exchangeRate("USD", "GBP", "0.8000", "2026-03-11");

        when(transactionRepository.findPreviousTransactions(
                "1234567890",
                "product",
                fromDate,
                OffsetDateTime.ofInstant(transactionDate, ZoneOffset.UTC)
        )).thenReturn(List.of(euroTransaction, poundTransaction));
        when(exchangeRateRepository.findLatestOnOrBefore("EUR", "USD", LocalDate.parse("2026-03-10")))
                .thenReturn(java.util.Optional.of(eurToUsd));
        when(exchangeRateRepository.findLatestOnOrBefore("GBP", "USD", LocalDate.parse("2026-03-11")))
                .thenReturn(java.util.Optional.of(usdToGbp));

        List<PreviousTransaction> previousTransactions = repository.getPreviousTransactions(
                "1234567890",
                new ExpenseCategory("product"),
                transactionDate,
                Instant.parse("2026-02-01T00:00:00Z")
        );

        assertThat(previousTransactions)
                .extracting(previous -> previous.transactionAmount().amount())
                .containsExactly(new BigDecimal("110.00"), new BigDecimal("100.00"));
        assertThat(previousTransactions)
                .allSatisfy(previous ->
                        assertThat(previous.transactionAmount().currency().code()).isEqualTo("USD"));
    }

    @Test
    void reportsMissingExchangeRateForPreviousTransaction() {
        Instant transactionDate = Instant.parse("2026-03-15T12:00:00Z");
        Transaction previousTransaction = previousTransaction("EUR", "100.00", "2026-03-10T10:00:00Z");
        when(transactionRepository.findPreviousTransactions(
                "1234567890",
                "product",
                OffsetDateTime.parse("2026-03-01T00:00:00Z"),
                OffsetDateTime.ofInstant(transactionDate, ZoneOffset.UTC)
        )).thenReturn(List.of(previousTransaction));
        when(exchangeRateRepository.findLatestOnOrBefore("EUR", "USD", LocalDate.parse("2026-03-10")))
                .thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> repository.getPreviousTransactions(
                "1234567890",
                new ExpenseCategory("product"),
                transactionDate,
                Instant.parse("2026-02-01T00:00:00Z")
        ))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("No exchange rate available for EUR to USD");
    }

    private Transaction previousTransaction(String currencyCode, String amount, String datetime) {
        Transaction transaction = new Transaction();
        transaction.setDatetime(OffsetDateTime.parse(datetime));
        transaction.setAmount(new BigDecimal(amount));
        Currency currency = new Currency();
        currency.setCode(currencyCode);
        transaction.setCurrencyCode(currency);
        return transaction;
    }

    private ExchangeRate exchangeRate(String base, String target, String rate, String rateDate) {
        ExchangeRate exchangeRate = new ExchangeRate();
        Currency baseCurrency = new Currency();
        baseCurrency.setCode(base);
        Currency targetCurrency = new Currency();
        targetCurrency.setCode(target);
        exchangeRate.setBaseCurrency(baseCurrency);
        exchangeRate.setTargetCurrency(targetCurrency);
        exchangeRate.setRate(new BigDecimal(rate));
        exchangeRate.setRateDate(LocalDate.parse(rateDate));
        return exchangeRate;
    }
}
