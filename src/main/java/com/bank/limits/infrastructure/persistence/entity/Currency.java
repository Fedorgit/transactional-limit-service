package com.bank.limits.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "currency")
public class Currency {
    @Id
    @Size(max = 3)
    @Column(name = "code", nullable = false, length = 3)
    private String code;

    @NotNull
    @Column(name = "numeric_code", nullable = false)
    private Short numericCode;

}