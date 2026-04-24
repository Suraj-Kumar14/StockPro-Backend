CREATE TABLE IF NOT EXISTS stock_movements (
    movement_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    warehouse_id BIGINT NOT NULL,
    movement_type VARCHAR(50) NOT NULL,
    quantity DECIMAL(19,4) NOT NULL,
    reference_id BIGINT NULL,
    reference_type VARCHAR(100) NULL,
    unit_cost DECIMAL(19,4) NULL,
    performed_by BIGINT NOT NULL,
    notes VARCHAR(1000) NULL,
    movement_date DATETIME NOT NULL,
    balance_after DECIMAL(19,4) NOT NULL,
    created_at DATETIME NOT NULL,
    created_by VARCHAR(255) NULL,
    source_service VARCHAR(100) NULL
);

CREATE INDEX idx_movement_product ON stock_movements (product_id);
CREATE INDEX idx_movement_warehouse ON stock_movements (warehouse_id);
CREATE INDEX idx_movement_type ON stock_movements (movement_type);
CREATE INDEX idx_movement_reference ON stock_movements (reference_id);
CREATE INDEX idx_movement_date ON stock_movements (movement_date);
CREATE INDEX idx_movement_performed_by ON stock_movements (performed_by);
