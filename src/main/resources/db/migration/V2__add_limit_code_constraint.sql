ALTER TABLE expense_limit
ADD CONSTRAINT chk_expense_limit_currency
CHECK ( currency_code = 'USD' );