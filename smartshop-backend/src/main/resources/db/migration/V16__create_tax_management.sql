-- V16: Tax Management System
-- Creates taxes table, tax_rates table (time-bound), and adds tax_id FK to products

CREATE TABLE taxes (
    id          UUID PRIMARY KEY,
    shop_id     UUID NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    type        VARCHAR(20)  NOT NULL DEFAULT 'PERCENTAGE',
    description VARCHAR(500),
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_taxes_shop_name UNIQUE (shop_id, name)
);

CREATE TABLE tax_rates (
    id          UUID PRIMARY KEY,
    tax_id      UUID NOT NULL REFERENCES taxes(id) ON DELETE CASCADE,
    rate        NUMERIC(7,4) NOT NULL CHECK (rate >= 0),
    valid_from  DATE NOT NULL,
    valid_to    DATE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_tax_rate_dates CHECK (valid_to IS NULL OR valid_to >= valid_from)
);

-- Add tax_id FK to products (nullable – fallback to legacy vat_rate if null)
ALTER TABLE products ADD COLUMN tax_id UUID REFERENCES taxes(id) ON DELETE SET NULL;

-- Indexes
CREATE INDEX idx_taxes_shop_id         ON taxes(shop_id);
CREATE INDEX idx_tax_rates_tax_id      ON tax_rates(tax_id);
CREATE INDEX idx_tax_rates_valid_from  ON tax_rates(valid_from);
CREATE INDEX idx_products_tax_id       ON products(tax_id);
