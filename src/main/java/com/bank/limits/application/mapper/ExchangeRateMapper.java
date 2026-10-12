package com.bank.limits.application.mapper;

import com.bank.limits.domain.model.Currency;
import com.bank.limits.domain.model.ExchangeRate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExchangeRateMapper {

    @Mapping(target = "baseCurrency", source = "baseCurrency")
    @Mapping(target = "targetCurrency", source = "targetCurrency")
    @Mapping(target = "rate", source = "rate")
    @Mapping(target = "rateDate", source = "rateDate")
    ExchangeRate toDomain(com.bank.limits.infrastructure.persistence.entity.ExchangeRate entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "baseCurrency", source = "baseCurrency")
    @Mapping(target = "targetCurrency", source = "targetCurrency")
    @Mapping(target = "rate", source = "rate")
    @Mapping(target = "rateDate", source = "rateDate")
    com.bank.limits.infrastructure.persistence.entity.ExchangeRate toEntity(ExchangeRate exchangeRate);

    default Currency toDomainCurrency(com.bank.limits.infrastructure.persistence.entity.Currency currency) {
        return new Currency(currency.getCode());
    }

    default com.bank.limits.infrastructure.persistence.entity.Currency toEntityCurrency(Currency currency) {
        return new com.bank.limits.infrastructure.persistence.entity.Currency(currency.code());
    }
}