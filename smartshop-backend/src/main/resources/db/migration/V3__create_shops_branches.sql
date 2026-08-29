CREATE TABLE shops (
    id uuid PRIMARY KEY,
    name varchar(200) NOT NULL,
    pan_vat_number varchar(50) NOT NULL UNIQUE,
    phone varchar(20),
    email varchar(255),
    address varchar(255),
    logo_url varchar(500),
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT chk_shops_status CHECK (status IN ('ACTIVE','INACTIVE','SUSPENDED'))
);

CREATE TABLE branches (
    id uuid PRIMARY KEY,
    shop_id uuid NOT NULL REFERENCES shops(id),
    name varchar(200) NOT NULL,
    code varchar(20) NOT NULL,
    address varchar(255),
    contact_number varchar(20),
    is_main_branch boolean NOT NULL DEFAULT false,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT uq_branches_shop_code UNIQUE (shop_id, code),
    CONSTRAINT chk_branches_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE INDEX idx_branches_shop_id ON branches (shop_id);