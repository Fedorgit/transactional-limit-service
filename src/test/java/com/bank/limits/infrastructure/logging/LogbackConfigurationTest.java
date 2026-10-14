package com.bank.limits.infrastructure.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.logging.logback.StructuredLogEncoder;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LogbackConfigurationTest {

    @Test
    void configuresJsonConsoleLoggingAndApplicationDebugLevel() throws Exception {
        LoggerContext context = new LoggerContext();
        context.setName("logback-configuration-test");
        context.putObject(Environment.class.getName(), new StandardEnvironment());

        try {
            ch.qos.logback.classic.joran.JoranConfigurator configurator =
                    new ch.qos.logback.classic.joran.JoranConfigurator();
            configurator.setContext(context);
            configurator.doConfigure(getClass().getResource("/logback-spring.xml"));

            Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
            Logger applicationLogger = context.getLogger("com.bank.limits");
            ConsoleAppender<?> consoleAppender = (ConsoleAppender<?>) rootLogger.getAppender("CONSOLE");
            StructuredLogEncoder encoder = assertInstanceOf(StructuredLogEncoder.class, consoleAppender.getEncoder());

            assertEquals(Level.INFO, rootLogger.getLevel());
            assertEquals(Level.DEBUG, applicationLogger.getLevel());
            assertNotNull(consoleAppender);
            LoggingEvent event = new LoggingEvent(
                    getClass().getName(),
                    applicationLogger,
                    Level.INFO,
                    "structured-log-test",
                    null,
                    null
            );
            event.setMDCPropertyMap(Map.of());
            JsonNode json = new ObjectMapper().readTree(encoder.encode(event));
            assertTrue(json.isObject());
            assertEquals("INFO", json.get("level").asText());
            assertEquals("structured-log-test", json.get("message").asText());
            assertEquals(0, context.getStatusManager().getCopyOfStatusList().stream()
                    .filter(status -> status.getLevel() >= ch.qos.logback.core.status.Status.ERROR)
                    .count());
        } finally {
            context.stop();
        }
    }
}
