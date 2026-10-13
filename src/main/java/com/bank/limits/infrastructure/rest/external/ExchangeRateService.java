package com.bank.limits.infrastructure.rest.external;

import com.bank.limits.infrastructure.persistence.entity.ExchangeRate;
import com.bank.limits.infrastructure.persistence.repository.ExchangeRateRepository;
import com.bank.limits.infrastructure.rest.external.dto.ExchangeRatesProperties;
import com.bank.limits.infrastructure.rest.external.dto.ExchangesFromApiDto;
import com.bank.limits.infrastructure.rest.external.mapper.ExternalApiResponseMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExchangeRateService {

    private final ExternalApiClient apiClient;
    private final ExchangeRateRepository exchangeRateRepository;
    private final ExternalApiResponseMapper externalApiResponseMapper;
    private final ExchangeRatesProperties ratesProperties;

    @Value("${exchange-rates.base-currency}")
    private String baseCurrency;

    public ExchangeRateService(@Qualifier("alphavantageService") ExternalApiClient apiClient, ExchangeRateRepository exchangeRateRepository, ExternalApiResponseMapper externalApiResponseMapper, ExchangeRatesProperties targetCurrencies) {
        this.apiClient = apiClient;
        this.exchangeRateRepository = exchangeRateRepository;
        this.externalApiResponseMapper = externalApiResponseMapper;
        this.ratesProperties = targetCurrencies;
    }

    @Scheduled(cron = "0 13 1 * * ?")

    public void fetchExchanges(){

        List<ExchangesFromApiDto> exchanges = apiClient.doRequest(baseCurrency, ratesProperties.targetCurrency());

        List<ExchangeRate> entities = exchanges.stream()
                .map(externalApiResponseMapper::toEntity)
                .toList();

        exchangeRateRepository.saveAll(entities);
    }


}
