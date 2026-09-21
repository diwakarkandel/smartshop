CREATE TABLE expense_categories (
    id uuid PRIMARY KEY,
    shop_id uuid REFERENCES shops(id),
    name varchar(100) NOT NULL,
    description varchar(255),
    created_at timestamp NOT NULL DEFAULT now(),
    updated_at timestamp NOT NULL DEFAULT now(),
    CONSTRAINT uq_expense_categories_shop_name UNIQUE (shop_id, name)
);

INSERT INTO expense_categories (id, name, description) VALUES
    ('b1d2e3f4-0001-4001-8001-000000000001', 'Salary & Wages', 'Employee salaries and wages'),
    ('b1d2e3f4-0002-4002-8002-000000000002', 'Rent', 'Shop and branch rent'),
    ('b1d2e3f4-0003-4003-8003-000000000003', 'Utilities', 'Electricity, water, internet, phone'),
    ('b1d2e3f4-0004-4004-8004-000000000004', 'Transport & Delivery', 'Logistics and delivery charges'),
    ('b1d2e3f4-0005-4005-8005-000000000005', 'Packaging', 'Packing materials and supplies'),
    ('b1d2e3f4-0006-4006-8006-000000000006', 'Office Supplies', 'Stationery and office consumables'),
    ('b1d2e3f4-0007-4007-8007-000000000007', 'Marketing & Advertising', 'Promotions and advertisements'),
    ('b1d2e3f4-0008-4008-8008-000000000008', 'Repairs & Maintenance', 'Equipment and premises upkeep'),
    ('b1d2e3f4-0009-4009-8009-000000000009', 'Insurance', 'Business and asset insurance'),
    ('b1d2e3f4-0010-4010-8010-000000000010', 'Taxes & Licenses', 'Government fees and licenses'),
    ('b1d2e3f4-0011-4011-8011-000000000011', 'Bank Charges', 'Bank fees and charges'),
    ('b1d2e3f4-0012-4012-8012-000000000012', 'Miscellaneous', 'Other expenses');

ALTER TABLE expenses ADD COLUMN category_id uuid REFERENCES expense_categories(id);

-- Optional: if you don't need to keep the old text data, you can drop the old category column:
-- ALTER TABLE expenses DROP COLUMN category;