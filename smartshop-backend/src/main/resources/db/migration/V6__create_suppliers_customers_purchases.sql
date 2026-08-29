CREATE TABLE suppliers (
    id uuid PRIMARY KEY,
    shop_id uuid NOT NULL REFERENCES shops(id),
    name varchar(200) NOT NULL,
    company_name varchar(200),
    phone varchar(20),
    email varchar(255),
    address varchar(255),
    pan_number varchar(50),
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT chk_suppliers_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE TABLE customers (
    id uuid PRIMARY KEY,
    shop_id uuid NOT NULL REFERENCES shops(id),
    name varchar(200) NOT NULL,
    phone varchar(20),
    email varchar(255),
    address varchar(255),
    loyalty_points numeric(12,2) NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE purchases (
    id uuid PRIMARY KEY,
    shop_id uuid NOT NULL REFERENCES shops(id),
    branch_id uuid NOT NULL REFERENCES branches(id),
    supplier_id uuid NOT NULL REFERENCES suppliers(id),
    purchase_number varchar(50) NOT NULL UNIQUE,
    purchase_date date NOT NULL,
    subtotal numeric(12,2) NOT NULL DEFAULT 0,
    discount_amount numeric(12,2) NOT NULL DEFAULT 0,
    taxable_amount numeric(12,2) NOT NULL DEFAULT 0,
    vat_amount numeric(12,2) NOT NULL DEFAULT 0,
    total_amount numeric(12,2) NOT NULL DEFAULT 0,
    payment_status varchar(20) NOT NULL DEFAULT 'UNPAID',
    notes varchar(1000),
    created_by uuid NOT NULL REFERENCES users(id),
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT chk_purchase_payment_status CHECK (payment_status IN ('UNPAID','PARTIAL','PAID'))
);

CREATE TABLE purchase_items (
    id uuid PRIMARY KEY,
    purchase_id uuid NOT NULL REFERENCES purchases(id) ON DELETE CASCADE,
    product_id uuid NOT NULL REFERENCES products(id),
    quantity numeric(12,2) NOT NULL,
    unit_cost numeric(12,2) NOT NULL,
    discount_amount numeric(12,2) NOT NULL DEFAULT 0,
    vat_rate numeric(5,2) NOT NULL DEFAULT 0,
    vat_amount numeric(12,2) NOT NULL DEFAULT 0,
    line_total numeric(12,2) NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT chk_purchase_item_qty CHECK (quantity > 0)
);

CREATE INDEX idx_purchases_shop ON purchases (shop_id);
CREATE INDEX idx_purchases_branch ON purchases (branch_id);