package com.bank.limits.domain.model;

import com.bank.limits.domain.exception.DomainValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DomainModelsValidationTest {

    private static final Instant TRANSACTION_DATE = Instant.parse("2026-01-02T00:00:00Z");

    @Test
    void currencyIsNormalizedToUppercase() {
        assertEquals("USD", new Currency("usd").code());
    }

    @Test
    void rejectsInvalidCurrenciesAndCategories() {
        assertThrows(DomainValidationException.class, () -> new Currency("US1"));
        assertThrows(DomainValidationException.class, () -> new ExpenseCategory(" "));
        assertThrows(DomainValidationException.class, () -> new ExpenseCategory("x".repeat(51)));
        assertThrows(DomainValidationException.class, () -> new AccountNumber("١٢٣٤٥٦٧٨٩٠"));
        assertThrows(DomainValidationException.class, () -> new AccountNumber("123456789"));
        assertThrows(DomainValidationException.class, () -> new AccountNumber("123456789A"));
    }

    @Test
    void moneyRequiresCurrencyAndNonNegativeAmount() {
        assertThrows(DomainValidationException.class, () -> new Money(null, BigDecimal.ONE));
        assertThrows(DomainValidationException.class, () -> new Money(new Currency("USD"), null));
        assertThrows(
                DomainValidationException.class,
                () -> new Money(new Currency("USD"), new BigDecimal("-0.01"))
        );
    }

    @Test
    void limitRequiresMatchingCurrenciesAccountCategoryAndTimestamp() {
        Currency usd = new Currency("USD");
        ExpenseCategory category = new ExpenseCategory("product");
        assertThrows(
                DomainValidationException.class,
                () -> new Limit(
                        new Money(usd, BigDecimal.ONE),
                        new Currency("EUR"),
                        category,
                        "1234567890",
                        OffsetDateTime.parse("2026-03-01T00:00:00Z")
                )
        );
        assertThrows(
                DomainValidationException.class,
                () -> new Limit(
                        new Money(usd, BigDecimal.ONE),
                        usd,
                        category,
                        "bad",
                        OffsetDateTime.parse("2026-03-01T00:00:00Z")
                )
        );
    }

    @Test
    void exchangeRateRequiresDistinctCurrenciesPositiveRateAndDate() {
        Currency usd = new Currency("USD");
        Currency eur = new Currency("EUR");
        assertThrows(
                DomainValidationException.class,
                () -> new ExchangeRate(usd, eur, BigDecimal.ZERO, LocalDate.now())
        );
        assertThrows(
                DomainValidationException.class,
                () -> new ExchangeRate(usd, usd, BigDecimal.ONE, LocalDate.now())
        );
    }

    @Test
    void transactionRequiresPositiveAmountMatchingCurrenciesAndDate() {
        Currency usd = new Currency("USD");
        AccountNumber from = new AccountNumber("1234567890");
        AccountNumber to = new AccountNumber("1234567891");
        ExpenseCategory category = new ExpenseCategory("product");

        assertThrows(
                DomainValidationException.class,
                () -> new Transaction(
                        from, to, category, new Money(usd, BigDecimal.ZERO),
                        TRANSACTION_DATE, usd
                )
        );
        assertThrows(
                DomainValidationException.class,
                () -> new Transaction(
                        from, to, category, new Money(usd, BigDecimal.ONE),
                        null, usd
                )
        );
        assertThrows(
                DomainValidationException.class,
                () -> new Transaction(
                        from, to, category, new Money(usd, BigDecimal.ONE),
                        TRANSACTION_DATE, new Currency("EUR")
                )
        );
    }

    @Test
    void previousTransactionAndLimitStatusRequireConsistentValues() {
        Currency usd = new Currency("USD");
        assertThrows(
                DomainValidationException.class,
                () -> new PreviousTransaction(new Money(usd, BigDecimal.ONE), null)
        );
        assertThrows(
                DomainValidationException.class,
                () -> new LimitStatus(false, Optional.of(new Money(usd, BigDecimal.ONE)))
        );
        assertThrows(
                DomainValidationException.class,
                () -> new LimitStatus(true, Optional.empty())
        );
    }
}
