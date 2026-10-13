package com.bank.limits;

import com.bank.limits.infrastructure.persistence.entity.Currency;
import com.bank.limits.infrastructure.persistence.entity.ExpenseCategory;
import com.bank.limits.infrastructure.persistence.entity.ExpenseLimit;
import com.bank.limits.infrastructure.persistence.entity.Transaction;
import com.bank.limits.infrastructure.persistence.repository.TransactionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

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
    void findExpenseLimitTransactionFiltersByAccountCurrencyExceededFlagAndApplicableLimit() {
        Currency usd = new Currency();
        usd.setCode("USD");
        entityManager.persist(usd);

        Currency eur = new Currency();
        eur.setCode("EUR");
        entityManager.persist(eur);

        ExpenseCategory category = new ExpenseCategory();
        category.setName("Food");
        entityManager.persist(category);

        saveLimit(ACCOUNT_FROM, category, LIMIT_SET_AT, "100.00", usd);
        saveLimit(ACCOUNT_FROM, category, LIMIT_SET_AT.plusDays(3), "150.00", usd);
        saveTransaction(ACCOUNT_FROM, category, LIMIT_SET_AT.minusDays(1), true, usd);
        Transaction transactionWithPreviousLimit = saveTransaction(
                ACCOUNT_FROM, category, LIMIT_SET_AT.plusDays(2), true, usd
        );
        Transaction expected = saveTransaction(
                ACCOUNT_FROM, category, LIMIT_SET_AT.plusDays(4), true, usd
        );
        saveTransaction(ACCOUNT_FROM, category, LIMIT_SET_AT.plusDays(4), false, usd);
        saveTransaction(OTHER_ACCOUNT, category, LIMIT_SET_AT.plusDays(4), true, usd);
        saveTransaction(ACCOUNT_FROM, category, LIMIT_SET_AT.plusDays(4), true, eur);

        Set<Transaction> results = transactionRepository.findExpenseLimitTransaction(ACCOUNT_FROM);

        assertThat(results)
                .extracting(Transaction::getId)
                .containsExactlyInAnyOrder(transactionWithPreviousLimit.getId(), expected.getId());
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
