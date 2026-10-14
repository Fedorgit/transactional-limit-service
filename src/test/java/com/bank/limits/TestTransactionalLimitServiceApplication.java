package com.bank.limits;

import org.springframework.boot.SpringApplication;

public class TestTransactionalLimitServiceApplication {

    public static void main(String[] args) {
        SpringApplication.from(TransactionalLimitServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
