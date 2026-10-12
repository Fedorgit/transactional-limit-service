package com.bank.limits.application.configuration;

import com.bank.limits.domain.repository.AvailableCategoriesRepository;
import com.bank.limits.domain.repository.PreviousTransactionRepository;
import com.bank.limits.domain.service.CheckAvailableCategory;
import com.bank.limits.domain.service.ExecuteTransactionService;
import com.bank.limits.infrastructure.persistence.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public CheckAvailableCategory checkAvailableCategory(
            AvailableCategoriesRepository availableCategoriesRepository
    ){
        return new CheckAvailableCategory(availableCategoriesRepository);
    }



}
