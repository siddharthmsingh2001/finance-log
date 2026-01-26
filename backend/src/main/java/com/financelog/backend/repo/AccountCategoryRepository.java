package com.financelog.backend.repo;

import com.financelog.backend.entity.AccountCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountCategoryRepository extends JpaRepository<AccountCategory, Integer> {
}
