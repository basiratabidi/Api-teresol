CREATE TABLE branch (
    branch_id     BIGSERIAL PRIMARY KEY,
    branch_code   VARCHAR(20) NOT NULL UNIQUE,
    branch_name   VARCHAR(100) NOT NULL,
    city          VARCHAR(50) NOT NULL,
    region_code   VARCHAR(10) NOT NULL,
    is_active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO branch (branch_code, branch_name, city, region_code) VALUES
    ('WE-001', 'Demo Bank West Main', 'Westville', 'WEST'),
    ('WE-002', 'Demo Bank West Plaza', 'Westville', 'WEST');

CREATE TABLE account (
    account_id          BIGSERIAL PRIMARY KEY,
    account_number      VARCHAR(20) NOT NULL UNIQUE,
    account_holder_name VARCHAR(100) NOT NULL,
    branch_code         VARCHAR(20) NOT NULL,
    account_type        VARCHAR(20) NOT NULL,
    balance             NUMERIC(18,2) NOT NULL DEFAULT 0,
    region_code         VARCHAR(10) NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO account (account_number, account_holder_name, branch_code, account_type, balance, region_code) VALUES
    ('WE-AC-0001', 'Zara Sheikh', 'WE-001', 'SAVINGS', 9800.00, 'WEST'),
    ('WE-AC-0002', 'Imran Qureshi', 'WE-002', 'CURRENT', 30250.00, 'WEST');
