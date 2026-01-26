package com.financelog.backend.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "BINARY(16)", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "id_text", insertable = false, updatable = false)
    private String idText;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_account_id", referencedColumnName = "id", nullable = false)
    private Account sourceAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_account_id", referencedColumnName = "id", nullable = true)
    private Account destinationAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", referencedColumnName = "id", nullable = true)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_type_id", referencedColumnName = "id", nullable = false)
    private TransactionType transactionType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "transaction_time", nullable = false)
    private LocalDateTime transactionTime;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @ManyToMany
    @JoinTable(
            name = "transaction_tags",
            joinColumns = @JoinColumn(name = "transaction_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    private Set<Tag> tags = new HashSet<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static Transaction create(
            User user,
            Account sourceAccount,
            Account destinationAccount,
            Category category,
            TransactionType transactionType,
            BigDecimal amount,
            LocalDateTime transactionTime,
            String comment
    ){
        return new Transaction()
                .setUser(user)
                .setSourceAccount(sourceAccount)
                .setDestinationAccount(destinationAccount)
                .setCategory(category)
                .setTransactionType(transactionType)
                .setAmount(amount)
                .setTransactionTime(transactionTime)
                .setComment(comment);
    }

    public UUID getId() {
        return id;
    }

    public String getIdText() {
        return idText;
    }

    public User getUser() {
        return user;
    }

    public Account getSourceAccount() {
        return sourceAccount;
    }

    public Account getDestinationAccount() {
        return destinationAccount;
    }

    public Category getCategory() {
        return category;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getTransactionTime() {
        return transactionTime;
    }

    public String getComment() {
        return comment;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Transaction setUser(User user) {
        this.user = user;
        return this;
    }

    public Transaction setSourceAccount(Account sourceAccount) {
        this.sourceAccount = sourceAccount;
        return this;
    }

    public Transaction setDestinationAccount(Account destinationAccount) {
        this.destinationAccount = destinationAccount;
        return this;
    }

    public Transaction setCategory(Category category) {
        this.category = category;
        return this;
    }

    public Transaction setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
        return this;
    }

    public Transaction setAmount(BigDecimal amount) {
        this.amount = amount;
        return this;
    }

    public Transaction setTransactionTime(LocalDateTime transactionTime) {
        this.transactionTime = transactionTime;
        return this;
    }

    public Transaction setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public Transaction setTags(Set<Tag> tags) {
        this.tags = tags;
        return this;
    }
}
