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
    ('SO-001', 'Demo Bank South Main', 'Southville', 'SOUTH'),
    ('SO-002', 'Demo Bank South Plaza', 'Southville', 'SOUTH');

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
    ('SO-AC-0001', 'Sana Malik', 'SO-001', 'SAVINGS', 8700.00, 'SOUTH'),
    ('SO-AC-0002', 'Usman Tariq', 'SO-002', 'CURRENT', 21000.75, 'SOUTH');
