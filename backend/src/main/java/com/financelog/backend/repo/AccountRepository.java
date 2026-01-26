package com.financelog.backend.repo;

import com.financelog.backend.entity.Account;
import com.financelog.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    List<Account> findByUser(User user);

    List<Account> findByUserAndIsHiddenFalseOrderByOrderIndexAsc(User user);

    Optional<Account> findByIdAndUser(UUID id, User user);

}
