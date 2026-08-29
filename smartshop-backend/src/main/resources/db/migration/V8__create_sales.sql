CREATE TABLE sales (
    id uuid PRIMARY KEY,
    shop_id uuid NOT NULL REFERENCES shops(id),
    branch_id uuid NOT NULL REFERENCES branches(id),
    customer_id uuid REFERENCES customers(id),
    invoice_number varchar(50) NOT NULL,
    bill_date date NOT NULL,
    subtotal numeric(12,2) NOT NULL DEFAULT 0,
    discount_amount numeric(12,2) NOT NULL DEFAULT 0,
    taxable_amount numeric(12,2) NOT NULL DEFAULT 0,
    vat_amount numeric(12,2) NOT NULL DEFAULT 0,
    total_amount numeric(12,2) NOT NULL DEFAULT 0,
    payment_status varchar(20) NOT NULL DEFAULT 'UNPAID',
    payment_method varchar(20),
    qr_reference varchar(100),
    cashier_id uuid NOT NULL REFERENCES users(id),
    remarks varchar(1000),
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT uq_sales_shop_invoice UNIQUE (shop_id, invoice_number),
    CONSTRAINT chk_sale_payment_status CHECK (payment_status IN ('UNPAID','PARTIAL','PAID'))
);

CREATE TABLE sale_items (
    id uuid PRIMARY KEY,
    sale_id uuid NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    product_id uuid NOT NULL REFERENCES products(id),
    quantity numeric(12,2) NOT NULL,
    unit_price numeric(12,2) NOT NULL,
    discount_amount numeric(12,2) NOT NULL DEFAULT 0,
    vat_rate numeric(5,2) NOT NULL DEFAULT 0,
    vat_amount numeric(12,2) NOT NULL DEFAULT 0,
    line_total numeric(12,2) NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT chk_sale_item_qty CHECK (quantity > 0)
);

CREATE INDEX idx_sales_shop ON sales (shop_id);
CREATE INDEX idx_sales_branch ON sales (branch_id);