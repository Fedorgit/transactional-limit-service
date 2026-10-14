package com.bank.limits.infrastructure.rest.validation;

import com.bank.limits.infrastructure.rest.conrtoller.TransactionsController;
import com.bank.limits.infrastructure.rest.dto.limit.LimitResponseDto;
import com.bank.limits.infrastructure.rest.dto.limit.SetNewLimitRequestDto;
import com.bank.limits.infrastructure.rest.dto.transaction.ExecutedTransactionResponseDto;
import com.bank.limits.infrastructure.rest.dto.transaction.TransactionRequestDto;
import com.bank.limits.infrastructure.rest.dto.transaction.TransactionResponseDto;
import com.bank.limits.infrastructure.rest.expection.GlobalExceptionHandler;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TransactionRequestValidationTest {

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();
    private MockMvc mockMvc;

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(mock(TransactionsController.class))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new SpringValidatorAdapter(VALIDATOR))
                .build();
    }

    @Test
    void acceptsValidTransactionRequest() {
        TransactionRequestDto request = validRequest();

        assertThat(VALIDATOR.validate(request)).isEmpty();
    }

    @Test
    void rejectsInvalidAccountsCurrencyAmountAndCategory() {
        TransactionRequestDto request = new TransactionRequestDto(
                "123456789",
                "123456789A",
                "ZZZ",
                new BigDecimal("1.001"),
                "travel",
                Instant.parse("2026-01-02T00:00:00Z")
        );

        Set<String> invalidFields = VALIDATOR.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertThat(invalidFields)
                .contains("accountFrom", "accountTo", "currencyShortname", "sum", "expenseCategory");
    }

    @Test
    void rejectsNonPositiveAndMissingAmountsAndFields() {
        TransactionRequestDto missingValues = new TransactionRequestDto(
                null, null, null, null, null, null
        );
        TransactionRequestDto nonPositiveAmounts = new TransactionRequestDto(
                "1234567890",
                "0987654321",
                "USD",
                BigDecimal.ZERO,
                "product",
                Instant.parse("2026-01-02T00:00:00Z")
        );

        assertThat(VALIDATOR.validate(missingValues))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("accountFrom", "accountTo", "currencyShortname", "sum", "expenseCategory", "datetime");
        assertThat(VALIDATOR.validate(nonPositiveAmounts))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("sum");
    }

    @Test
    void rejectsInvalidExpenseLimitRequest() {
        SetNewLimitRequestDto request = new SetNewLimitRequestDto(
                BigDecimal.ZERO,
                "ZZZ",
                "123456789",
                "travel"
        );

        assertThat(VALIDATOR.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("amount", "currency_code", "account_from", "expense_category");
    }

    @Test
    void validatesResponseDtoRequiredFieldsAndConstraints() {
        LimitResponseDto limitResponse = new LimitResponseDto(
                BigDecimal.ZERO,
                "ZZZ",
                "travel",
                null
        );
        TransactionResponseDto transactionResponse =
                new TransactionResponseDto(null, null, null, null, null, null, null);
        ExecutedTransactionResponseDto executedTransactionResponse =
                new ExecutedTransactionResponseDto(null, null, null, null, null, null, false);

        assertThat(VALIDATOR.validate(limitResponse))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("amount", "currencyCode", "expenseCategory", "datetime");
        assertThat(VALIDATOR.validate(transactionResponse))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("accountFrom", "accountTo", "expenseCategory", "transactionAmount",
                        "transactionDate", "currency", "limitExceeded");
        assertThat(VALIDATOR.validate(executedTransactionResponse))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("accountFrom", "accountTo", "expenseCategory", "transactionAmount",
                        "transactionDate", "currency");
    }

    @Test
    void mapsInvalidJsonAndBeanValidationErrorsToProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.type").value("about:blank"));

        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account_from": "invalid",
                                  "account_to": "0987654321",
                                  "currency_shortname": "USD",
                                  "sum": 10.00,
                                  "expense_category": "product",
                                  "datetime": "2026-01-02T00:00:00Z"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectsDateTimeWithoutTimezoneOffset() throws Exception {
        mockMvc.perform(post("/api/v1/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "account_from": "1234567890",
                                  "account_to": "0987654321",
                                  "currency_shortname": "USD",
                                  "sum": 10.00,
                                  "expense_category": "product",
                                  "datetime": "2026-01-02T00:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));
    }

    private TransactionRequestDto validRequest() {
        return new TransactionRequestDto(
                "1234567890",
                "0987654321",
                "USD",
                new BigDecimal("10.00"),
                "product",
                Instant.parse("2026-01-02T00:00:00Z")
        );
    }
}
