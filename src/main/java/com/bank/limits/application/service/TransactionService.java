package com.bank.limits.application.service;

import com.bank.limits.application.mapper.ExchangeRateMapper;
import com.bank.limits.application.mapper.LimitMapper;
import com.bank.limits.application.mapper.TransactionMapper;
import com.bank.limits.application.util.LimitUtil;
import com.bank.limits.domain.model.LimitStatus;
import com.bank.limits.domain.service.ExecuteTransactionService;
import com.bank.limits.infrastructure.persistence.dto.ExceededTransactionWithLimitDto;
import com.bank.limits.infrastructure.persistence.entity.Currency;
import com.bank.limits.infrastructure.persistence.entity.ExchangeRate;
import com.bank.limits.infrastructure.persistence.entity.ExpenseLimit;
import com.bank.limits.infrastructure.persistence.repository.CurrencyRepository;
import com.bank.limits.infrastructure.persistence.repository.ExpenseCategoryRepository;
import com.bank.limits.infrastructure.persistence.repository.ExchangeRateRepository;
import com.bank.limits.infrastructure.persistence.repository.ExpenseLimitRepository;
import com.bank.limits.infrastructure.persistence.repository.TransactionRepository;
import com.bank.limits.infrastructure.rest.dto.transaction.ExecutedTransactionResponseDto;
import com.bank.limits.infrastructure.rest.dto.transaction.TransactionRequestDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

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
    private final CurrencyRepository currencyRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;

    @Transactional
    public ExecutedTransactionResponseDto executeTransaction(TransactionRequestDto dto){
        if (dto.datetime() == null) {
            throw new IllegalArgumentException("Transaction date is required");
        }

        OffsetDateTime transactionDateTime = OffsetDateTime.ofInstant(dto.datetime(), ZoneOffset.UTC);
        ExpenseLimit limit = limitRepository
                .findTopByAccountFromAndExpenseCategory_NameAndSetDatetimeLessThanEqualOrderBySetDatetimeDesc(
                        dto.accountFrom(), dto.expenseCategory(), transactionDateTime
                )
                .orElseGet(() -> limitUtil.createLimit(
                        dto.accountFrom(), dto.expenseCategory(), transactionDateTime
                ));

        String sourceCurrencyCode = dto.currencyShortname();
        String targetCurrencyCode = limit.getCurrencyCode().getCode();
        ExchangeRate rate = null;

        if (!sourceCurrencyCode.equals(targetCurrencyCode)) {
            LocalDate rateDate = transactionDateTime.toLocalDate();
            rate = exchangeRateRepository.findLatestOnOrBefore(
                    sourceCurrencyCode, targetCurrencyCode, rateDate
            ).orElseThrow(() -> new EntityNotFoundException(
                    "No exchange rate available for " + sourceCurrencyCode + " to " + targetCurrencyCode
                            + " on or before " + rateDate
            ));
        }

        LimitStatus limitStatus = transactionService.execute(
                transactionMapper.toTransaction(dto),
                rate == null ? null : exchangeRateMapper.toDomain(rate),
                limitMapper.toDomain(limit)
        );

        Currency currency = currencyRepository.findById(sourceCurrencyCode)
                .orElseThrow(() -> new EntityNotFoundException("Currency not found: " + sourceCurrencyCode));
        com.bank.limits.infrastructure.persistence.entity.ExpenseCategory category =
                expenseCategoryRepository.findTopByName(dto.expenseCategory())
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Expense category not found: " + dto.expenseCategory()
                        ));
        transactionRepository.save(transactionMapper.toEntity(
                dto, currency, category, limitStatus.isLimitExceeded()
        ));

        return transactionMapper.toExecutedTransactionResponseDto(dto, limitStatus.isLimitExceeded());

    }

    @Transactional(readOnly = true)
    public Page<ExceededTransactionWithLimitDto> getExceededTransactions
            (String accountFrom, Pageable pageable)
    {
        return transactionRepository.findExceededTransactions(accountFrom,pageable);
    }

}
