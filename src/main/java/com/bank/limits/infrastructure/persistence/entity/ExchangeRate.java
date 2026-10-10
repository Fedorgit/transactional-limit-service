package com.bank.limits.infrastructure.persistence.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "exchange_rate")
public class ExchangeRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "base_currency", nullable = false)
    private Currency baseCurrency;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_currency", nullable = false)
    private Currency targetCurrency;

    @NotNull
    @Column(name = "rate", nullable = false, precision = 15, scale = 4)
    private BigDecimal rate;

    @NotNull
    @Column(name = "rate_date", nullable = false)
    private LocalDate rateDate;

}