package com.financelog.backend.repo;

import com.financelog.backend.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByUser(User user);

    List<Transaction> findByUserAndTransactionTimeBetween(
            User user,
            LocalDateTime from,
            LocalDateTime to
    );

    List<Transaction> findByUserAndSourceAccount(User user, Account account);

    List<Transaction> findByUserAndCategory(User user, Category category);

    List<Transaction> findByUserAndTagsContaining(User user, Tag tag);

    Page<Transaction> findByUser(
            User user,
            Pageable pageable
    );

    List<Transaction> findByUserAndSourceAccountAndTransactionTimeBetween(
            User user,
            Account account,
            LocalDateTime from,
            LocalDateTime to
    );
}
