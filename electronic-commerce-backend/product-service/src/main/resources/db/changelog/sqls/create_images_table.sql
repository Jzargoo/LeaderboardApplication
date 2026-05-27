CREATE TABLE
    IF NOT EXISTS
        images(
            id bigserial primary key ,
            image varchar(128) NOT NULL ,
            product_id bigint REFERENCES products(id)
        )