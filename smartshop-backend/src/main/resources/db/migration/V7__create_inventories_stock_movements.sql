CREATE TABLE inventories (
    id uuid PRIMARY KEY,
    branch_id uuid NOT NULL REFERENCES branches(id),
    product_id uuid NOT NULL REFERENCES products(id),
    quantity_available numeric(12,2) NOT NULL DEFAULT 0,
    quantity_reserved numeric(12,2) NOT NULL DEFAULT 0,
    average_cost numeric(12,2) NOT NULL DEFAULT 0,
    last_stock_update timestamp,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT uq_inventories_branch_product UNIQUE (branch_id, product_id),
    CONSTRAINT chk_quantity_available CHECK (quantity_available >= 0),
    CONSTRAINT chk_quantity_reserved CHECK (quantity_reserved >= 0)
);

CREATE TABLE stock_movements (
    id uuid PRIMARY KEY,
    branch_id uuid NOT NULL REFERENCES branches(id),
    product_id uuid NOT NULL REFERENCES products(id),
    movement_type varchar(30) NOT NULL,
    quantity numeric(12,2) NOT NULL,
    reference_type varchar(30),
    reference_id uuid,
    note varchar(1000),
    created_by uuid REFERENCES users(id),
    created_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT chk_movement_type CHECK (movement_type IN ('PURCHASE_IN','SALE_OUT','SALE_RETURN_IN','PURCHASE_RETURN_OUT','ADJUSTMENT_IN','ADJUSTMENT_OUT','TRANSFER_IN','TRANSFER_OUT','OPENING_STOCK'))
);

CREATE INDEX idx_inventories_branch ON inventories (branch_id);
CREATE INDEX idx_inventories_product ON inventories (product_id);
CREATE INDEX idx_stock_movements_product_branch ON stock_movements (product_id, branch_id);