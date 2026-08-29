-- Sales date and composite indexes for reports
CREATE INDEX IF NOT EXISTS idx_sales_bill_date ON sales (bill_date DESC);
CREATE INDEX IF NOT EXISTS idx_sales_shop_bill_date ON sales (shop_id, bill_date DESC);
CREATE INDEX IF NOT EXISTS idx_sales_branch_bill_date ON sales (branch_id, bill_date DESC);

-- Purchases date and composite indexes
CREATE INDEX IF NOT EXISTS idx_purchases_purchase_date ON purchases (purchase_date DESC);
CREATE INDEX IF NOT EXISTS idx_purchases_shop_purchase_date ON purchases (shop_id, purchase_date DESC);
CREATE INDEX IF NOT EXISTS idx_purchases_branch_purchase_date ON purchases (branch_id, purchase_date DESC);

-- Expenses date and composite indexes
CREATE INDEX IF NOT EXISTS idx_expenses_expense_date ON expenses (expense_date DESC);
CREATE INDEX IF NOT EXISTS idx_expenses_shop_expense_date ON expenses (shop_id, expense_date DESC);
CREATE INDEX IF NOT EXISTS idx_expenses_branch_expense_date ON expenses (branch_id, expense_date DESC);

-- Stock movements created_at indexes
CREATE INDEX IF NOT EXISTS idx_stock_movements_created_at ON stock_movements (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_stock_movements_branch_created_at ON stock_movements (branch_id, created_at DESC);

-- Audit logs timestamp index
CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON audit_logs (created_at DESC);
