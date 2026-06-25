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
    public CommandLineRunner migrateH2OrderStatusEnum(DataSource dataSource) {
        return args -> {
            if (!isH2Database(dataSource)) {
                return;
            }
            try (var connection = dataSource.getConnection();
                    var statement = connection.createStatement()) {
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
                LOGGER.debug("Order status enum migration skipped: {}", ex.getMessage());
            }
        };
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
            try (ResultSet rs = meta.getColumns(null, null, tableName, columnName)) {
                if (rs.next()) {
                    return true;
                }
            }
            try (ResultSet rs = meta.getColumns(null, null, tableName.toLowerCase(), columnName)) {
                return rs.next();
            }
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isH2Database(DataSource dataSource) {
        try (var connection = dataSource.getConnection()) {
            return connection.getMetaData().getURL().contains(":h2:");
        } catch (Exception ex) {
            return false;
        }
    }
}
