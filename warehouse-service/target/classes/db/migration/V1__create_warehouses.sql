CREATE TABLE warehouses (
    warehouse_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255) NOT NULL,
    address VARCHAR(500) NOT NULL,
    manager_id BIGINT NULL,
    capacity INT NOT NULL,
    used_capacity INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    phone VARCHAR(50) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);

CREATE INDEX idx_warehouse_location ON warehouses (location);
CREATE INDEX idx_warehouse_manager ON warehouses (manager_id);
CREATE INDEX idx_warehouse_active ON warehouses (is_active);
