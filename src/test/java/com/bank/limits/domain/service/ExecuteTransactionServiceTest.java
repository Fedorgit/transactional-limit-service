package com.bank.limits.domain.service;

import com.bank.limits.domain.exception.BusinessRuleException;
import com.bank.limits.domain.exception.DomainValidationException;
import com.bank.limits.domain.model.*;
import com.bank.limits.domain.repository.AvailableCategoriesRepository;
import com.bank.limits.domain.repository.PreviousTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ExecuteTransactionServiceTest {

    private static final Currency USD = new Currency("USD");
    private static final Currency EUR = new Currency("EUR");
    private static final ExpenseCategory CATEGORY = new ExpenseCategory("product");
    private static final Instant TRANSACTION_DATE = Instant.parse("2026-03-15T12:00:00Z");

    private final AvailableCategoriesRepository categoriesRepository = mock(AvailableCategoriesRepository.class);
    private final PreviousTransactionRepository previousTransactionRepository =
            mock(PreviousTransactionRepository.class);
    private ExecuteTransactionService service;

    @BeforeEach
    void setUp() {
        service = new ExecuteTransactionService(
                new CheckAvailableCategory(categoriesRepository),
                previousTransactionRepository
        );
        when(categoriesRepository.contains(CATEGORY)).thenReturn(true);
        when(previousTransactionRepository.getPreviousTransactions(
                "1234567890",
                CATEGORY,
                TRANSACTION_DATE,
                Instant.parse("2026-03-01T00:00:00Z")
        )).thenReturn(List.of(new PreviousTransaction(
                new Money(USD, new BigDecimal("100.00")),
                Instant.parse("2026-03-10T10:00:00Z")
        )));
    }

    @Test
    void amountEqualToLimitDoesNotExceedLimitWhenRateIsReversed() {
        LimitStatus status = service.execute(
                transaction("80.00"),
                new ExchangeRate(USD, EUR, new BigDecimal("0.8000"), LocalDate.parse("2026-03-14")),
                limit("200.00")
        );

        assertFalse(status.isLimitExceeded());
        assertTrue(status.excessAmount().isEmpty());
    }

    @Test
    void amountGreaterThanLimitExceedsLimitWhenRateIsReversed() {
        LimitStatus status = service.execute(
                transaction("80.01"),
                new ExchangeRate(USD, EUR, new BigDecimal("0.8000"), LocalDate.parse("2026-03-14")),
                limit("200.00")
        );

        assertTrue(status.isLimitExceeded());
        assertEquals(new BigDecimal("0.01"), status.excessAmount().orElseThrow().amount());
        assertEquals(USD, status.excessAmount().orElseThrow().currency());
    }

    @Test
    void exactMonthlyLimitDoesNotExceedAndNextCentDoes() {
        Instant limitStart = Instant.parse("2026-03-01T00:00:00Z");
        Instant firstTransactionDate = Instant.parse("2026-03-15T12:00:00Z");
        Instant secondTransactionDate = Instant.parse("2026-03-15T12:01:00Z");
        stubPreviousTransactions(firstTransactionDate, limitStart, "999.99");
        stubPreviousTransactions(secondTransactionDate, limitStart, "1000.00");

        LimitStatus atLimit = service.execute(
                transaction("0.01", firstTransactionDate, USD),
                null,
                limit("1000.00", limitStart)
        );
        LimitStatus oneCentOver = service.execute(
                transaction("0.01", secondTransactionDate, USD),
                null,
                limit("1000.00", limitStart)
        );

        assertFalse(atLimit.isLimitExceeded());
        assertTrue(atLimit.excessAmount().isEmpty());
        assertTrue(oneCentOver.isLimitExceeded());
        assertEquals(new BigDecimal("0.01"), oneCentOver.excessAmount().orElseThrow().amount());
        assertEquals(USD, oneCentOver.excessAmount().orElseThrow().currency());
    }

    @Test
    void limitChangeDuringMonthUsesTheLimitEffectiveAtEachTransactionDate() {
        Instant oldLimitStart = Instant.parse("2026-04-01T00:00:00Z");
        Instant newLimitStart = Instant.parse("2026-04-10T00:00:00Z");
        Instant beforeChangeDate = Instant.parse("2026-04-09T12:00:00Z");
        Instant afterChangeDate = Instant.parse("2026-04-10T12:00:00Z");
        stubPreviousTransactions(beforeChangeDate, oldLimitStart, "999.99");
        stubPreviousTransactions(afterChangeDate, newLimitStart, "0.00");

        LimitStatus beforeChange = service.execute(
                transaction("0.01", beforeChangeDate, USD),
                null,
                limit("1000.00", oldLimitStart)
        );
        LimitStatus afterChange = service.execute(
                transaction("0.01", afterChangeDate, USD),
                null,
                limit("2000.00", newLimitStart)
        );

        assertFalse(beforeChange.isLimitExceeded());
        assertTrue(beforeChange.excessAmount().isEmpty());
        assertFalse(afterChange.isLimitExceeded());
        assertTrue(afterChange.excessAmount().isEmpty());
        verify(previousTransactionRepository).getPreviousTransactions(
                eq("1234567890"),
                eq(CATEGORY),
                eq(beforeChangeDate),
                eq(oldLimitStart)
        );
        verify(previousTransactionRepository).getPreviousTransactions(
                eq("1234567890"),
                eq(CATEGORY),
                eq(afterChangeDate),
                eq(newLimitStart)
        );
    }

    @Test
    void newCalendarMonthStartsAccumulationFromZero() {
        Instant januaryLimitStart = Instant.parse("2026-01-01T00:00:00Z");
        Instant januaryTransactionDate = Instant.parse("2026-01-31T23:00:00Z");
        Instant februaryLimitStart = Instant.parse("2026-02-01T00:00:00Z");
        Instant februaryTransactionDate = Instant.parse("2026-02-01T00:01:00Z");
        stubPreviousTransactions(januaryTransactionDate, januaryLimitStart, "1000.00");
        stubPreviousTransactions(februaryTransactionDate, februaryLimitStart, "0.00");

        LimitStatus januaryStatus = service.execute(
                transaction("0.01", januaryTransactionDate, USD),
                null,
                limit("1000.00", januaryLimitStart)
        );
        LimitStatus februaryStatus = service.execute(
                transaction("100.00", februaryTransactionDate, USD),
                null,
                limit("1000.00", februaryLimitStart)
        );

        assertTrue(januaryStatus.isLimitExceeded());
        assertEquals(new BigDecimal("0.01"), januaryStatus.excessAmount().orElseThrow().amount());
        assertFalse(februaryStatus.isLimitExceeded());
        assertTrue(februaryStatus.excessAmount().isEmpty());
        verify(previousTransactionRepository).getPreviousTransactions(
                eq("1234567890"),
                eq(CATEGORY),
                eq(februaryTransactionDate),
                eq(februaryLimitStart)
        );
    }

    @Test
    void rejectsExchangeRateForDifferentCurrencies() {
        ExchangeRate unrelatedRate = new ExchangeRate(
                new Currency("GBP"),
                new Currency("JPY"),
                new BigDecimal("100.0000"),
                LocalDate.parse("2026-03-14")
        );

        org.junit.jupiter.api.Assertions.assertThrows(
                BusinessRuleException.class,
                () -> service.execute(transaction("80.00"), unrelatedRate, limit("200.00"))
        );
    }

    @Test
    void rejectsExchangeRateDatedAfterTheTransaction() {
        ExchangeRate futureRate = new ExchangeRate(
                USD,
                EUR,
                new BigDecimal("0.8000"),
                LocalDate.parse("2026-03-16")
        );

        org.junit.jupiter.api.Assertions.assertThrows(
                BusinessRuleException.class,
                () -> service.execute(transaction("80.00"), futureRate, limit("200.00"))
        );
    }

    @Test
    void rejectsLimitForDifferentAccount() {
        Limit otherAccountLimit = new Limit(
                new Money(USD, new BigDecimal("200.00")),
                USD,
                CATEGORY,
                "1234567899",
                OffsetDateTime.parse("2026-03-01T00:00:00Z")
        );

        org.junit.jupiter.api.Assertions.assertThrows(
                BusinessRuleException.class,
                () -> service.execute(
                        transaction("80.00"),
                        new ExchangeRate(USD, EUR, new BigDecimal("0.8000"), LocalDate.parse("2026-03-14")),
                        otherAccountLimit
                )
        );
    }

    @Test
    void executesTransactionInLimitCurrencyWithoutExchangeRate() {
        stubPreviousTransactions(TRANSACTION_DATE, Instant.parse("2026-03-01T00:00:00Z"), "0.00");

        LimitStatus status = service.execute(
                transaction("100.00", TRANSACTION_DATE, USD),
                null,
                limit("100.00")
        );

        assertThat(status.isLimitExceeded()).isFalse();
        assertThat(status.excessAmount()).isEmpty();
    }

    @Test
    void transactionBelowLimitDoesNotExceedLimit() {
        stubPreviousTransactions(TRANSACTION_DATE, Instant.parse("2026-03-01T00:00:00Z"), "0.00");

        LimitStatus status = service.execute(
                transaction("99.99", TRANSACTION_DATE, USD),
                null,
                limit("100.00")
        );

        assertThat(status.isLimitExceeded()).isFalse();
        assertThat(status.excessAmount()).isEmpty();
    }

    @Test
    void convertsForeignCurrencyAndRoundsHalfUpBeforeComparingWithLimit() {
        stubPreviousTransactions(TRANSACTION_DATE, Instant.parse("2026-03-01T00:00:00Z"), "0.00");
        ExchangeRate usdToEur = new ExchangeRate(
                USD, EUR, new BigDecimal("0.8000"), LocalDate.parse("2026-03-14")
        );

        LimitStatus status = service.execute(
                transaction("80.02", TRANSACTION_DATE, EUR),
                usdToEur,
                limit("100.01")
        );

        assertThat(status.isLimitExceeded()).isTrue();
        assertThat(status.excessAmount()).get()
                .extracting(Money::amount)
                .isEqualTo(new BigDecimal("0.02"));
    }

    @Test
    void rejectsZeroAndNegativeTransactionAmounts() {
        assertThatThrownBy(() -> transaction("0.00"))
                .isInstanceOf(DomainValidationException.class);
        assertThatThrownBy(() -> transaction("-0.01"))
                .isInstanceOf(DomainValidationException.class);
    }

    @Test
    void rejectsUnknownExpenseCategory() {
        ExpenseCategory unknownCategory = new ExpenseCategory("unknown");
        Transaction unknownCategoryTransaction = new Transaction(
                new AccountNumber("1234567890"),
                new AccountNumber("1234567891"),
                unknownCategory,
                new Money(EUR, new BigDecimal("10.00")),
                TRANSACTION_DATE,
                EUR
        );
        when(categoriesRepository.contains(unknownCategory)).thenReturn(false);
        Limit unknownCategoryLimit = new Limit(
                new Money(USD, new BigDecimal("100.00")),
                USD,
                unknownCategory,
                "1234567890",
                OffsetDateTime.parse("2026-03-01T00:00:00Z")
        );

        assertThatThrownBy(() -> service.execute(
                unknownCategoryTransaction,
                new ExchangeRate(USD, EUR, new BigDecimal("0.8000"), LocalDate.parse("2026-03-14")),
                unknownCategoryLimit
        ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Unknown expense category: unknown");
    }

    @Test
    void rejectsMissingOrMismatchedExchangeRate() {
        assertThatThrownBy(() -> service.execute(
                transaction("10.00"),
                null,
                limit("100.00")
        ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Exchange rate does not match");

        ExchangeRate unrelatedRate = new ExchangeRate(
                new Currency("GBP"),
                new Currency("JPY"),
                BigDecimal.ONE,
                LocalDate.parse("2026-03-14")
        );
        assertThatThrownBy(() -> service.execute(
                transaction("10.00"),
                unrelatedRate,
                limit("100.00")
        ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Exchange rate does not match");
    }

    @Test
    void rejectsPreviousTransactionsNotConvertedToTheLimitCurrency() {
        PreviousTransaction previousInEur = new PreviousTransaction(
                new Money(EUR, new BigDecimal("10.00")),
                TRANSACTION_DATE.minusSeconds(60)
        );
        when(previousTransactionRepository.getPreviousTransactions(
                "1234567890",
                CATEGORY,
                TRANSACTION_DATE,
                Instant.parse("2026-03-01T00:00:00Z")
        )).thenReturn(List.of(previousInEur));
        when(categoriesRepository.contains(CATEGORY)).thenReturn(true);

        assertThatThrownBy(() -> service.execute(
                transaction("10.00", TRANSACTION_DATE, USD),
                null,
                limit("100.00")
        ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Previous transaction amounts must be in USD");
    }

    private Transaction transaction(String amount) {
        return transaction(amount, TRANSACTION_DATE, EUR);
    }

    private Transaction transaction(String amount, Instant transactionDate, Currency currency) {
        return new Transaction(
                new AccountNumber("1234567890"),
                new AccountNumber("1234567891"),
                CATEGORY,
                new Money(currency, new BigDecimal(amount)),
                transactionDate,
                currency
        );
    }

    private Limit limit(String amount) {
        return limit(amount, Instant.parse("2026-03-01T00:00:00Z"));
    }

    private Limit limit(String amount, Instant setDate) {
        return new Limit(
                new Money(USD, new BigDecimal(amount)),
                USD,
                CATEGORY,
                "1234567890",
                OffsetDateTime.ofInstant(setDate, java.time.ZoneOffset.UTC)
        );
    }

    private void stubPreviousTransactions(Instant transactionDate, Instant limitStart, String amount) {
        List<PreviousTransaction> previousTransactions = amount.equals("0.00")
                ? List.of()
                : List.of(new PreviousTransaction(
                new Money(USD, new BigDecimal(amount)),
                transactionDate.minusSeconds(60)
        ));
        when(previousTransactionRepository.getPreviousTransactions(
                "1234567890",
                CATEGORY,
                transactionDate,
                limitStart
        )).thenReturn(previousTransactions);
    }
}
