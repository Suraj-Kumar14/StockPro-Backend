package com.stockpro.purchaseservice.schema;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderStatusSchemaInitializer implements ApplicationRunner {

    private static final Set<String> REQUIRED_STATUSES = Set.of(
            "DRAFT",
            "PENDING_PAYMENT",
            "PAYMENT_INITIATED",
            "PENDING_APPROVAL",
            "APPROVED",
            "PAID",
            "PARTIALLY_RECEIVED",
            "RECEIVED",
            "FULLY_RECEIVED",
            "CANCELLED",
            "REJECTED"
    );

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            if (!isMySqlFamilyDatabase()) {
                return;
            }

            Map<String, Object> statusColumn = jdbcTemplate.query(
                    """
                    SELECT DATA_TYPE, COLUMN_TYPE
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE TABLE_SCHEMA = DATABASE()
                      AND TABLE_NAME = 'purchase_orders'
                      AND COLUMN_NAME = 'status'
                    """,
                    rs -> rs.next()
                            ? Map.of(
                                    "dataType", rs.getString("DATA_TYPE"),
                                    "columnType", rs.getString("COLUMN_TYPE"))
                            : null);

            if (statusColumn == null) {
                log.info("purchase_orders.status column not found yet; skipping schema normalization.");
                return;
            }

            String dataType = value(statusColumn.get("dataType"));
            String columnType = value(statusColumn.get("columnType"));
            if (needsNormalization(dataType, columnType)) {
                log.warn("Normalizing purchase_orders.status column from {} ({}) to VARCHAR(50).", dataType, columnType);
                jdbcTemplate.execute("ALTER TABLE purchase_orders MODIFY COLUMN status VARCHAR(50) NOT NULL");
            }

            int renamedRows = jdbcTemplate.update(
                    "UPDATE purchase_orders SET status = 'PENDING_APPROVAL' WHERE status = 'PENDING'");
            if (renamedRows > 0) {
                log.info("Updated {} legacy purchase order rows from PENDING to PENDING_APPROVAL.", renamedRows);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to normalize purchase_orders.status schema", ex);
        }
    }

    private boolean isMySqlFamilyDatabase() {
        Boolean executeResult = jdbcTemplate.execute((org.springframework.jdbc.core.ConnectionCallback<Boolean>) connection -> {
            DatabaseMetaData metaData = connection.getMetaData();
            String productName = metaData.getDatabaseProductName();
            return productName != null
                    && (productName.toLowerCase(Locale.ROOT).contains("mysql")
                    || productName.toLowerCase(Locale.ROOT).contains("mariadb"));
        });
        return Boolean.TRUE.equals(executeResult);
    }

    private boolean needsNormalization(String dataType, String columnType) {
        if ("enum".equalsIgnoreCase(dataType)) {
            return true;
        }
        String normalizedColumnType = columnType == null ? "" : columnType.toUpperCase(Locale.ROOT);
        return REQUIRED_STATUSES.stream().anyMatch(status -> !normalizedColumnType.contains(status));
    }

    private String value(Object raw) {
        return raw == null ? null : String.valueOf(raw);
    }
}
