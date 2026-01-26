package com.financelog.backend.repo;

import com.financelog.backend.entity.Category;
import com.financelog.backend.entity.TransactionType;
import com.financelog.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByUser(User user);

    List<Category> findByUserAndTransactionType(User user, TransactionType transactionType);

    List<Category> findByUserAndIsHiddenFalseOrderByOrderIndexAsc(User user);

    List<Category> findByParent(Category parent);


}
