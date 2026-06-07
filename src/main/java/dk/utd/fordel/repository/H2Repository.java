package dk.utd.fordel.repository;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.web.context.annotation.RequestScope;

import dk.utd.fordel.domain.UserDoesNotExist;
import dk.utd.fordel.domain.Email;
import dk.utd.fordel.domain.EmailAlreadyInUse;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.domain.User.Data;
import dk.utd.fordel.services.Authenticator.PasswordEncoding;
import dk.utd.fordel.services.Authenticator.PasswordRepository;
import dk.utd.fordel.services.UserManager.UserRepository;
import dk.utd.fordel.utils.Assert;
import dk.utd.fordel.utils.Log;
import dk.utd.fordel.utils.UnitOfWork;

@Repository
@RequestScope
public class H2Repository implements PasswordRepository, UnitOfWork, UserRepository {

    private static final Logger logger = LoggerFactory.getLogger(H2Repository.class);
    private final DataSource dataSource;

    // An active connection to the database, might be null if not opened.
    private java.sql.Connection connection;

    /** Get the connection, throw runtime error if not open. */
    private Connection getConnection() {
        if (connection == null) {
            throw new RuntimeException("Connection not opened");
        }
        return connection;
    }

    public H2Repository(DataSource dataSource) {
        Assert.isNotNull(dataSource, "DataSource cannot be null");

        this.dataSource = dataSource;
        logger.info("H2Repository created");

        runMigrations();
    }

    private void runMigrations() {
        try (var con = dataSource.getConnection()) {
            new Migrator("migrations", con).runMigrations();
        } catch (SQLException e) {
            Log.giveUp(logger, e, "Failed to open connection");
        }
    }

    public PasswordEncoding getPassword(User user) throws UserDoesNotExist {
        Assert.isNotNull(user, "User cannot be null");

        var stmt = "SELECT password_hash FROM users WHERE email = ?";
        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setString(1, user.getEmail().asString());
            var resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return new PasswordEncoding(resultSet.getString("password_hash"));
            }
        } catch (SQLException e) {
            Log.giveUp(logger, e, "Failed to get password for user: " + user);
        }
        throw new UserDoesNotExist(user, "User not found");
    }

    @Override
    public void setPassword(User user, PasswordEncoding encoding) throws UserDoesNotExist {
        var stmt = "UPDATE users SET password_hash = ? WHERE email = ?";
        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setString(1, encoding.hash());
            statement.setString(2, user.getEmail().asString());
            var affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new UserDoesNotExist(user, "User not found, while setting password");
            }
        } catch (SQLException e) {
            Log.giveUp(logger, e, "Failed to set password for user: " + user);
        }
    }

    @Override
    public void create(User user, User.Data data) throws EmailAlreadyInUse {
        var stmt = "INSERT INTO users (email, name) VALUES (?, ?)";
        try (var statement = getConnection().prepareStatement(stmt)) {
            assert Email.MAX_EMAIL_SIZE == 350;
            statement.setString(1, user.getEmail().asString());
            statement.setString(2, data.name());
            statement.executeUpdate();
        } catch (SQLException e) {
            if (23505 == e.getErrorCode()) { // Unique constraint violation
                throw new EmailAlreadyInUse(user.getEmail());
            }
            throw Log.giveUp(logger, e, "Failed to create user: " + user);
        }
    }

    @Override
    public User.Data read(User user) throws UserDoesNotExist {
        var stmt = "SELECT name FROM users WHERE email = ?";
        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setString(1, user.getEmail().asString());
            var resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return new User.Data(resultSet.getString("name"));
            }
        } catch (SQLException e) {
            Log.giveUp(logger, e, "Failed to read user: " + user);
        }
        throw new UserDoesNotExist(user, "User not found");
    }

    @Override
    public void delete(User user) throws UserDoesNotExist {
        var stmt = "DELETE FROM users WHERE email = ?";
        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setString(1, user.getEmail().asString());
            if (statement.executeUpdate() != 1) {
                throw new UserDoesNotExist(user, "While deleting user");
            }
        } catch (SQLException e) {
            throw Log.giveUp(logger, e, "Failed to delete user: " + user);
        }
    }

    @Override
    public void update(User user, Data data) throws UserDoesNotExist {
        var stmt = "UPDATE users SET name = ? WHERE email = ?";
        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setString(1, data.name());
            statement.setString(2, user.getEmail().asString());
            if (statement.executeUpdate() != 1) {
                throw new UserDoesNotExist(user, "While updating user");
            }
        } catch (SQLException e) {
            throw Log.giveUp(logger, e, "Failed to update user: " + user);
        }
    }

    @Override
    public UnitOfWork begin() throws UnitOfWorkException {
        try {
            logger.info("Starting transaction for {}", this);
            connection = dataSource.getConnection();
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new UnitOfWorkException("Failed to open transaction", e);
        }
        return this;
    }

    @Override
    public void commit() throws UnitOfWorkException {
        logger.info("Commiting transaction");
        try {
            getConnection().commit();
        } catch (SQLException e) {
            throw new UnitOfWorkException("Failed to commit transaction", e);
        }
    }

    @Override
    public void rollback() throws UnitOfWorkException {
        logger.info("Rolling back transaction");
        try {
            getConnection().rollback();
        } catch (SQLException e) {
            throw new UnitOfWorkException("Failed to rollback transaction", e);
        }
    }

    @Override
    public void close() throws UnitOfWorkException {
        logger.info("Closing transaction for {}", this);
        try {
            getConnection().close();
        } catch (SQLException e) {
            throw new UnitOfWorkException("Failed to close connection", e);
        }
    }

}
