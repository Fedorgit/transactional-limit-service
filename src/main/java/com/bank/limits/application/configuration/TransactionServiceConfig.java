package com.bank.limits.application.configuration;

import com.bank.limits.domain.repository.PreviousTransactionRepository;
import com.bank.limits.domain.service.CheckAvailableCategory;
import com.bank.limits.domain.service.ExecuteTransactionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TransactionServiceConfig {

    @Bean
    public ExecuteTransactionService executeTransactionService(
            CheckAvailableCategory checkAvailableCategory,
            PreviousTransactionRepository previousTransactionRepository
    ) {
        return new ExecuteTransactionService(checkAvailableCategory, previousTransactionRepository);
    }
}
