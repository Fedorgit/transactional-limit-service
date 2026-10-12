package com.bank.limits.application.service;

import com.bank.limits.application.mapper.ExchangeRateMapper;
import com.bank.limits.application.mapper.LimitMapper;
import com.bank.limits.application.util.LimitUtil;
import com.bank.limits.domain.model.Transaction;
import com.bank.limits.domain.service.ExecuteTransactionService;
import com.bank.limits.infrastructure.persistence.entity.Currency;
import com.bank.limits.infrastructure.persistence.entity.ExchangeRate;
import com.bank.limits.infrastructure.persistence.entity.ExpenseLimit;
import com.bank.limits.infrastructure.persistence.repository.ExchangeRateRepository;
import com.bank.limits.infrastructure.persistence.repository.ExpenseLimitRepository;
import com.bank.limits.infrastructure.rest.dto.TransactionRequestDto;
import com.bank.limits.application.mapper.TransactionMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final ExecuteTransactionService transactionService;

    private final TransactionMapper transactionMapper;
    private final ExchangeRateMapper exchangeRateMapper;
    private final LimitMapper limitMapper;

    private final ExchangeRateRepository exchangeRateRepository;
    private final ExpenseLimitRepository limitRepository;
    private final LimitUtil limitUtil;

    public void executeTransaction(TransactionRequestDto dto){

        ExpenseLimit limit = limitRepository
                .findTopByExpenseCategory_NameOrderBySetDatetimeDesc(dto.expenseCategory())
                .orElseGet(() -> limitUtil.createLimit(dto.accountFrom(), dto.expenseCategory()));
        ExchangeRate rate = exchangeRateRepository
                .findTopByBaseCurrencyAndTargetCurrencyOrderByRateDateDesc(
                        new Currency(dto.currencyShortname()),
                        new Currency(limit.getCurrencyCode().getCode())
                ).orElseThrow(()->new EntityNotFoundException("No such exchange rate!"));


        transactionService.execute(
                transactionMapper.toTransaction(dto),
                exchangeRateMapper.toDomain(rate),
                limitMapper.toDomain(limit)
        );

    }

}
