package org.ayosynk.hubparkour.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.ayosynk.hubparkour.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class DatabaseManager {
    private final DatabaseConfig config;
    private final Logger logger;
    private HikariDataSource dataSource;

    public DatabaseManager(DatabaseConfig config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public void connect() {
        if (!config.enabled()) {
            logger.info("Database disabled");
            return;
        }
        if (config.createDatabase()) {
            ensureDatabase();
        }
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(buildJdbcUrl());
        hikari.setUsername(config.user());
        hikari.setPassword(config.password());
        hikari.setMaximumPoolSize(config.poolSize());
        hikari.setPoolName("HubParkourPool");
        hikari.addDataSourceProperty("cachePrepStmts", "true");
        hikari.addDataSourceProperty("prepStmtCacheSize", "250");
        hikari.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikari.addDataSourceProperty("useServerPrepStmts", "true");
        dataSource = new HikariDataSource(hikari);
        ensureSchema();
        logger.info("Database connected");
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database not initialized");
        }
        return dataSource.getConnection();
    }

    public void shutdown() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    private String buildJdbcUrl() {
        String ssl = config.useSsl() ? "true" : "false";
        return "jdbc:mysql://" + config.host() + ":" + config.port() + "/" + config.name()
                + "?useSSL=" + ssl + "&useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    }

    private String buildAdminJdbcUrl() {
        String ssl = config.useSsl() ? "true" : "false";
        return "jdbc:mysql://" + config.host() + ":" + config.port()
                + "/?useSSL=" + ssl + "&useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    }

    private void ensureDatabase() {
        String schema = sanitizeSchema(config.name());
        String sql = "CREATE DATABASE IF NOT EXISTS `" + schema + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";
        try (Connection connection = DriverManager.getConnection(buildAdminJdbcUrl(), config.user(), config.password());
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
            logger.info("Database ensured: " + schema);
        } catch (SQLException ex) {
            logger.severe("Failed to ensure database " + schema + ": " + ex.getMessage());
        }
    }

    private void ensureSchema() {
        String parkours = "CREATE TABLE IF NOT EXISTS parkours ("
                + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + "name VARCHAR(64) NOT NULL UNIQUE,"
                + "start_location VARCHAR(128),"
                + "start_material VARCHAR(64),"
                + "end_location VARCHAR(128),"
                + "end_material VARCHAR(64),"
                + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
                + ")";
        String checkpoints = "CREATE TABLE IF NOT EXISTS checkpoints ("
                + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + "parkour_id INT NOT NULL,"
                + "cp_index INT NOT NULL,"
                + "location VARCHAR(128) NOT NULL,"
                + "material VARCHAR(64) NOT NULL,"
                + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "UNIQUE KEY uq_checkpoint (parkour_id, cp_index),"
                + "INDEX idx_checkpoint_location (location)"
                + ")";
        String times = "CREATE TABLE IF NOT EXISTS parkour_times ("
                + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + "player_uuid VARCHAR(36) NOT NULL,"
                + "parkour_id INT NOT NULL,"
                + "comp_time DOUBLE NOT NULL,"
                + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "INDEX idx_times_player (player_uuid),"
                + "INDEX idx_times_parkour (parkour_id)"
                + ")";
        String leaderboards = "CREATE TABLE IF NOT EXISTS leaderboards ("
                + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + "parkour_id INT NOT NULL,"
                + "location VARCHAR(128) NOT NULL,"
                + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
                + ")";
        String leaderboardLines = "CREATE TABLE IF NOT EXISTS leaderboard_lines ("
                + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                + "leaderboard_id INT NOT NULL,"
                + "line_position INT NOT NULL,"
                + "hologram_name VARCHAR(128) NOT NULL,"
                + "location VARCHAR(128) NOT NULL,"
                + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                + "INDEX idx_lb_line (leaderboard_id, line_position)"
                + ")";
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(parkours);
            statement.execute(checkpoints);
            statement.execute(times);
            statement.execute(leaderboards);
            statement.execute(leaderboardLines);
        } catch (SQLException ex) {
            logger.severe("Failed to ensure schema: " + ex.getMessage());
        }
    }

    private String sanitizeSchema(String schema) {
        return schema == null ? "minecraft_hub" : schema.replace("`", "");
    }
}
