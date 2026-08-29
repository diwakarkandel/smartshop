CREATE TABLE categories (
    id uuid PRIMARY KEY,
    shop_id uuid NOT NULL REFERENCES shops(id),
    parent_id uuid REFERENCES categories(id),
    name varchar(200) NOT NULL,
    code varchar(50),
    description varchar(500),
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT chk_categories_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE TABLE products (
    id uuid PRIMARY KEY,
    shop_id uuid NOT NULL REFERENCES shops(id),
    category_id uuid REFERENCES categories(id),
    name varchar(200) NOT NULL,
    sku varchar(50) NOT NULL,
    barcode varchar(100),
    brand varchar(100),
    unit varchar(20) NOT NULL DEFAULT 'PCS',
    description varchar(500),
    purchase_price numeric(12,2) NOT NULL DEFAULT 0,
    selling_price numeric(12,2) NOT NULL DEFAULT 0,
    vat_applicable boolean NOT NULL DEFAULT true,
    vat_rate numeric(5,2) NOT NULL DEFAULT 13.00,
    reorder_level numeric(12,2) NOT NULL DEFAULT 0,
    image_url varchar(500),
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT uq_products_shop_sku UNIQUE (shop_id, sku),
    CONSTRAINT chk_products_status CHECK (status IN ('ACTIVE','INACTIVE','DISCONTINUED'))
);

CREATE INDEX idx_products_shop ON products (shop_id);
CREATE INDEX idx_products_category ON products (category_id);
CREATE INDEX idx_categories_shop ON categories (shop_id);