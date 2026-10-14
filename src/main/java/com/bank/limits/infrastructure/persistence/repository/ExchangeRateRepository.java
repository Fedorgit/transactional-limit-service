package com.bank.limits.infrastructure.persistence.repository;

import com.bank.limits.infrastructure.persistence.entity.ExchangeRate;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    Optional<ExchangeRate> findTopByBaseCurrency_CodeAndTargetCurrency_CodeAndRateDateLessThanEqualOrderByRateDateDesc(
            @NotNull String baseCurrencyCode,
            @NotNull String targetCurrencyCode,
            @NotNull LocalDate rateDate
    );

    default Optional<ExchangeRate> findLatestOnOrBefore(
            String baseCurrencyCode,
            String targetCurrencyCode,
            LocalDate rateDate
    ) {
        Optional<ExchangeRate> direct = findTopByBaseCurrency_CodeAndTargetCurrency_CodeAndRateDateLessThanEqualOrderByRateDateDesc(
                baseCurrencyCode, targetCurrencyCode, rateDate
        );
        Optional<ExchangeRate> reverse = findTopByBaseCurrency_CodeAndTargetCurrency_CodeAndRateDateLessThanEqualOrderByRateDateDesc(
                targetCurrencyCode, baseCurrencyCode, rateDate
        );

        return Stream.concat(direct.stream(), reverse.stream())
                .max(Comparator.comparing(ExchangeRate::getRateDate));
    }
}
