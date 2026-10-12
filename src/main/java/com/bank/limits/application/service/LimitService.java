package com.bank.limits.application.service;

import com.bank.limits.application.mapper.LimitMapper;
import com.bank.limits.domain.model.Limit;
import com.bank.limits.infrastructure.persistence.entity.ExpenseLimit;
import com.bank.limits.infrastructure.persistence.repository.ExpenseLimitRepository;
import com.bank.limits.infrastructure.rest.dto.LimitResponseDto;
import com.bank.limits.infrastructure.rest.dto.SetNewLimitRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LimitService {

    private final com.bank.limits.domain.service.LimitService limitService;
    private final LimitMapper limitMapper;
    private final ExpenseLimitRepository limitRepository;

    public LimitResponseDto setNewExpenseLimit(SetNewLimitRequestDto dto){
        Limit newLimit= limitService.setLimit(limitMapper.toDomain(dto));
        ExpenseLimit expenseLimit= limitRepository.save(limitMapper.toEntity(newLimit));

        return limitMapper.toResponseDto(expenseLimit);

    }

}
