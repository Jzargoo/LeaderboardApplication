CREATE TABLE
    IF NOT EXISTS
    products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64),
    description VARCHAR(512),
    -- There will be also json characteristics
    stock_price DECIMAL(10, 2),
    rate_id bigint,
    category_id INT REFERENCES categories
)