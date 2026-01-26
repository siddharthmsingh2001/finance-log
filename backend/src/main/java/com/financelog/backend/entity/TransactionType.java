package com.financelog.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transaction_types")
public class TransactionType {

    @Id
    @Column(name = "id", updatable = false, unique = true, nullable = false)
    private Integer id; // 1: Income, 2: Expense, 3: Transfer

    @Column(name = "name", nullable = false, length = 20)
    private String name;

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
