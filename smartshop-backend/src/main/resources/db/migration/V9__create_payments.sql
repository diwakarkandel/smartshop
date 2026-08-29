CREATE TABLE payments (
    id uuid PRIMARY KEY,
    sale_id uuid NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    amount numeric(12,2) NOT NULL,
    payment_method varchar(20) NOT NULL,
    payment_status varchar(20) NOT NULL DEFAULT 'COMPLETED',
    reference_number varchar(100),
    paid_at timestamp NOT NULL DEFAULT now(),
    received_by uuid NOT NULL REFERENCES users(id),
    created_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT chk_payment_method CHECK (payment_method IN ('CASH','CARD','ESEWA','KHALTI','BANK_TRANSFER','OTHER')),
    CONSTRAINT chk_payment_status CHECK (payment_status IN ('PENDING','COMPLETED','FAILED','REFUNDED'))
);

CREATE INDEX idx_payments_sale ON payments (sale_id);