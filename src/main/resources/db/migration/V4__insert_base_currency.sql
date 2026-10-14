INSERT INTO currency (code, numeric_code)
VALUES
    ('USD', '840'),
    ('KZT', '398'),
    ('RUB', '643')
    ON CONFLICT (code) DO NOTHING;