package com.bank.limits.application.service;

import com.bank.limits.application.mapper.LimitMapper;
import com.bank.limits.domain.model.Limit;
import com.bank.limits.infrastructure.persistence.entity.ExpenseLimit;
import com.bank.limits.infrastructure.persistence.repository.ExpenseLimitRepository;
import com.bank.limits.infrastructure.rest.dto.LimitResponseDto;
import com.bank.limits.infrastructure.rest.dto.SetNewLimitRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LimitServiceTest {

    private com.bank.limits.domain.service.LimitService domainLimitService;
    private LimitMapper limitMapper;
    private ExpenseLimitRepository limitRepository;
    private LimitService limitService;

    @BeforeEach
    void setUp() {
        domainLimitService = mock(com.bank.limits.domain.service.LimitService.class);
        limitMapper = mock(LimitMapper.class);
        limitRepository = mock(ExpenseLimitRepository.class);
        limitService = new LimitService(domainLimitService, limitMapper, limitRepository);
    }

    @Test
    void setNewExpenseLimitMapsSavesAndReturnsResponse() {
        SetNewLimitRequestDto request = new SetNewLimitRequestDto(
                new BigDecimal("250.00"), "USD", "account-1", "product"
        );
        Limit mappedLimit = mock(Limit.class);
        ExpenseLimit entityToSave = new ExpenseLimit();
        ExpenseLimit savedEntity = new ExpenseLimit();
        LimitResponseDto expectedResponse = new LimitResponseDto(
                new BigDecimal("250.00"), "USD", "product", Instant.parse("2026-01-01T00:00:00Z")
        );

        when(limitMapper.toDomain(request)).thenReturn(mappedLimit);
        when(domainLimitService.setLimit(mappedLimit)).thenReturn(mappedLimit);
        when(limitMapper.toEntity(mappedLimit)).thenReturn(entityToSave);
        when(limitRepository.save(entityToSave)).thenReturn(savedEntity);
        when(limitMapper.toResponseDto(savedEntity)).thenReturn(expectedResponse);

        LimitResponseDto response = limitService.setNewExpenseLimit(request);

        assertSame(expectedResponse, response);
        verify(limitMapper).toDomain(request);
        verify(domainLimitService).setLimit(mappedLimit);
        verify(limitMapper).toEntity(mappedLimit);
        verify(limitRepository).save(entityToSave);
        verify(limitMapper).toResponseDto(savedEntity);
    }

    @Test
    void setNewExpenseLimitDoesNotSaveWhenDomainValidationFails() {
        SetNewLimitRequestDto request = new SetNewLimitRequestDto(
                new BigDecimal("250.00"), "USD", "account-1", "product"
        );
        Limit mappedLimit = mock(Limit.class);
        IllegalArgumentException expectedException = new IllegalArgumentException("Invalid limit");

        when(limitMapper.toDomain(request)).thenReturn(mappedLimit);
        when(domainLimitService.setLimit(mappedLimit)).thenThrow(expectedException);

        IllegalArgumentException actualException = assertThrows(
                IllegalArgumentException.class,
                () -> limitService.setNewExpenseLimit(request)
        );

        assertSame(expectedException, actualException);
        verify(limitRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(limitMapper, never()).toEntity(org.mockito.ArgumentMatchers.any());
        verify(limitMapper, never()).toResponseDto(org.mockito.ArgumentMatchers.any());
    }
}
