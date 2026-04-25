CREATE TABLE alerts (
    alert_id BIGINT NOT NULL AUTO_INCREMENT,
    recipient_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    related_product_id BIGINT NULL,
    related_warehouse_id BIGINT NULL,
    related_purchase_order_id BIGINT NULL,
    channel VARCHAR(50) NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    is_acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    read_at DATETIME NULL,
    acknowledged_at DATETIME NULL,
    metadata TEXT NULL,
    PRIMARY KEY (alert_id)
);

CREATE INDEX idx_alert_recipient ON alerts (recipient_id);
CREATE INDEX idx_alert_recipient_read ON alerts (recipient_id, is_read);
CREATE INDEX idx_alert_type ON alerts (type);
CREATE INDEX idx_alert_severity ON alerts (severity);
CREATE INDEX idx_alert_product ON alerts (related_product_id);
CREATE INDEX idx_alert_warehouse ON alerts (related_warehouse_id);
CREATE INDEX idx_alert_po ON alerts (related_purchase_order_id);
CREATE INDEX idx_alert_acknowledged ON alerts (is_acknowledged);
CREATE INDEX idx_alert_created_at ON alerts (created_at);
