CREATE TABLE currency
(
    code         VARCHAR(3) PRIMARY KEY,
    numeric_code SMALLINT NOT NULL UNIQUE,
    CHECK (numeric_code BETWEEN 1 AND 999)
);

CREATE TABLE expense_category
(
    id   INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE expense_limit
(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    set_datetime        TIMESTAMPTZ    NOT NULL,
    amount              NUMERIC(19, 2) NOT NULL,
    currency_code       VARCHAR(3)     NOT NULL,
    account_from        VARCHAR(10)    NOT NULL,
    expense_category_id INTEGER        NOT NULL,

    CONSTRAINT fk_limit_currency
        FOREIGN KEY (currency_code)
            REFERENCES currency (code),

    CONSTRAINT fk_limit_category
        FOREIGN KEY (expense_category_id)
            REFERENCES expense_category (id),

    CONSTRAINT chk_limit_amount
        CHECK (amount > 0),

    CONSTRAINT chk_limit_account
        CHECK (account_from ~ '^[0-9]{10}$')
);

CREATE TABLE transaction
(
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    datetime            TIMESTAMPTZ    NOT NULL,
    account_from        VARCHAR(10)    NOT NULL,
    account_to          VARCHAR(10)    NOT NULL,
    amount              NUMERIC(19, 2) NOT NULL,
    currency_code       VARCHAR(3)     NOT NULL,
    expense_category_id INTEGER        NOT NULL,
    limit_exceeded      BOOLEAN        NOT NULL,

    CONSTRAINT fk_transaction_currency
        FOREIGN KEY (currency_code)
            REFERENCES currency (code),

    CONSTRAINT fk_transaction_category
        FOREIGN KEY (expense_category_id)
            REFERENCES expense_category (id),

    CONSTRAINT chk_transaction_amount
        CHECK (amount > 0),

    CONSTRAINT chk_transaction_account_from
        CHECK (account_from ~ '^[0-9]{10}$'),

    CONSTRAINT chk_transaction_account_to
        CHECK (account_to ~ '^[0-9]{10}$')
);

CREATE TABLE exchange_rate
(
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    base_currency   VARCHAR(3)     NOT NULL,
    target_currency VARCHAR(3)     NOT NULL,
    rate            NUMERIC(15, 4) NOT NULL,
    rate_date       DATE           NOT NULL,

    CONSTRAINT fk_exchange_base_currency
        FOREIGN KEY (base_currency)
            REFERENCES currency (code),

    CONSTRAINT fk_exchange_target_currency
        FOREIGN KEY (target_currency)
            REFERENCES currency (code),

    CONSTRAINT chk_exchange_rate
        CHECK (rate > 0),

    CONSTRAINT chk_exchange_currencies
        CHECK (base_currency <> target_currency)
);