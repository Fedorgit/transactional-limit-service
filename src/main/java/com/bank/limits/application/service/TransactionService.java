package com.bank.limits.application.service;

import com.bank.limits.application.mapper.ExchangeRateMapper;
import com.bank.limits.application.mapper.LimitMapper;
import com.bank.limits.application.util.LimitUtil;
import com.bank.limits.domain.model.LimitStatus;
import com.bank.limits.domain.service.ExecuteTransactionService;
import com.bank.limits.infrastructure.persistence.dto.ExceededTransactionWithLimitDto;
import com.bank.limits.infrastructure.persistence.entity.Currency;
import com.bank.limits.infrastructure.persistence.entity.ExchangeRate;
import com.bank.limits.infrastructure.persistence.entity.ExpenseLimit;
import com.bank.limits.infrastructure.persistence.repository.ExchangeRateRepository;
import com.bank.limits.infrastructure.persistence.repository.ExpenseLimitRepository;
import com.bank.limits.infrastructure.persistence.repository.TransactionRepository;
import com.bank.limits.infrastructure.rest.dto.transaction.ExecutedTransactionResponseDto;
import com.bank.limits.infrastructure.rest.dto.transaction.TransactionRequestDto;
import com.bank.limits.application.mapper.TransactionMapper;
import com.bank.limits.infrastructure.rest.dto.transaction.TransactionResponseDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final ExecuteTransactionService transactionService;

    private final TransactionMapper transactionMapper;
    private final ExchangeRateMapper exchangeRateMapper;
    private final LimitMapper limitMapper;

    private final ExchangeRateRepository exchangeRateRepository;
    private final ExpenseLimitRepository limitRepository;
    private final TransactionRepository transactionRepository;
    private final LimitUtil limitUtil;

    public ExecutedTransactionResponseDto executeTransaction(TransactionRequestDto dto){

        ExpenseLimit limit = limitRepository
                .findTopByExpenseCategory_NameOrderBySetDatetimeDesc(dto.expenseCategory())
                .orElseGet(() -> limitUtil.createLimit(dto.accountFrom(), dto.expenseCategory()));
        ExchangeRate rate = exchangeRateRepository
                .findTopByBaseCurrencyAndTargetCurrencyOrderByRateDateDesc(
                        new Currency(dto.currencyShortname()),
                        new Currency(limit.getCurrencyCode().getCode())
                ).orElseThrow(()->new EntityNotFoundException("No such exchange rate!"));


        LimitStatus limitStatus = transactionService.execute(
                transactionMapper.toTransaction(dto),
                exchangeRateMapper.toDomain(rate),
                limitMapper.toDomain(limit)
        );

        return transactionMapper.toExecutedTransactionResponseDto(dto, limitStatus.isLimitExceeded());

    }

    public Page<ExceededTransactionWithLimitDto> getExceededTransactions
            (String accountFrom, Pageable pageable)
    {
        return transactionRepository.findExceededTransactions(accountFrom,pageable);
    }

}
