CREATE TABLE sale_returns (
    id uuid PRIMARY KEY,
    sale_id uuid NOT NULL REFERENCES sales(id),
    branch_id uuid NOT NULL REFERENCES branches(id),
    return_number varchar(50) NOT NULL UNIQUE,
    return_date date NOT NULL,
    reason varchar(1000),
    refund_amount numeric(12,2) NOT NULL DEFAULT 0,
    created_by uuid NOT NULL REFERENCES users(id),
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE sale_return_items (
    id uuid PRIMARY KEY,
    sale_return_id uuid NOT NULL REFERENCES sale_returns(id) ON DELETE CASCADE,
    sale_item_id uuid NOT NULL REFERENCES sale_items(id),
    product_id uuid NOT NULL REFERENCES products(id),
    quantity numeric(12,2) NOT NULL,
    unit_price numeric(12,2) NOT NULL,
    line_total numeric(12,2) NOT NULL DEFAULT 0,
    CONSTRAINT chk_sale_return_item_qty CHECK (quantity > 0)
);

CREATE TABLE purchase_returns (
    id uuid PRIMARY KEY,
    purchase_id uuid NOT NULL REFERENCES purchases(id),
    branch_id uuid NOT NULL REFERENCES branches(id),
    return_number varchar(50) NOT NULL UNIQUE,
    return_date date NOT NULL,
    reason varchar(1000),
    refund_amount numeric(12,2) NOT NULL DEFAULT 0,
    created_by uuid NOT NULL REFERENCES users(id),
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now()
);

CREATE TABLE purchase_return_items (
    id uuid PRIMARY KEY,
    purchase_return_id uuid NOT NULL REFERENCES purchase_returns(id) ON DELETE CASCADE,
    purchase_item_id uuid NOT NULL REFERENCES purchase_items(id),
    product_id uuid NOT NULL REFERENCES products(id),
    quantity numeric(12,2) NOT NULL,
    unit_cost numeric(12,2) NOT NULL,
    line_total numeric(12,2) NOT NULL DEFAULT 0,
    CONSTRAINT chk_purchase_return_item_qty CHECK (quantity > 0)
);

CREATE TABLE stock_transfers (
    id uuid PRIMARY KEY,
    shop_id uuid NOT NULL REFERENCES shops(id),
    from_branch_id uuid NOT NULL REFERENCES branches(id),
    to_branch_id uuid NOT NULL REFERENCES branches(id),
    transfer_number varchar(50) NOT NULL UNIQUE,
    status varchar(20) NOT NULL DEFAULT 'PENDING',
    note varchar(1000),
    created_by uuid NOT NULL REFERENCES users(id),
    approved_by uuid REFERENCES users(id),
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT chk_transfer_status CHECK (status IN ('PENDING','APPROVED','REJECTED'))
);

CREATE TABLE stock_transfer_items (
    id uuid PRIMARY KEY,
    stock_transfer_id uuid NOT NULL REFERENCES stock_transfers(id) ON DELETE CASCADE,
    product_id uuid NOT NULL REFERENCES products(id),
    quantity numeric(12,2) NOT NULL,
    CONSTRAINT chk_transfer_item_qty CHECK (quantity > 0)
);

CREATE INDEX idx_sale_returns_sale ON sale_returns (sale_id);
CREATE INDEX idx_purchase_returns_purchase ON purchase_returns (purchase_id);