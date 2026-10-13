package com.bank.limits.infrastructure.rest.external.provider.aplhavantage;

import com.bank.limits.infrastructure.rest.external.ExternalApiClient;
import com.bank.limits.infrastructure.rest.external.dto.ExchangesFromApiDto;
import com.bank.limits.infrastructure.rest.external.mapper.ExternalApiResponseMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Objects;

@Component
public class AlphavantageService implements ExternalApiClient {

    private final RestClient restClient;
    private final String apiKey;
    private final ExternalApiResponseMapper externalApiResponseMapper;

    public AlphavantageService(
            RestClient.Builder restClientBuilder,
            @Value("${external-api.alphavantage.key}") String apiKey,
            ExternalApiResponseMapper externalApiResponseMapper
    ) {
        this.apiKey = apiKey;
        this.restClient = restClientBuilder
                .baseUrl("https://www.alphavantage.co/query")
                .build();
        this.externalApiResponseMapper = externalApiResponseMapper;
    }

    @Override
    @Retryable(
            retryFor = {RestClientException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 5000, multiplier = 2)
    )
    public List<ExchangesFromApiDto> doRequest(String baseCurrency, List<String> toCurrency) {

        return toCurrency.stream()
                .map(tCurrency -> {
                    AplhavantageExchangeRateDetails response = this.restClient.get()
                            .uri(uriBuilder -> uriBuilder
                                    .queryParam("function", "CURRENCY_EXCHANGE_RATE")
                                    .queryParam("from_currency", baseCurrency)
                                    .queryParam("to_currency", tCurrency)
                                    .queryParam("apikey", apiKey)
                                    .build())
                            .retrieve()
                            .body(AplhavantageExchangeRateDetails.class);

                    return externalApiResponseMapper.toLocal(response);
                })
                .filter(Objects::nonNull)
                .toList();

    }



}
