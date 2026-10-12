package com.bank.limits.infrastructure.persistence.repository;

import com.bank.limits.infrastructure.persistence.entity.Currency;
import com.bank.limits.infrastructure.persistence.entity.ExchangeRate;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {
    public Optional<ExchangeRate> findTopByBaseCurrencyAndTargetCurrencyOrderByRateDateDesc(
            @NotNull Currency baseCurrency, @NotNull Currency targetCurrency
    );
}
