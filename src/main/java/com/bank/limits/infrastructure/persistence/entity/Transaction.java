package com.bank.limits.infrastructure.persistence.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "transaction")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @Column(name = "datetime", nullable = false)
    private OffsetDateTime datetime;

    @Size(max = 10)
    @NotNull
    @Column(name = "account_from", nullable = false, length = 10)
    private String accountFrom;

    @Size(max = 10)
    @NotNull
    @Column(name = "account_to", nullable = false, length = 10)
    private String accountTo;

    @NotNull
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currencyCode;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_category_id", nullable = false)
    private ExpenseCategory expenseCategory;

    @NotNull
    @Column(name = "limit_exceeded", nullable = false)
    private Boolean limitExceeded = false;

}