CREATE TABLE stock_levels (
    stock_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    warehouse_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    reserved_quantity DECIMAL(19,4) NOT NULL DEFAULT 0.0000,
    location VARCHAR(255) NULL,
    last_updated DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_stock_levels_warehouse FOREIGN KEY (warehouse_id)
        REFERENCES warehouses (warehouse_id)
);

CREATE UNIQUE INDEX idx_stock_warehouse_product ON stock_levels (warehouse_id, product_id);
CREATE INDEX idx_stock_product ON stock_levels (product_id);
CREATE INDEX idx_stock_last_updated ON stock_levels (last_updated);
