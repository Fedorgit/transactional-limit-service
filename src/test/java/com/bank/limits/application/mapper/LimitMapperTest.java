package com.bank.limits.application.mapper;

import com.bank.limits.domain.model.Limit;
import com.bank.limits.infrastructure.persistence.repository.ExpenseCategoryRepository;
import com.bank.limits.infrastructure.rest.dto.limit.SetNewLimitRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class LimitMapperTest {

    @Test
    void assignsInstallationTimeFromInjectedClockNotFromRequest() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
        LimitMapper mapper = new LimitMapperImpl();
        ReflectionTestUtils.setField(mapper, "clock", clock);

        Limit limit = mapper.toDomain(new SetNewLimitRequestDto(
                new java.math.BigDecimal("1000.00"),
                "USD",
                "1234567890",
                "product"
        ));

        assertThat(limit.setDatetime()).isEqualTo(OffsetDateTime.parse("2026-01-01T00:00:00Z"));
        assertThat(Arrays.stream(SetNewLimitRequestDto.class.getRecordComponents())
                .map(component -> component.getName()))
                .doesNotContain("datetime", "setDatetime");
    }

    @Test
    void mapsSeparateCategoriesAsSeparateLimits() {
        LimitMapper mapper = new LimitMapperImpl();
        ReflectionTestUtils.setField(mapper, "clock", Clock.fixed(
                Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC
        ));
        ReflectionTestUtils.setField(mapper, "expenseCategoryRepository", mock(ExpenseCategoryRepository.class));

        Limit productLimit = mapper.toDomain(new SetNewLimitRequestDto(
                new java.math.BigDecimal("1000.00"), "USD", "1234567890", "product"
        ));
        Limit serviceLimit = mapper.toDomain(new SetNewLimitRequestDto(
                new java.math.BigDecimal("2000.00"), "USD", "1234567890", "service"
        ));

        assertThat(productLimit.expenseCategory().category()).isEqualTo("product");
        assertThat(serviceLimit.expenseCategory().category()).isEqualTo("service");
        assertThat(productLimit).isNotEqualTo(serviceLimit);
    }
}
