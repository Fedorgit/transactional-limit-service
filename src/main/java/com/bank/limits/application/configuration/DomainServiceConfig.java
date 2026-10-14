package com.bank.limits.application.configuration;

import com.bank.limits.domain.repository.AvailableCategoriesRepository;
import com.bank.limits.domain.service.CheckAvailableCategory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public CheckAvailableCategory checkAvailableCategory(
            AvailableCategoriesRepository availableCategoriesRepository
    ) {
        return new CheckAvailableCategory(availableCategoriesRepository);
    }


}
