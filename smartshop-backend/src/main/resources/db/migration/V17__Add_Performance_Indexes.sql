-- V17: Performance Indexes
-- Adds covering indexes on date-range columns used heavily by reporting queries.
-- NOTE: CONCURRENTLY is PostgreSQL-only. In test (H2), standard CREATE INDEX is used.

-- ─── Sales ────────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_sales_bill_date
    ON sales (bill_date DESC);

CREATE INDEX IF NOT EXISTS idx_sales_shop_bill_date
    ON sales (shop_id, bill_date DESC);

CREATE INDEX IF NOT EXISTS idx_sales_branch_bill_date
    ON sales (branch_id, bill_date DESC);

-- ─── Purchases ────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_purchases_purchase_date
    ON purchases (purchase_date DESC);

CREATE INDEX IF NOT EXISTS idx_purchases_shop_purchase_date
    ON purchases (shop_id, purchase_date DESC);

CREATE INDEX IF NOT EXISTS idx_purchases_branch_purchase_date
    ON purchases (branch_id, purchase_date DESC);

-- ─── Expenses ─────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_expenses_expense_date
    ON expenses (expense_date DESC);

CREATE INDEX IF NOT EXISTS idx_expenses_branch_expense_date
    ON expenses (branch_id, expense_date DESC);

-- ─── Stock Movements ──────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_stock_movements_created_at
    ON stock_movements (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_stock_movements_branch_created_at
    ON stock_movements (branch_id, created_at DESC);

-- ─── Inventory ────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_inventories_product_branch
    ON inventories (product_id, branch_id);

-- ─── Audit Logs ───────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at
    ON audit_logs (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_logs_entity
    ON audit_logs (entity_name, entity_id);

-- ─── Sale Items ───────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_sale_items_product_id
    ON sale_items (product_id);

-- ─── Purchase Items ───────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_purchase_items_product_id
    ON purchase_items (product_id);
