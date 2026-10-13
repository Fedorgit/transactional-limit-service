package com.bank.limits.application.mapper;

import com.bank.limits.domain.model.ExpenseCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ExpenseCategoryMapper {


    @Mapping(target = "category", source = "name")
    ExpenseCategory toDomain(com.bank.limits.infrastructure.persistence.entity.ExpenseCategory expenseCategory);

}
