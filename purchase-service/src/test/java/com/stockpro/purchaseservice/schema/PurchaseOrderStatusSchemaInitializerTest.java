package com.stockpro.purchaseservice.schema;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseOrderStatusSchemaInitializerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ApplicationArguments applicationArguments;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData databaseMetaData;

    private PurchaseOrderStatusSchemaInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new PurchaseOrderStatusSchemaInitializer(jdbcTemplate);
    }

    @Test
    void run_shouldSkipWhenDatabaseIsNotMySqlFamily() throws Exception {
        mockDatabaseProduct("PostgreSQL");

        initializer.run(applicationArguments);

        verify(jdbcTemplate, never()).query(anyString(), any(org.springframework.jdbc.core.ResultSetExtractor.class));
        verify(jdbcTemplate, never()).update(anyString());
    }

    @Test
    void run_shouldSkipWhenStatusColumnIsMissing() throws Exception {
        mockDatabaseProduct("MySQL");
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.ResultSetExtractor.class))).thenReturn(null);

        initializer.run(applicationArguments);

        verify(jdbcTemplate, never()).execute("ALTER TABLE purchase_orders MODIFY COLUMN status VARCHAR(50) NOT NULL");
        verify(jdbcTemplate, never()).update(anyString());
    }

    @Test
    void run_shouldNormalizeEnumColumnAndRenameLegacyPendingStatus() throws Exception {
        mockDatabaseProduct("MariaDB");
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.ResultSetExtractor.class)))
                .thenReturn(Map.of("dataType", "enum", "columnType", "enum('DRAFT','PENDING')"));
        when(jdbcTemplate.update(anyString())).thenReturn(2);

        initializer.run(applicationArguments);

        verify(jdbcTemplate).execute("ALTER TABLE purchase_orders MODIFY COLUMN status VARCHAR(50) NOT NULL");
        verify(jdbcTemplate).update("UPDATE purchase_orders SET status = 'PENDING_APPROVAL' WHERE status = 'PENDING'");
    }

    @Test
    void run_shouldRenameLegacyPendingWithoutSchemaAlterWhenColumnAlreadyNormalized() throws Exception {
        mockDatabaseProduct("MySQL");
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.ResultSetExtractor.class)))
                .thenReturn(Map.of(
                        "dataType", "varchar",
                        "columnType", "varchar(50) DRAFT PENDING_PAYMENT PAYMENT_INITIATED PENDING_APPROVAL APPROVED PAID PARTIALLY_RECEIVED RECEIVED FULLY_RECEIVED CANCELLED REJECTED"));
        when(jdbcTemplate.update(anyString())).thenReturn(0);

        initializer.run(applicationArguments);

        verify(jdbcTemplate, never()).execute("ALTER TABLE purchase_orders MODIFY COLUMN status VARCHAR(50) NOT NULL");
        verify(jdbcTemplate).update("UPDATE purchase_orders SET status = 'PENDING_APPROVAL' WHERE status = 'PENDING'");
    }

    @Test
    void run_shouldWrapUnexpectedFailure() throws Exception {
        mockDatabaseProduct("MySQL");
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.ResultSetExtractor.class)))
                .thenThrow(new RuntimeException("boom"));

        assertThrows(IllegalStateException.class, () -> initializer.run(applicationArguments));
    }

    private void mockDatabaseProduct(String productName) throws Exception {
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn(productName);
        when(jdbcTemplate.execute(any(ConnectionCallback.class))).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ConnectionCallback<Boolean> callback = invocation.getArgument(0);
            return callback.doInConnection(connection);
        });
    }
}
