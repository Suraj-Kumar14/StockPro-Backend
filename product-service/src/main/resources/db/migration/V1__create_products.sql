CREATE TABLE products (
    product_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sku VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NULL,
    category VARCHAR(150) NOT NULL,
    brand VARCHAR(150) NOT NULL,
    unit_of_measure VARCHAR(100) NOT NULL,
    cost_price DECIMAL(19,4) NOT NULL,
    selling_price DECIMAL(19,4) NOT NULL,
    reorder_level DECIMAL(19,4) NOT NULL,
    max_stock_level DECIMAL(19,4) NOT NULL,
    lead_time_days INT NOT NULL,
    image_url VARCHAR(1000) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    barcode VARCHAR(150) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_product_sku UNIQUE (sku),
    CONSTRAINT uk_product_barcode UNIQUE (barcode)
);

CREATE INDEX idx_product_sku ON products (sku);
CREATE INDEX idx_product_barcode ON products (barcode);
CREATE INDEX idx_product_category ON products (category);
CREATE INDEX idx_product_brand ON products (brand);
CREATE INDEX idx_product_name ON products (name);
CREATE INDEX idx_product_active ON products (is_active);
