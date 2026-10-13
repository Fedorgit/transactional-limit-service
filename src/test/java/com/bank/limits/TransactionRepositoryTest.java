package com.bank.limits;

import com.bank.limits.infrastructure.persistence.dto.ExceededTransactionWithLimitDto;
import com.bank.limits.infrastructure.persistence.entity.Currency;
import com.bank.limits.infrastructure.persistence.entity.ExpenseCategory;
import com.bank.limits.infrastructure.persistence.entity.ExpenseLimit;
import com.bank.limits.infrastructure.persistence.entity.Transaction;
import com.bank.limits.infrastructure.persistence.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest
@ActiveProfiles("test")
public class TransactionRepositoryTest {

    private static final String ACCOUNT_FROM = "1234567890";
    private static final String OTHER_ACCOUNT = "9876543210";
    private static final OffsetDateTime LIMIT_SET_AT =
            OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private TransactionRepository transactionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void findExceededTransactionsReturnsTransactionsWithLatestApplicableLimitInDatetimeOrder() {
        Currency usd = new Currency();
        usd.setCode("USD");
        entityManager.persist(usd);

        Currency eur = new Currency();
        eur.setCode("EUR");
        entityManager.persist(eur);

        ExpenseCategory food = saveCategory("Food");
        ExpenseCategory travel = saveCategory("Travel");
        ExpenseCategory uncategorized = saveCategory("Other");

        OffsetDateTime nextLimitSetAt = LIMIT_SET_AT.plusDays(3);
        saveLimit(ACCOUNT_FROM, food, LIMIT_SET_AT, "100.00", usd);
        saveLimit(ACCOUNT_FROM, food, nextLimitSetAt, "150.00", usd);
        saveLimit(ACCOUNT_FROM, travel, LIMIT_SET_AT.plusDays(1), "75.00", eur);
        saveLimit(OTHER_ACCOUNT, food, LIMIT_SET_AT, "200.00", usd);

        Transaction latestLimitTransaction = saveTransaction(
                ACCOUNT_FROM, food, nextLimitSetAt.plusDays(1), true, usd
        );
        Transaction earlierTransaction = saveTransaction(
                ACCOUNT_FROM, food, LIMIT_SET_AT.plusDays(1), true, usd
        );
        Transaction otherCategoryTransaction = saveTransaction(
                ACCOUNT_FROM, travel, LIMIT_SET_AT.plusDays(2), true, eur
        );
        saveTransaction(ACCOUNT_FROM, food, LIMIT_SET_AT.minusDays(1), true, usd);
        saveTransaction(ACCOUNT_FROM, food, nextLimitSetAt.plusDays(2), false, usd);
        saveTransaction(OTHER_ACCOUNT, food, nextLimitSetAt.plusDays(2), true, usd);
        saveTransaction(ACCOUNT_FROM, uncategorized, nextLimitSetAt.plusDays(2), true, usd);

        Page<ExceededTransactionWithLimitDto> firstPage =
                transactionRepository.findExceededTransactions(ACCOUNT_FROM, PageRequest.of(0, 2));
        Page<ExceededTransactionWithLimitDto> secondPage =
                transactionRepository.findExceededTransactions(ACCOUNT_FROM, PageRequest.of(1, 2));

        assertThat(firstPage.getContent())
                .extracting(this::transactionDetails)
                .containsExactly(
                        tuple(
                                earlierTransaction.getId(),
                                LIMIT_SET_AT.plusDays(1),
                                ACCOUNT_FROM,
                                OTHER_ACCOUNT,
                                new BigDecimal("10.00"),
                                "USD",
                                "Food",
                                true,
                                new BigDecimal("100.00"),
                                "USD",
                                LIMIT_SET_AT
                        ),
                        tuple(
                                otherCategoryTransaction.getId(),
                                LIMIT_SET_AT.plusDays(2),
                                ACCOUNT_FROM,
                                OTHER_ACCOUNT,
                                new BigDecimal("10.00"),
                                "EUR",
                                "Travel",
                                true,
                                new BigDecimal("75.00"),
                                "EUR",
                                LIMIT_SET_AT.plusDays(1)
                        )
                );
        assertThat(firstPage.getNumber()).isZero();
        assertThat(firstPage.getSize()).isEqualTo(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.hasNext()).isTrue();

        assertThat(secondPage.getContent())
                .extracting(this::transactionDetails)
                .containsExactly(
                        tuple(
                                latestLimitTransaction.getId(),
                                nextLimitSetAt.plusDays(1),
                                ACCOUNT_FROM,
                                OTHER_ACCOUNT,
                                new BigDecimal("10.00"),
                                "USD",
                                "Food",
                                true,
                                new BigDecimal("150.00"),
                                "USD",
                                nextLimitSetAt
                        )
                );
        assertThat(secondPage.getNumber()).isEqualTo(1);
        assertThat(secondPage.getTotalElements()).isEqualTo(3);
        assertThat(secondPage.hasNext()).isFalse();
    }

    private ExpenseCategory saveCategory(String name) {
        ExpenseCategory category = new ExpenseCategory();
        category.setName(name);
        entityManager.persist(category);
        return category;
    }

    private Tuple transactionDetails(ExceededTransactionWithLimitDto transaction) {
        return tuple(
                transaction.id(),
                transaction.datetime(),
                transaction.accountFrom(),
                transaction.accountTo(),
                transaction.amount(),
                transaction.currencyCode().getCode(),
                transaction.expenseCategory().getName(),
                transaction.limitExceeded(),
                transaction.limit_amount(),
                transaction.limit_currencyCode().getCode(),
                transaction.limit_setDatetime()
        );
    }

    private void saveLimit(
            String accountFrom,
            ExpenseCategory category,
            OffsetDateTime setDatetime,
            String amount,
            Currency currency
    ) {
        ExpenseLimit expenseLimit = new ExpenseLimit();
        expenseLimit.setAccountFrom(accountFrom);
        expenseLimit.setExpenseCategory(category);
        expenseLimit.setSetDatetime(setDatetime);
        expenseLimit.setAmount(new BigDecimal(amount));
        expenseLimit.setCurrencyCode(currency);
        entityManager.persist(expenseLimit);
    }

    private Transaction saveTransaction(
            String accountFrom,
            ExpenseCategory category,
            OffsetDateTime datetime,
            boolean limitExceeded,
            Currency currency
    ) {
        Transaction transaction = new Transaction();
        transaction.setAccountFrom(accountFrom);
        transaction.setAccountTo(OTHER_ACCOUNT);
        transaction.setDatetime(datetime);
        transaction.setAmount(new BigDecimal("10.00"));
        transaction.setCurrencyCode(currency);
        transaction.setExpenseCategory(category);
        transaction.setLimitExceeded(limitExceeded);
        entityManager.persist(transaction);
        return transaction;
    }

}
