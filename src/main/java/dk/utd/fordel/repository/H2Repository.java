package dk.utd.fordel.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

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
        String name = null;
        LocalDate studyStart = null;
        int userId = -1;

        var stmt1 = "SELECT name, study_start_date FROM users WHERE email = ?";
        try (var statement = getConnection().prepareStatement(stmt1)) {
            statement.setString(1, user.getEmail().asString());
            var resultSet = statement.executeQuery();
            if (resultSet.next()) {
                userId = resultSet.getInt("id");
                name = resultSet.getString("name");
                studyStart = resultSet.getObject("study_start_date", LocalDate.class);
            } else {
                throw new UserDoesNotExist(user, "User not found");
            }
        } catch (SQLException e) {
            Log.giveUp(logger, e, "Failed to read user: " + user);
        }

        List<Roles> roles = new ArrayList<>();
        var stmt2 = "SELECT r.name FROM roles r JOIN user_roles ur ON r.id = ur.role_id WHERE ur.user_id = ?";
        try (var statement = getConnection().prepareStatement(stmt2)) {
            statement.setInt(1, userId);
            var resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String roleName = resultSet.getString("name");
                roles.add(Roles.valueOf(roleName.toUpperCase().replace(" ", "_")));
            }
        } catch (SQLException e) {
            Log.giveUp(logger, e, "Failed to read user: " + user);
        }

        String studyLine = "";
        var stmt3 = "SELECT sl.name FROM study_lines sl JOIN study_line_students sls ON sl.id = sls.study_line_id WHERE sls.user_id = ?";
        try (var statement = getConnection().prepareStatement(stmt3)) {
            statement.setInt(1, userId);
            var resultSet = statement.executeQuery();
            if (resultSet.next()) {
                studyLine = resultSet.getString("name");
            }
        } catch (SQLException e) {
            throw Log.giveUp(logger, e, "Failed to read study line for user: " + user);
        }

        return new User.Data(name, roles, studyLine, studyStart);
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
    public void grantRole(User user, Roles roleToGrant) {
        var stmt = "INSERT INTO user_roles (user_email, role) VALUES (?, ?)";

        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setString(1, user.getEmail().asString());
            statement.setString(2, roleToGrant.name());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw Log.giveUp(logger, e, "Kunne ikke tildele rolle til brugeren");
        }
    }

    @Override
    public List<UserInfo> getAllUsers() {
        var users = new ArrayList<UserInfo>();
        var stmt = "SELECT id, email, name FROM users";
        try (var statement = getConnection().prepareStatement(stmt)) {
            var resultSet = statement.executeQuery();
            while (resultSet.next()) {
                users.add(new UserInfo(
                        resultSet.getString("id"),
                        resultSet.getString("email"),
                        resultSet.getString("name")
                ));
            }
            return users;
        } catch (SQLException e) {
            throw Log.giveUp(logger, e, "Failed to find all users");
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
