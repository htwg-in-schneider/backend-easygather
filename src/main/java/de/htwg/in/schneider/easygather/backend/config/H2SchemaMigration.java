package de.htwg.in.schneider.easygather.backend.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class H2SchemaMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(H2SchemaMigration.class);

    @Bean
    @Order(0)
    public CommandLineRunner migrateSchema(DataSource dataSource) {
        return args -> {
            try (var connection = dataSource.getConnection();
                    var statement = connection.createStatement()) {
                migrateMissingImageUrlColumns(connection, statement);
                if (!isH2Database(connection)) {
                    return;
                }
                migrateLegacyProductPriceColumn(connection, statement);
                statement.execute(
                        "ALTER TABLE customer_order ALTER COLUMN status ENUM('BESTAETIGT', 'UNTERWEGS', 'ABGESCHLOSSEN')");
                statement.execute(
                        "UPDATE delivery_order SET status = 'ANGENOMMEN' WHERE status = 'OFFEN' AND driver_id IS NOT NULL");
                statement.execute(
                        "UPDATE delivery_order SET status = 'EINGEGANGEN' WHERE status = 'OFFEN' AND driver_id IS NULL");
                statement.execute(
                        "ALTER TABLE delivery_order ALTER COLUMN status ENUM('EINGEGANGEN', 'ANGENOMMEN', 'UNTERWEGS', 'GELIEFERT')");
                LOGGER.info("Ensured delivery_order.status supports accept workflow");
            } catch (Exception ex) {
                LOGGER.debug("Schema migration skipped: {}", ex.getMessage());
            }
        };
    }

    private void migrateMissingImageUrlColumns(Connection connection, java.sql.Statement statement) {
        String lobType = lobColumnType(connection);
        addColumnIfMissing(connection, statement, "category", "image_url", lobType);
        addColumnIfMissing(connection, statement, "product", "image_url", lobType);
        addColumnIfMissing(connection, statement, "product", "included_items_text", lobType);
    }

    private String lobColumnType(Connection connection) {
        try {
            if (connection.getMetaData().getURL().toLowerCase().contains(":h2:")) {
                return "CLOB";
            }
        } catch (Exception ex) {
            LOGGER.debug("Could not detect database for image_url column type: {}", ex.getMessage());
        }
        return "LONGTEXT";
    }

    private void addColumnIfMissing(Connection connection, java.sql.Statement statement, String tableName,
            String columnName, String columnType) {
        if (hasColumn(connection, tableName, columnName)) {
            return;
        }
        try {
            statement.execute(
                    "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType);
            LOGGER.info("Added missing column {}.{}", tableName, columnName);
        } catch (Exception ex) {
            LOGGER.warn("Could not add column {}.{}: {}", tableName, columnName, ex.getMessage());
        }
    }

    private void migrateLegacyProductPriceColumn(Connection connection, java.sql.Statement statement) {
        if (!hasColumn(connection, "PRODUCT", "PRICE_PER_DAY")) {
            return;
        }
        try {
            if (hasColumn(connection, "PRODUCT", "PRICE")) {
                statement.execute(
                        "UPDATE product SET price = price_per_day WHERE price IS NULL AND price_per_day IS NOT NULL");
            }
            statement.execute("ALTER TABLE product DROP COLUMN price_per_day");
            LOGGER.info("Dropped legacy product.price_per_day column");
        } catch (Exception ex) {
            LOGGER.warn("Could not migrate legacy product.price_per_day column: {}", ex.getMessage());
        }
    }

    private boolean hasColumn(Connection connection, String tableName, String columnName) {
        try {
            DatabaseMetaData meta = connection.getMetaData();
            String catalog = connection.getCatalog();
            String[] tables = { tableName, tableName.toUpperCase(), tableName.toLowerCase() };
            for (String table : tables) {
                try (ResultSet rs = meta.getColumns(catalog, null, table, null)) {
                    while (rs.next()) {
                        String found = rs.getString("COLUMN_NAME");
                        if (found != null && found.equalsIgnoreCase(columnName)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.debug("hasColumn check failed for {}.{}: {}", tableName, columnName, ex.getMessage());
        }
        return false;
    }

    private boolean isH2Database(Connection connection) {
        try {
            return connection.getMetaData().getURL().contains(":h2:");
        } catch (Exception ex) {
            return false;
        }
    }
}
