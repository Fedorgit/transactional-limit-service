package com.bank.limits.infrastructure.expection;

import com.bank.limits.domain.exception.BusinessRuleException;
import com.bank.limits.domain.exception.DomainValidationException;
import com.bank.limits.infrastructure.rest.expection.GlobalExceptionHandler;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ExceptionThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void mapsDomainValidationToBadRequestProblemDetail() throws Exception {
        mockMvc.perform(get("/errors/domain-validation"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").value(HttpStatus.BAD_REQUEST.getReasonPhrase()))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Invalid transaction"))
                .andExpect(jsonPath("$.instance").value("/errors/domain-validation"));
    }

    @Test
    void mapsBusinessRuleToUnprocessableEntityProblemDetail() throws Exception {
        mockMvc.perform(get("/errors/business-rule"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value("Transaction violates a business rule"));
    }

    @Test
    void mapsMissingEntityToNotFoundProblemDetail() throws Exception {
        mockMvc.perform(get("/errors/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Expense category not found: product"));
    }

    @Test
    void hidesUnexpectedExceptionDetailsFromProblemDetail() throws Exception {
        mockMvc.perform(get("/errors/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }

    @Test
    void formatsFrameworkHttpErrorsAsProblemDetail() throws Exception {
        mockMvc.perform(post("/errors/domain-validation"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.instance").value("/errors/domain-validation"));
    }

    @RestController
    static class ExceptionThrowingController {

        @GetMapping("/errors/domain-validation")
        void domainValidation() {
            throw new DomainValidationException("Invalid transaction");
        }

        @GetMapping("/errors/business-rule")
        void businessRule() {
            throw new BusinessRuleException("Transaction violates a business rule");
        }

        @GetMapping("/errors/not-found")
        void notFound() {
            throw new EntityNotFoundException("Expense category not found: product");
        }

        @GetMapping("/errors/unexpected")
        void unexpected() {
            throw new IllegalStateException("Sensitive internal details");
        }
    }
}
