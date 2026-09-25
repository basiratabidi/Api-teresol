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
    ('EA-001', 'Demo Bank East Main', 'Eastville', 'EAST'),
    ('EA-002', 'Demo Bank East Plaza', 'Eastville', 'EAST');

CREATE TABLE account (
    account_id          BIGSERIAL PRIMARY KEY,
    account_number      VARCHAR(20) NOT NULL UNIQUE,
    account_holder_name VARCHAR(100) NOT NULL,
    branch_code         VARCHAR(20) NOT NULL REFERENCES branch(branch_code),
    account_type        VARCHAR(20) NOT NULL,
    balance             NUMERIC(18,2) NOT NULL DEFAULT 0,
    region_code         VARCHAR(10) NOT NULL,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO account (account_number, account_holder_name, branch_code, account_type, balance, region_code) VALUES
    ('EA-AC-0001', 'Hassan Raza', 'EA-001', 'SAVINGS', 12300.00, 'EAST'),
    ('EA-AC-0002', 'Fatima Noor', 'EA-002', 'CURRENT', 5600.25, 'EAST');
