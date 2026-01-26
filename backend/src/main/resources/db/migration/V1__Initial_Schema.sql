CREATE TABLE IF NOT EXISTS transaction_types (
    id TINYINT PRIMARY KEY,
    name VARCHAR(20) NOT NULL -- Income, Expense, Transfer
);

CREATE TABLE IF NOT EXISTS account_categories (
    id TINYINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL -- Cash, Bank, Credit Card, etc.
);

CREATE TABLE IF NOT EXISTS users (
    id BINARY(16) PRIMARY KEY,
    id_text VARCHAR(36) GENERATED ALWAYS AS (BIN_TO_UUID(id)) VIRTUAL,
    cognito_sub BINARY(16) UNIQUE NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    profile_image_url VARCHAR(255) DEFAULT "default_image",
    default_currency VARCHAR(3) DEFAULT 'INR',
    first_day_of_week TINYINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS categories (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    parent_id BINARY(16) DEFAULT NULL,
    name VARCHAR(100) NOT NULL,
    transaction_type_id TINYINT NOT NULL,
    icon_type INT DEFAULT 0,
    colour VARCHAR(7) DEFAULT "#000000",
    order_index INT DEFAULT 0,
    is_hidden BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY(parent_id) REFERENCES categories(id) ON DELETE CASCADE,
    FOREIGN KEY(transaction_type_id) REFERENCES transaction_types(id)
);

CREATE TABLE IF NOT EXISTS accounts (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    name VARCHAR(100) NOT NULL,
    account_category_id TINYINT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    balance DECIMAL(19, 2) DEFAULT 0.00,
    icon_type INT DEFAULT 0,
    colour VARCHAR(7) DEFAULT "#000000",
    order_index INT DEFAULT 0,
    is_hidden BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY(account_category_id) REFERENCES account_categories(id)
);

CREATE TABLE IF NOT EXISTS transactions (
    id BINARY(16) PRIMARY KEY,
    id_text VARCHAR(36) GENERATED ALWAYS AS (BIN_TO_UUID(id)) VIRTUAL,
    user_id BINARY(16) NOT NULL,
    source_account_id BINARY(16) NOT NULL,
    destination_account_id BINARY(16) DEFAULT NULL, -- Only used for 'Transfer' type
    category_id BINARY(16) DEFAULT NULL,
    transaction_type_id TINYINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL DEFAULT 0.00,
    transaction_time DATETIME NOT NULL, -- User-defined date/time
    comment TEXT,
    geo_latitude DECIMAL(10, 8),
    geo_longitude DECIMAL(11, 8),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY(source_account_id) REFERENCES accounts(id),
    FOREIGN KEY(destination_account_id) REFERENCES accounts(id),
    FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE SET NULL,
    FOREIGN KEY(transaction_type_id) REFERENCES transaction_types(id)
);

CREATE TABLE IF NOT EXISTS tags (
    id BINARY(16) PRIMARY KEY,
    user_id BINARY(16) NOT NULL,
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS transaction_tags (
    transaction_id BINARY(16) NOT NULL,
    tag_id BINARY(16) NOT NULL,

    PRIMARY KEY (transaction_id, tag_id),
    FOREIGN KEY (transaction_id) REFERENCES transactions(id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE
);