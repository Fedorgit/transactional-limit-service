package com.bank.limits.application.configuration;

import com.bank.limits.domain.service.CheckAvailableCategory;
import com.bank.limits.domain.service.LimitService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LimitServiceConfiguration {

    @Bean
    public LimitService limitService(CheckAvailableCategory checkAvailableCategory){
        return new LimitService(checkAvailableCategory);
    }

}
