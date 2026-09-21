ALTER TABLE purchase_items
    ADD COLUMN extra_cost NUMERIC(12, 2) NOT NULL DEFAULT 0;

ALTER TABLE sale_items
    ADD COLUMN unit_cost_at_sale NUMERIC(12, 2) NOT NULL DEFAULT 0;

ALTER TABLE sale_items
    ADD COLUMN line_profit NUMERIC(12, 2) NOT NULL DEFAULT 0;