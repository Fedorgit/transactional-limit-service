package com.bank.limits.application.service;

import com.bank.limits.application.mapper.LimitMapper;
import com.bank.limits.domain.model.Limit;
import com.bank.limits.infrastructure.persistence.entity.ExpenseLimit;
import com.bank.limits.infrastructure.persistence.repository.ExpenseLimitRepository;
import com.bank.limits.infrastructure.rest.dto.limit.LimitResponseDto;
import com.bank.limits.infrastructure.rest.dto.limit.SetNewLimitRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class ExpenseLimitService {

    private final com.bank.limits.domain.service.LimitService limitService;
    private final LimitMapper limitMapper;
    private final ExpenseLimitRepository limitRepository;

    @Transactional
    public LimitResponseDto setNewExpenseLimit(SetNewLimitRequestDto dto) {
        Limit newLimit = limitService.setLimit(limitMapper.toDomain(dto));
        ExpenseLimit expenseLimit = limitRepository.save(limitMapper.toEntity(newLimit));

        return limitMapper.toResponseDto(expenseLimit);

    }

    @Transactional(readOnly = true)
    public Page<LimitResponseDto> getLimits(String accountFrom, Pageable pageable) {

        return limitRepository
                .findByAccountFrom(accountFrom, pageable)
                .map(limitMapper::toResponseDto);
    }
}
