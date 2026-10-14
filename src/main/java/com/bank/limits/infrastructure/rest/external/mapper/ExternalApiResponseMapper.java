package com.bank.limits.infrastructure.rest.external.mapper;

import com.bank.limits.infrastructure.persistence.entity.ExchangeRate;
import com.bank.limits.infrastructure.rest.external.dto.ExchangesFromApiDto;
import com.bank.limits.infrastructure.rest.external.provider.aplhavantage.AplhavantageExchangeRateDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.math.RoundingMode;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ExternalApiResponseMapper {

    @Mapping(target = "base_currency", source = "fromCurrencyCode")
    @Mapping(target = "target_currency", source = "toCurrencyCode")
    @Mapping(target = "rate_date", source = "lastRefreshed")
    @Mapping(target = "rate", source = "exchangeRate",qualifiedByName = "scaleRate")
    ExchangesFromApiDto toLocal(AplhavantageExchangeRateDetails aplhavantageExchangeRateDetails);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "targetCurrency", source = "target_currency")
    @Mapping(target = "rateDate", source = "rate_date")
    @Mapping(target = "baseCurrency", source = "base_currency")
    ExchangeRate toEntity(ExchangesFromApiDto exchangesFromApiDto);

    default com.bank.limits.infrastructure.persistence.entity.Currency toEntityCurrency(String code) {
        return code == null ? null : new com.bank.limits.infrastructure.persistence.entity.Currency(code);
    }

    default LocalDate toLocalDate(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.toLocalDate();
    }

    default OffsetDateTime toOffsetDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.atOffset(ZoneOffset.UTC);
    }

    @Named("scaleRate")
    default BigDecimal scaleRate(BigDecimal rawRate) {
        if (rawRate == null) return null;

        return rawRate.setScale(4, RoundingMode.HALF_UP);
    }
}
