package com.financelog.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "BINARY(16)", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_category_id", referencedColumnName = "id", nullable = false)
    private AccountCategory accountCategory;

    @Column(nullable = false, length = 3)
    private String currency = "INR";

    @Column(precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "icon_type")
    private Integer iconType = 0;

    @Column(length = 7)
    private String colour = "#000000";

    @Column(name = "order_index")
    private Integer orderIndex = 0;

    @Column(name = "is_hidden")
    private Boolean isHidden = false;

    public static Account create(User user, String name, AccountCategory accountCategory, String currency, BigDecimal balance){
        return new Account()
                .setUser(user)
                .setName(name)
                .setAccountCategory(accountCategory)
                .setCurrency(currency)
                .setBalance(balance);
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public AccountCategory getAccountCategory() {
        return accountCategory;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Integer getIconType() {
        return iconType;
    }

    public String getColour() {
        return colour;
    }

    public Boolean getHidden() {
        return isHidden;
    }


    public Account setHidden(Boolean hidden) {
        this.isHidden = hidden;
        return this;
    }

    public Account setOrderIndex(Integer orderIndex){
        this.orderIndex = orderIndex;
        return this;
    }

    public Account setColour(String colour) {
        this.colour = colour;
        return this;
    }

    public Account setIconType(Integer iconType) {
        this.iconType = iconType;
        return this;
    }

    public Account setBalance(BigDecimal balance) {
        this.balance = balance;
        return this;
    }

    public Account setCurrency(String currency) {
        this.currency = currency;
        return this;
    }

    public Account setAccountCategory(AccountCategory accountCategory) {
        this.accountCategory = accountCategory;
        return this;
    }

    public Account setName(String name) {
        this.name = name;
        return this;
    }

    public Account setUser(User user) {
        this.user = user;
        return this;
    }

}
