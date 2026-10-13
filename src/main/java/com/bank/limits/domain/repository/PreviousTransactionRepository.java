package com.bank.limits.domain.repository;

import com.bank.limits.domain.model.PreviousTransaction;

import java.util.List;

public interface PreviousTransactionRepository {

    public List<PreviousTransaction> getPreviousTransactions();

}
