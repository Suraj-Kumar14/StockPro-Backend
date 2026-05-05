ALTER TABLE purchase_orders
MODIFY COLUMN status VARCHAR(50) NOT NULL;

UPDATE purchase_orders
SET status = 'PENDING_APPROVAL'
WHERE status = 'PENDING';
