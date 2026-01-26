package com.financelog.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "categories")
public class Category {

    public Category(){}

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "BINARY(16)", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", referencedColumnName = "id", nullable = true)
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
    private List<Category> subCategories = new ArrayList<>();

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_type_id", referencedColumnName = "id", nullable = false)
    private TransactionType transactionType;

    @Column(name = "icon_type")
    private Integer iconType = 0;

    @Column(length = 7)
    private String colour = "#000000";

    @Column(name = "order_index")
    private Integer orderIndex = 0;

    @Column(name = "is_hidden")
    private Boolean isHidden = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static Category create(User user, Category parent, String name, TransactionType transactionType ){
        return new Category()
                .setUser(user)
                .setParent(parent)
                .setName(name)
                .setTransactionType(transactionType);
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Category getParent() {
        return parent;
    }

    public List<Category> getSubCategories() {
        return subCategories;
    }

    public String getName() {
        return name;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public Integer getIconType() {
        return iconType;
    }

    public String getColour() {
        return colour;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public Boolean getHidden() {
        return isHidden;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Category setUser(User user) {
        this.user = user;
        return this;
    }

    public Category setParent(Category parent) {
        this.parent = parent;
        return this;
    }

    public Category setName(String name) {
        this.name = name;
        return this;
    }

    public Category setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
        return this;
    }

    public Category setIconType(Integer iconType) {
        this.iconType = iconType;
        return this;
    }

    public Category setColour(String colour) {
        this.colour = colour;
        return this;
    }

    public Category setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
        return this;
    }

    public Category setHidden(Boolean hidden) {
        isHidden = hidden;
        return this;
    }
}
