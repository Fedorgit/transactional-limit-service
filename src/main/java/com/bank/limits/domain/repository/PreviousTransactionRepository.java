package com.bank.limits.domain.repository;

import com.bank.limits.domain.model.PreviousTransaction;
import com.bank.limits.domain.model.Transaction;

import java.util.Set;

public interface PreviousTransactionRepository {

    public Set<PreviousTransaction> getPreviousTransactions();

}
