package dk.utd.fordel.repository;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import dk.utd.fordel.utils.Assert;
import dk.utd.fordel.utils.Log;

public class Migrator {

    private static final Logger logger = Log.getLogger(Migrator.class);

    private final Connection connection;
    private final String migrationPath;

    public Migrator(String migrationPath, Connection connection) {
        this.migrationPath = migrationPath;
        this.connection = connection;
    }

    public int getVersion() {
        try (var statement = connection.prepareStatement(
              "SELECT COALESCE (MAX(version), -1) FROM schema_migrations");) {
            var resultSet = statement.executeQuery();
            Assert.isTrue(resultSet.next(), "should always return a value");
            return resultSet.getInt(1);
        } catch (SQLException e) {
            Log.giveUp(logger, e, "Unexpected SQL Exception");
        }
        return -1;
    }

    private void applyVersion(int version) {
        try (var statement = connection.prepareStatement(
              "INSERT INTO schema_migrations (version) VALUES(?)")) {
            statement.setInt(1, version);
            statement.executeUpdate();
        } catch (SQLException e) {
            Log.giveUp(logger, e, "Failed to set version");
        }
    }

    private void ensureVersionTable() {
        try (var statement = connection.createStatement()) {
            logger.debug("Creating version table");
            statement.execute(
                "CREATE TABLE IF NOT EXISTS schema_migrations (\n" +
                  "version INT PRIMARY KEY,\n" +
                  "applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP\n" +
                  ")"
                );
        } catch (SQLException e) {
            Log.giveUp(logger, e, "Failed to create version table");
        }
    }
    
    public void runMigrations() throws SQLException {
        ensureVersionTable();
        int currentVersion = getVersion();
        while (true) { 
          String name = String.format("%s/%04d.sql", this.migrationPath, currentVersion + 1);

          URL url = Migrator.class.getClassLoader().getResource(name);
          if (url == null) {
            logger.debug("Could not find migration {} (done migrating): ", name);
            break;
          } 
            
          logger.info("Applying migration: {}", name);
          try (var stream = url.openStream()) {
            String cmd = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            try (var statement = connection.createStatement()) {
              statement.execute(cmd);
              applyVersion(currentVersion + 1);
              currentVersion = currentVersion + 1;
            }
          } catch (IOException e) { 
            Log.giveUp(logger, e, "Could not open stream");
          }
        }
    }

}
