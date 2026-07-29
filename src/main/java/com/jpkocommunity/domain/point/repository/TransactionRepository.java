package com.jpkocommunity.domain.point.repository;

import com.jpkocommunity.domain.point.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
