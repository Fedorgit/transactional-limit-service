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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;

@Component
public class AlphavantageService implements ExternalApiClient {

    private final RestClient restClient;
    private final String apiKey;
    private final ExternalApiResponseMapper externalApiResponseMapper;

    public AlphavantageService(
            RestClient.Builder restClientBuilder,
            @Value("${external-api.alphavantage.key}") String apiKey,
            @Value("${external-api.alphavantage.uri}") String baseUrl,
            ExternalApiResponseMapper externalApiResponseMapper
    ) {
        this.apiKey = apiKey;
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
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

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<ExchangesFromApiDto>> futures = toCurrency.stream()
                    .map(tCurrency -> CompletableFuture.supplyAsync(() -> {
                        AlphaVantageResponse response = this.restClient.get()
                                .uri(uriBuilder -> uriBuilder
                                        .queryParam("function", "CURRENCY_EXCHANGE_RATE")
                                        .queryParam("from_currency", baseCurrency)
                                        .queryParam("to_currency", tCurrency)
                                        .queryParam("apikey", apiKey)
                                        .build())
                                .retrieve()
                                .body(AlphaVantageResponse.class);

                        if (response == null || response.exchangeRateDetails() == null) {
                            throw new IllegalStateException(
                                    "Exchange-rate API returned no rate details for " + baseCurrency + "/" + tCurrency
                            );
                        }
                        ExchangesFromApiDto exchange = externalApiResponseMapper.toLocal(
                                response.exchangeRateDetails()
                        );
                        if (exchange == null
                                || !baseCurrency.equals(exchange.base_currency())
                                || !tCurrency.equals(exchange.target_currency())
                                || exchange.rate() == null
                                || exchange.rate().signum() <= 0
                                || exchange.rate_date() == null) {
                            throw new IllegalStateException(
                                    "Exchange-rate API returned an invalid rate for " + baseCurrency + "/" + tCurrency
                            );
                        }
                        return exchange;
                    }, executor))
                    .toList();

            try {
                return futures.stream()
                        .map(CompletableFuture::join)
                        .toList();
            } catch (CompletionException exception) {
                if (exception.getCause() instanceof RuntimeException cause) {
                    throw cause;
                }
                throw exception;
            }
        }
    }


}
