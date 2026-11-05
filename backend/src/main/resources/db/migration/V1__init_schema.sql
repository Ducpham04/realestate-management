-- Core schema for the real estate management system.
-- All timestamps are stored in UTC (hibernate.jdbc.time_zone=UTC).

CREATE TABLE users
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone         VARCHAR(20),
    role          VARCHAR(20)  NOT NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    version       BIGINT       NOT NULL DEFAULT 0,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'AGENT'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE refresh_tokens
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    revoked_at DATETIME(6),
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_refresh_tokens_user (user_id),
    INDEX idx_refresh_tokens_expires (expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE properties
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    title            VARCHAR(200)   NOT NULL,
    description      TEXT,
    type             VARCHAR(20)    NOT NULL,
    listing_type     VARCHAR(10)    NOT NULL,
    status           VARCHAR(20)    NOT NULL,
    price            DECIMAL(15, 2) NOT NULL,
    area             DECIMAL(10, 2) NOT NULL,
    bedrooms         INT,
    bathrooms        INT,
    address_street   VARCHAR(255)   NOT NULL,
    address_ward     VARCHAR(100),
    address_district VARCHAR(100)   NOT NULL,
    address_city     VARCHAR(100)   NOT NULL,
    agent_id         BIGINT         NOT NULL,
    version          BIGINT         NOT NULL DEFAULT 0,
    created_at       DATETIME(6)    NOT NULL,
    updated_at       DATETIME(6)    NOT NULL,
    CONSTRAINT fk_properties_agent FOREIGN KEY (agent_id) REFERENCES users (id),
    CONSTRAINT ck_properties_price CHECK (price > 0),
    CONSTRAINT ck_properties_area CHECK (area > 0),
    INDEX idx_properties_status (status),
    INDEX idx_properties_type (type),
    INDEX idx_properties_price (price),
    INDEX idx_properties_city_district (address_city, address_district),
    INDEX idx_properties_agent (agent_id),
    INDEX idx_properties_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE property_images
(
    property_id BIGINT       NOT NULL,
    position    INT          NOT NULL,
    url         VARCHAR(500) NOT NULL,
    PRIMARY KEY (property_id, position),
    CONSTRAINT fk_property_images_property FOREIGN KEY (property_id) REFERENCES properties (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE customers
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name          VARCHAR(100) NOT NULL,
    email              VARCHAR(150),
    phone              VARCHAR(20)  NOT NULL,
    type               VARCHAR(20)  NOT NULL,
    budget_min         DECIMAL(15, 2),
    budget_max         DECIMAL(15, 2),
    preferred_location VARCHAR(200),
    notes              TEXT,
    agent_id           BIGINT       NOT NULL,
    version            BIGINT       NOT NULL DEFAULT 0,
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6)  NOT NULL,
    CONSTRAINT uk_customers_phone UNIQUE (phone),
    CONSTRAINT fk_customers_agent FOREIGN KEY (agent_id) REFERENCES users (id),
    INDEX idx_customers_agent (agent_id),
    INDEX idx_customers_name (full_name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE appointments
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id  BIGINT      NOT NULL,
    customer_id  BIGINT      NOT NULL,
    agent_id     BIGINT      NOT NULL,
    scheduled_at DATETIME(6) NOT NULL,
    end_at       DATETIME(6) NOT NULL,
    status       VARCHAR(20) NOT NULL,
    note         VARCHAR(1000),
    version      BIGINT      NOT NULL DEFAULT 0,
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    CONSTRAINT fk_appointments_property FOREIGN KEY (property_id) REFERENCES properties (id),
    CONSTRAINT fk_appointments_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_appointments_agent FOREIGN KEY (agent_id) REFERENCES users (id),
    CONSTRAINT ck_appointments_range CHECK (end_at > scheduled_at),
    INDEX idx_appointments_agent_time (agent_id, scheduled_at),
    INDEX idx_appointments_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE deals
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT         NOT NULL,
    customer_id BIGINT         NOT NULL,
    agent_id    BIGINT         NOT NULL,
    amount      DECIMAL(15, 2) NOT NULL,
    commission  DECIMAL(15, 2) NOT NULL,
    status      VARCHAR(20)    NOT NULL,
    closed_at   DATETIME(6),
    notes       TEXT,
    version     BIGINT         NOT NULL DEFAULT 0,
    created_at  DATETIME(6)    NOT NULL,
    updated_at  DATETIME(6)    NOT NULL,
    CONSTRAINT fk_deals_property FOREIGN KEY (property_id) REFERENCES properties (id),
    CONSTRAINT fk_deals_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT fk_deals_agent FOREIGN KEY (agent_id) REFERENCES users (id),
    CONSTRAINT ck_deals_amount CHECK (amount > 0 AND commission >= 0),
    INDEX idx_deals_status_closed (status, closed_at),
    INDEX idx_deals_agent (agent_id),
    INDEX idx_deals_property (property_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
