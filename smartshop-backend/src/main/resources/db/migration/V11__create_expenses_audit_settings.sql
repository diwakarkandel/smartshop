CREATE TABLE expenses (
    id uuid PRIMARY KEY,
    shop_id uuid NOT NULL REFERENCES shops(id),
    branch_id uuid REFERENCES branches(id),
    title varchar(200) NOT NULL,
    category varchar(100),
    amount numeric(12,2) NOT NULL,
    expense_date date NOT NULL,
    payment_method varchar(20),
    note varchar(1000),
    created_by uuid NOT NULL REFERENCES users(id),
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE audit_logs (
    id uuid PRIMARY KEY,
    user_id uuid REFERENCES users(id),
    action varchar(50) NOT NULL,
    entity_name varchar(100),
    entity_id varchar(36),
    old_value text,
    new_value text,
    ip_address varchar(45),
    created_at timestamp NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_user ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_name, entity_id);

CREATE TABLE settings (
    id uuid PRIMARY KEY,
    shop_id uuid NOT NULL REFERENCES shops(id),
    setting_key varchar(100) NOT NULL,
    setting_value varchar(500),
    description varchar(255),
    updated_by uuid REFERENCES users(id),
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT uq_settings_shop_key UNIQUE (shop_id, setting_key)
);