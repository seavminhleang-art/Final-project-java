package com.proctor.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConnection {
    private static volatile HikariDataSource dataSource;

    public static synchronized void init() {
        if (dataSource != null && !dataSource.isClosed()) {
            return;
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(Config.get("db.url", "jdbc:postgresql://localhost:5432/proctor_db"));
        hikariConfig.setUsername(Config.get("db.username", "postgres"));
        hikariConfig.setPassword(Config.get("db.password", ""));
        hikariConfig.setMaximumPoolSize(Config.getInt("db.pool.max", 20));
        hikariConfig.setMinimumIdle(Config.getInt("db.pool.min_idle", 3));
        hikariConfig.setConnectionTimeout(Config.getInt("db.pool.timeout_ms", 15000));
        hikariConfig.setLeakDetectionThreshold(Config.getInt("db.pool.leak_threshold_ms", 15000));

        dataSource = new HikariDataSource(hikariConfig);
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            init();
        }
        return dataSource.getConnection();
    }

    public static synchronized void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            dataSource = null;
        }
    }
}