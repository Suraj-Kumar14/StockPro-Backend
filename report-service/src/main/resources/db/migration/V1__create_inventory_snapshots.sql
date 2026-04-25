create table if not exists inventory_snapshots (
    snapshot_id bigint primary key auto_increment,
    warehouse_id bigint not null,
    product_id bigint not null,
    quantity decimal(19,4) not null,
    stock_value decimal(19,4) not null,
    snapshot_date date not null,
    created_at datetime not null,
    product_sku varchar(100) null,
    product_name varchar(255) null,
    warehouse_name varchar(255) null,
    cost_price decimal(19,4) null,
    source varchar(100) null,
    created_by varchar(255) null
);

create index idx_snapshot_warehouse on inventory_snapshots (warehouse_id);
create index idx_snapshot_product on inventory_snapshots (product_id);
create index idx_snapshot_date on inventory_snapshots (snapshot_date);
create index idx_snapshot_warehouse_date on inventory_snapshots (warehouse_id, snapshot_date);
create index idx_snapshot_product_date on inventory_snapshots (product_id, snapshot_date);
create unique index idx_snapshot_wh_product_date on inventory_snapshots (warehouse_id, product_id, snapshot_date);
