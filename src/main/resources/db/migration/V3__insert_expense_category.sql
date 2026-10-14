INSERT INTO expense_category (name)
VALUES ('product'), ('service')
ON CONFLICT (name) DO NOTHING;
