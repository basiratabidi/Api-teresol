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