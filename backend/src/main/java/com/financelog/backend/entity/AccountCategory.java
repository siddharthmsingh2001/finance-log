package com.financelog.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "account_categories")
public class AccountCategory {

    @Id
    @Column(name = "id", updatable = false, unique = true, nullable = false)
    private Integer id; // 1: Cash, 2: Bank, 3: Credit Card, etc.

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
