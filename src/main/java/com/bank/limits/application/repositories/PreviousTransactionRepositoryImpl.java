package com.bank.limits.application.repositories;

import com.bank.limits.domain.model.PreviousTransaction;
import com.bank.limits.domain.repository.PreviousTransactionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PreviousTransactionRepositoryImpl implements PreviousTransactionRepository {
    @Override
    public List<PreviousTransaction> getPreviousTransactions() {
        return List.of();
    }
}
