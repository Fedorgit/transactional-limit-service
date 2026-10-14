package com.bank.limits;

import com.bank.limits.infrastructure.persistence.entity.*;
import com.bank.limits.infrastructure.persistence.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TransactionalLimitApiIntegrationTest.FixedClockConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TransactionalLimitApiIntegrationTest {

    private static final String ACCOUNT_FROM = "1234567890";
    private static final String ACCOUNT_TO = "0987654321";
    private static final WireMockServer WIRE_MOCK = new WireMockServer(0);

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));
    private final HttpClient httpClient = HttpClient.newHttpClient();
    @LocalServerPort
    private int port;
    private ObjectMapper objectMapper = new ObjectMapper();
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private CurrencyRepository currencyRepository;
    @Autowired
    private ExpenseCategoryRepository categoryRepository;
    @Autowired
    private ExpenseLimitRepository expenseLimitRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private ExchangeRateRepository exchangeRateRepository;
    @Autowired
    private MutableClock clock;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (!WIRE_MOCK.isRunning()) {
            WIRE_MOCK.start();
        }
        if (DockerClientFactory.instance().isDockerAvailable()) {
            if (!POSTGRES.isRunning()) {
                POSTGRES.start();
            }
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
            registry.add("spring.flyway.enabled", () -> true);
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        }
        registry.add("external-api.alphavantage.uri", WIRE_MOCK::baseUrl);
    }

    @AfterAll
    static void stopWireMock() {
        if (WIRE_MOCK.isRunning()) {
            WIRE_MOCK.stop();
        }
    }

    @BeforeEach
    void cleanDatabaseAndResetClock() {
        jdbcTemplate.execute("TRUNCATE TABLE transaction, expense_limit, exchange_rate RESTART IDENTITY");
        clock.setInstant(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    void flywayCreatesSchemaAndSeedsSupportedCategoriesAndCurrencies() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true",
                Integer.class
        )).isGreaterThanOrEqualTo(5);
        assertThat(categoryRepository.findTopByName("product")).isPresent();
        assertThat(categoryRepository.findTopByName("service")).isPresent();
        assertThat(currencyRepository.findById("USD")).isPresent();
    }

    @Test
    void persistsAndReadsTransactionLimitAndExchangeRate() {
        Currency usd = currencyRepository.findById("USD").orElseThrow();
        ExpenseCategory category = categoryRepository.findTopByName("product").orElseThrow();
        OffsetDateTimeFixtures fixtures = new OffsetDateTimeFixtures();

        ExpenseLimit limit = new ExpenseLimit();
        limit.setAccountFrom(ACCOUNT_FROM);
        limit.setExpenseCategory(category);
        limit.setSetDatetime(fixtures.januaryFirst());
        limit.setAmount(new BigDecimal("1000.00"));
        limit.setCurrencyCode(usd);
        ExpenseLimit savedLimit = expenseLimitRepository.saveAndFlush(limit);

        Transaction transaction = transaction(
                category,
                usd,
                fixtures.januarySecond(),
                new BigDecimal("500.00"),
                false
        );
        Transaction savedTransaction = transactionRepository.saveAndFlush(transaction);

        Currency eur = currencyRepository.saveAndFlush(new Currency("EUR"));
        ExchangeRate rate = new ExchangeRate();
        rate.setBaseCurrency(usd);
        rate.setTargetCurrency(eur);
        rate.setRate(new BigDecimal("0.9200"));
        rate.setRateDate(fixtures.januarySecond().toLocalDate());
        exchangeRateRepository.saveAndFlush(rate);

        assertThat(jdbcTemplate.queryForMap(
                "SELECT account_from, amount, set_datetime FROM expense_limit WHERE id = ?",
                savedLimit.getId()
        ))
                .containsEntry("account_from", ACCOUNT_FROM)
                .containsEntry("amount", new BigDecimal("1000.00"));
        assertThat(jdbcTemplate.queryForMap(
                "SELECT account_from, account_to, amount, limit_exceeded FROM transaction WHERE id = ?",
                savedTransaction.getId()
        ))
                .containsEntry("account_from", ACCOUNT_FROM)
                .containsEntry("account_to", ACCOUNT_TO)
                .containsEntry("amount", new BigDecimal("500.00"))
                .containsEntry("limit_exceeded", false);
        assertThat(exchangeRateRepository.findLatestOnOrBefore(
                "USD",
                "EUR",
                fixtures.januarySecond().toLocalDate()
        )).get()
                .extracting(ExchangeRate::getRate)
                .isEqualTo(new BigDecimal("0.9200"));
    }

    @Test
    void postgresEnforcesUniqueAndForeignKeyConstraints() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO expense_category (name) VALUES ('product')"
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbcTemplate.update("""
                INSERT INTO transaction (
                    datetime, account_from, account_to, amount, currency_code, expense_category_id, limit_exceeded
                )
                SELECT TIMESTAMPTZ '2026-01-02T00:00:00Z', ?, ?, 1.00, 'BAD',
                       id, false
                FROM expense_category
                WHERE name = 'product'
                """, ACCOUNT_FROM, ACCOUNT_TO))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void endToEndLimitChangesMonthlyEvaluationAndExceededTransactionsApi() throws Exception {
        ApiResponse initialLimit = post(
                "/api/v1/limits",
                """
                        {
                          "amount": 1000.00,
                          "currency_code": "USD",
                          "account_from": "1234567890",
                          "expense_category": "product"
                        }
                        """
        );
        assertThat(initialLimit.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(initialLimit.body().path("datetime").asText())
                .startsWith("2026-01-01T00:00:00");

        assertTransaction("2026-01-02T12:00:00Z", "500.00", false);
        assertTransaction("2026-01-03T12:00:00Z", "600.00", true);

        clock.setInstant(Instant.parse("2026-01-10T00:00:00Z"));
        ApiResponse replacementLimit = post(
                "/api/v1/limits",
                """
                        {
                          "amount": 2000.00,
                          "currency_code": "USD",
                          "account_from": "1234567890",
                          "expense_category": "product"
                        }
                        """
        );
        assertThat(replacementLimit.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(replacementLimit.body().path("datetime").asText())
                .startsWith("2026-01-10T00:00:00");

        assertTransaction("2026-01-11T12:00:00Z", "100.00", false);
        assertTransaction("2026-01-12T12:00:00Z", "700.00", false);
        assertTransaction("2026-01-13T12:00:00Z", "100.00", false);
        assertTransaction("2026-01-13T12:01:00Z", "1101.00", true);

        ApiResponse exceededResponse = get(
                "/api/v1/transactions/exceeded?account_from=" + ACCOUNT_FROM + "&page=0&size=10"
        );
        assertThat(exceededResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
        JsonNode exceeded = exceededResponse.body().path("content");
        assertThat(exceeded).hasSize(2);
        assertThat(exceeded.get(0).path("datetime").asText()).startsWith("2026-01-03T12:00:00");
        assertThat(exceeded.get(0).path("limit_amount").decimalValue())
                .isEqualByComparingTo("1000.00");
        assertThat(exceeded.get(0).path("limit_currency_code").path("code").asText()).isEqualTo("USD");
        assertThat(exceeded.get(0).path("limit_set_datetime").asText())
                .startsWith("2026-01-01T00:00:00");
        assertThat(exceeded.get(1).path("datetime").asText()).startsWith("2026-01-13T12:01:00");
        assertThat(exceeded.get(1).path("limit_amount").decimalValue())
                .isEqualByComparingTo("2000.00");
        assertThat(exceeded.get(1).path("limit_currency_code").path("code").asText()).isEqualTo("USD");
        assertThat(exceeded.get(1).path("limit_set_datetime").asText())
                .startsWith("2026-01-10T00:00:00");
        assertThat(transactionRepository.count()).isEqualTo(6);
        assertThat(expenseLimitRepository.count()).isEqualTo(2);
        WIRE_MOCK.verify(0, anyRequestedFor(urlPathMatching(".*")));
    }

    @Test
    void simultaneousTransactionsAgainstOneLimitAreSerialized() throws Exception {
        post("/api/v1/limits", """
                {
                  "amount": 1000.00,
                  "currency_code": "USD",
                  "account_from": "1234567890",
                  "expense_category": "product"
                }
                """);

        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.CountDownLatch ready = new java.util.concurrent.CountDownLatch(2);
            java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
            var first = executor.submit(() -> postConcurrentTransaction(
                    ready, start, "2026-01-02T12:00:00Z", "100.00"
            ));
            var second = executor.submit(() -> postConcurrentTransaction(
                    ready, start, "2026-01-02T12:00:00Z", "1100.00"
            ));

            assertThat(ready.await(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            start.countDown();
            JsonNode firstBody = first.get(20, java.util.concurrent.TimeUnit.SECONDS);
            JsonNode secondBody = second.get(20, java.util.concurrent.TimeUnit.SECONDS);

            assertThat(firstBody.path("limit_exceeded").asBoolean()).isFalse();
            assertThat(secondBody.path("limit_exceeded").asBoolean()).isTrue();
            assertThat(transactionRepository.count()).isEqualTo(2);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void successiveTransactionsUsePreviouslyCommittedSpend() throws Exception {
        post("/api/v1/limits", """
                {
                  "amount": 1000.00,
                  "currency_code": "USD",
                  "account_from": "1234567890",
                  "expense_category": "product"
                }
                """);

        assertTransaction("2026-01-02T12:00:00Z", "600.00", false);
        assertTransaction("2026-01-02T12:00:01Z", "600.00", true);
        assertThat(transactionRepository.count()).isEqualTo(2);
    }

    @Test
    void failedExchangeRateLookupReturnsProblemDetailAndRollsBackPartialWrites() throws Exception {
        ApiResponse response = post(
                "/api/v1/transactions",
                """
                        {
                          "account_from": "1234567890",
                          "account_to": "0987654321",
                          "currency_shortname": "EUR",
                          "sum": 10.00,
                          "expense_category": "product",
                          "datetime": "2026-01-02T12:00:00Z"
                        }
                        """
        );

        assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.contentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.body().path("status").asInt()).isEqualTo(404);
        assertThat(response.body().path("detail").asText())
                .contains("No exchange rate available");
        assertThat(transactionRepository.count()).isZero();
        assertThat(expenseLimitRepository.count()).isZero();
        WIRE_MOCK.verify(0, anyRequestedFor(urlPathMatching(".*")));
    }

    private void assertTransaction(String datetime, String amount, boolean exceeded) throws Exception {
        ApiResponse response = post(
                "/api/v1/transactions",
                """
                        {
                          "account_from": "1234567890",
                          "account_to": "0987654321",
                          "currency_shortname": "USD",
                          "sum": %s,
                          "expense_category": "product",
                          "datetime": "%s"
                        }
                        """.formatted(amount, datetime)
        );

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body().path("limit_exceeded").asBoolean()).isEqualTo(exceeded);
    }

    private JsonNode postConcurrentTransaction(
            java.util.concurrent.CountDownLatch ready,
            java.util.concurrent.CountDownLatch start,
            String datetime,
            String amount
    ) throws Exception {
        ready.countDown();
        if (!start.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent transaction start was not released");
        }
        return post(
                "/api/v1/transactions",
                """
                        {
                          "account_from": "1234567890",
                          "account_to": "0987654321",
                          "currency_shortname": "USD",
                          "sum": %s,
                          "expense_category": "product",
                          "datetime": "%s"
                        }
                        """.formatted(amount, datetime)
        ).body();
    }

    private ApiResponse post(String path, String json) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url(path)))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        return new ApiResponse(
                response.statusCode(),
                response.headers().firstValue("Content-Type").orElse(""),
                objectMapper.readTree(response.body())
        );
    }

    private ApiResponse get(String path) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(
                HttpRequest.newBuilder(URI.create(url(path))).GET().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        return new ApiResponse(
                response.statusCode(),
                response.headers().firstValue("Content-Type").orElse(""),
                objectMapper.readTree(response.body())
        );
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private Transaction transaction(
            ExpenseCategory category,
            Currency currency,
            java.time.OffsetDateTime datetime,
            BigDecimal amount,
            boolean limitExceeded
    ) {
        Transaction transaction = new Transaction();
        transaction.setAccountFrom(ACCOUNT_FROM);
        transaction.setAccountTo(ACCOUNT_TO);
        transaction.setDatetime(datetime);
        transaction.setAmount(amount);
        transaction.setCurrencyCode(currency);
        transaction.setExpenseCategory(category);
        transaction.setLimitExceeded(limitExceeded);
        return transaction;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        }
    }

    static class MutableClock extends Clock {

        private final AtomicReference<Instant> instant;

        MutableClock(Instant initialInstant) {
            instant = new AtomicReference<>(initialInstant);
        }

        void setInstant(Instant newInstant) {
            instant.set(newInstant);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant(), zone);
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }

    private static class OffsetDateTimeFixtures {
        java.time.OffsetDateTime januaryFirst() {
            return java.time.OffsetDateTime.parse("2026-01-01T00:00:00Z");
        }

        java.time.OffsetDateTime januarySecond() {
            return java.time.OffsetDateTime.parse("2026-01-02T00:00:00Z");
        }
    }

    private record ApiResponse(int statusCode, String contentType, JsonNode body) {
    }
}
