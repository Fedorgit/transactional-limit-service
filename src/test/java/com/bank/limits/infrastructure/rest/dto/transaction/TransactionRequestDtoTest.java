package com.bank.limits.infrastructure.rest.dto.transaction;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionRequestDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @Test
    void deserializesCamelCaseStringFields() throws Exception {
        TransactionRequestDto dto = objectMapper.readValue("""
                {
                  "accountFrom": "1234567890",
                  "accountTo": "0987654321",
                  "currencyShortname": "USD",
                  "expenseCategory": "product",
                  "sum": 10.00
                }
                """, TransactionRequestDto.class);

        assertEquals("1234567890", dto.accountFrom());
        assertEquals("0987654321", dto.accountTo());
        assertEquals("USD", dto.currencyShortname());
        assertEquals("product", dto.expenseCategory());
    }

    @Test
    void continuesToDeserializeSnakeCaseStringFields() throws Exception {
        TransactionRequestDto dto = objectMapper.readValue("""
                {
                  "account_from": "1234567890",
                  "account_to": "0987654321",
                  "currency_shortname": "USD",
                  "expense_category": "product",
                  "sum": 10.00
                }
                """, TransactionRequestDto.class);

        assertEquals("1234567890", dto.accountFrom());
        assertEquals("0987654321", dto.accountTo());
        assertEquals("USD", dto.currencyShortname());
        assertEquals("product", dto.expenseCategory());
    }
}
