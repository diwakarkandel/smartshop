
CREATE TABLE purchase_payments (
    id UUID PRIMARY KEY,
    purchase_id UUID NOT NULL REFERENCES purchases(id) ON DELETE CASCADE,
    payment_date DATE NOT NULL,
    amount NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    payment_method VARCHAR(20) NOT NULL,
    reference_number VARCHAR(100),
    notes VARCHAR(500),
    created_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_purchase_payments_purchase_id
    ON purchase_payments (purchase_id);

CREATE INDEX IF NOT EXISTS idx_purchase_payments_date
    ON purchase_payments (payment_date DESC);
