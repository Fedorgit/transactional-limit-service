package com.bank.limits.infrastructure.rest.external.provider.aplhavantage;

import com.bank.limits.infrastructure.rest.external.ExternalApiClient;
import com.bank.limits.infrastructure.rest.external.dto.ExchangesFromApiDto;
import com.bank.limits.infrastructure.rest.external.mapper.ExternalApiResponseMapperImpl;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AlphavantageServiceWireMockTest {

    private static final String SUCCESSFUL_RESPONSE = """
            {
              "Realtime Currency Exchange Rate": {
                "1. From_Currency Code": "USD",
                "3. To_Currency Code": "EUR",
                "5. Exchange Rate": "0.9200",
                "6. Last Refreshed": "2026-01-02 16:00:00"
              }
            }
            """;

    private WireMockServer wireMockServer;

    @BeforeEach
    void startWireMock() {
        wireMockServer = new WireMockServer(0);
        wireMockServer.start();
        wireMockServerBaseUrl = wireMockServer.baseUrl();
    }

    @AfterEach
    void stopWireMock() {
        wireMockServer.stop();
    }

    @Test
    void parsesSuccessfulExchangeRateResponse() {
        wireMockServer.stubFor(get(urlPathEqualTo("/"))
                .withQueryParam("function", equalTo("CURRENCY_EXCHANGE_RATE"))
                .withQueryParam("from_currency", equalTo("USD"))
                .withQueryParam("to_currency", equalTo("EUR"))
                .withQueryParam("apikey", equalTo("wiremock-key"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(SUCCESSFUL_RESPONSE)));

        List<ExchangesFromApiDto> rates = apiClient().doRequest("USD", List.of("EUR"));

        assertThat(rates).singleElement().satisfies(rate -> {
            assertThat(rate.base_currency()).isEqualTo("USD");
            assertThat(rate.target_currency()).isEqualTo("EUR");
            assertThat(rate.rate()).isEqualByComparingTo(new BigDecimal("0.9200"));
            assertThat(rate.rate_date()).isEqualTo(OffsetDateTime.parse("2026-01-02T16:00:00Z"));
        });
    }

    @Test
    void reportsHttpErrorsInsteadOfDroppingTransactions() {
        wireMockServer.stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> apiClient().doRequest("USD", List.of("EUR")))
                .isInstanceOf(RestClientException.class);
    }

    @Test
    void reportsTimedOutApiRequests() {
        wireMockServer.stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse().withFixedDelay(500).withBody(SUCCESSFUL_RESPONSE)));

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofMillis(100));
        AlphavantageService apiClient = apiClient(requestFactory);

        assertThatThrownBy(() -> apiClient.doRequest("USD", List.of("EUR")))
                .isInstanceOf(RestClientException.class);
    }

    @Test
    void rejectsIncompleteApiResponseExplicitly() {
        wireMockServer.stubFor(get(urlPathEqualTo("/"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"Note\":\"No data available\"}")));

        assertThatThrownBy(() -> apiClient().doRequest("USD", List.of("EUR")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no rate details");
    }

    @Test
    void requestsCurrenciesConcurrentlyWithoutLosingResponses() {
        wireMockServer.stubFor(get(urlPathEqualTo("/"))
                .withQueryParam("to_currency", equalTo("EUR"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(SUCCESSFUL_RESPONSE)));
        wireMockServer.stubFor(get(urlPathEqualTo("/"))
                .withQueryParam("to_currency", equalTo("KZT"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(SUCCESSFUL_RESPONSE.replace("\"EUR\"", "\"KZT\"")
                                .replace("\"0.9200\"", "\"500.0000\""))));

        List<ExchangesFromApiDto> rates = apiClient().doRequest("USD", List.of("EUR", "KZT"));

        assertThat(rates)
                .extracting(ExchangesFromApiDto::target_currency)
                .containsExactlyInAnyOrder("EUR", "KZT");
        wireMockServer.verify(1, getRequestedFor(urlPathEqualTo("/"))
                .withQueryParam("to_currency", equalTo("EUR")));
        wireMockServer.verify(1, getRequestedFor(urlPathEqualTo("/"))
                .withQueryParam("to_currency", equalTo("KZT")));
    }

    @Test
    void retriesTransientApiFailure() {
        wireMockServer.stubFor(get(urlPathEqualTo("/"))
                .inScenario("transient-failure")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("recovered"));
        wireMockServer.stubFor(get(urlPathEqualTo("/"))
                .inScenario("transient-failure")
                .whenScenarioStateIs("recovered")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(SUCCESSFUL_RESPONSE)));

        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(RetryEnabledApiConfiguration.class)) {
            List<ExchangesFromApiDto> rates = context.getBean(ExternalApiClient.class)
                    .doRequest("USD", List.of("EUR"));

            assertThat(rates).hasSize(1);
            wireMockServer.verify(2, getRequestedFor(urlPathEqualTo("/")));
        }
    }

    private AlphavantageService apiClient() {
        return apiClient(RestClient.builder());
    }

    private AlphavantageService apiClient(SimpleClientHttpRequestFactory requestFactory) {
        return apiClient(RestClient.builder().requestFactory(requestFactory));
    }

    private AlphavantageService apiClient(RestClient.Builder restClientBuilder) {
        return new AlphavantageService(
                restClientBuilder,
                "wiremock-key",
                wireMockServer.baseUrl(),
                new ExternalApiResponseMapperImpl()
        );
    }

    @Configuration(proxyBeanMethods = false)
    @EnableRetry
    static class RetryEnabledApiConfiguration {

        @Bean
        ExternalApiClient externalApiClient() {
            return new AlphavantageService(
                    RestClient.builder(),
                    "wiremock-key",
                    wireMockServerBaseUrl,
                    new ExternalApiResponseMapperImpl()
            );
        }
    }

    private static String wireMockServerBaseUrl;

}
