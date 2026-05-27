CREATE TABLE
    IF NOT EXISTS
    products (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64),
    description VARCHAR(512),
    shopId VARCHAR(64)
    characteristics jsonb,
    stock_price DECIMAL(10, 2),
    category_id INT REFERENCES categories
)