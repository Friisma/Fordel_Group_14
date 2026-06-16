package dk.utd.fordel.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import dk.utd.fordel.domain.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.web.context.annotation.RequestScope;

import dk.utd.fordel.domain.User.Data;
import dk.utd.fordel.services.Authenticator.PasswordEncoding;
import dk.utd.fordel.services.Authenticator.PasswordRepository;
import dk.utd.fordel.services.UserManager.UserRepository;
import dk.utd.fordel.services.StudyLineManager.StudyLineRepository;
import dk.utd.fordel.services.ProjectManager.ProjectRepository;
import dk.utd.fordel.services.InvitationManager.InvitationRepository;

import dk.utd.fordel.utils.Assert;
import dk.utd.fordel.utils.Log;
import dk.utd.fordel.utils.UnitOfWork;

@Repository
@RequestScope
public class H2Repository implements PasswordRepository, UnitOfWork, UserRepository, ProjectRepository,
        InvitationRepository, StudyLineRepository {

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

        var stmt1 = "SELECT id, name, study_start_date FROM users WHERE email = ?";
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

        List<Roles> roles = getUserRoles(user);

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

    // TODO temporary fix for getting id from user table. this should maybe be
    // changed when the domain is more complete

    public int getUserIdByEmail(Email email) {
        String stmt = "SELECT id FROM users WHERE email = ?";

        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setString(1, email.asString());

            try (var rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
            throw new UserDoesNotExist(
                    User.of(email.toString()), "User does not exist");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (UserDoesNotExist e) {
            throw Log.giveUp(logger, e, "User does not exist");
        } catch (Email.InvalidEmail e) {
            throw Log.giveUp(logger, e, "Invalid email");
        }
    }

    // TODO Write more proper repository methods to update the database.

    @Override
    public void update(Project project) {
    }

    @Override
    public void delete(Project project) {
    }

    @Override
    public void create(Project project, User currUser, int userId) throws ProjectTitleAlreadyInUse {
        int studyLineId = getStudyLineIdByUser(currUser);
        createProjectWithStudyLine(project, userId, studyLineId);
    }

    @Override
    public void create(Project project, User currUser, String studyLineName, int userId)
            throws ProjectTitleAlreadyInUse {
        int studyLineId = getStudyLineIdByName(studyLineName);
        createProjectWithStudyLine(project, userId, studyLineId);
    }

    private void createProjectWithStudyLine(Project project, int userId, int studyLineId)
            throws ProjectTitleAlreadyInUse {
        var stmt = "INSERT INTO projects (title, description, study_line_id) VALUES (?, ?, ?)";

        try (var statement = getConnection().prepareStatement(stmt,
                Statement.RETURN_GENERATED_KEYS)) { // Henter den key der er genereret
            statement.setString(1, project.getProjectName());
            statement.setString(2, project.getDescription());
            statement.setInt(3, studyLineId);

            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {

                if (!keys.next()) {
                    throw new RuntimeException("No project ID generated");
                }

                project.setProjectID(keys.getInt(1)); // sætter den rigtige key på domain.
            }
        } catch (SQLException e) {
            logger.error("SQL error code: {}, SQL state: {}, message: {}",
                    e.getErrorCode(), e.getSQLState(), e.getMessage());
            if (23505 == e.getErrorCode()) { // Unique constraint violation
                throw new ProjectTitleAlreadyInUse("Project name already being used: " + project.getProjectName());
            }
            throw Log.giveUp(logger, e, "Failed to create project: " + project);
        }
        var stmt2 = "INSERT INTO project_students (user_id, project_id) VALUES (?,?)";
        try (var statement2 = getConnection().prepareStatement(stmt2)) {
            statement2.setInt(1, userId);
            statement2.setInt(2, project.getProjectID());
            statement2.executeUpdate();
        } catch (SQLException e) {
            logger.error("SQL error code: {}, SQL state: {}, message: {}",
                    e.getErrorCode(), e.getSQLState(), e.getMessage());
            throw Log.giveUp(logger, e, "Failed to create project: " + project);
        }
    }

    private int getStudyLineIdByUser(User user) {
        var stmt = """
                SELECT sl.id
                FROM study_lines sl
                JOIN study_line_students sls ON sl.id = sls.study_line_id
                WHERE sls.user_id = (SELECT id FROM users WHERE email = ?)
                """;

        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setString(1, user.getEmail().asString());
            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
            }

            throw new RuntimeException("User is not admitted to a study line");
        } catch (SQLException e) {
            throw Log.giveUp(logger, e, "Failed to look up study line for user: " + user);
        }
    }

    private int getStudyLineIdByName(String studyLineName) {
        if (studyLineName == null || studyLineName.isBlank()) {
            throw new RuntimeException("User is not admitted to a study line");
        }

        var stmt = "SELECT id FROM study_lines WHERE name = ?";
        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setString(1, studyLineName);
            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
            }

            throw new RuntimeException("Study line does not exist: " + studyLineName);
        } catch (SQLException e) {
            throw Log.giveUp(logger, e, "Failed to look up study line: " + studyLineName);
        }
    }

    @Override
    public List<Project> getAllProjectsRelatedToUser(User user) throws Project.InvalidProject {
        List<Project> projects = new ArrayList<>();
        int userId = getUserIdByEmail(user.getEmail());
        var stmt = """
                SELECT p.title, p.description
                FROM projects p
                JOIN project_students ps ON p.id = ps.project_id
                WHERE ps.user_id = ?
                """;
        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setInt(1, userId);
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    projects.add(new Project(
                            resultSet.getString("title"),
                            resultSet.getString("description")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return projects;
    }

    @Override
    public boolean canViewProject(User user, int projectId) {
        int userId = getUserIdByEmail(user.getEmail());
        var stmt = """
                SELECT 1
                FROM project_students
                WHERE user_id = ? AND project_id = ?
                UNION
                SELECT 1
                FROM project_advisors
                WHERE user_id = ? AND project_id = ?
                """;

        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setInt(1, userId);
            statement.setInt(2, projectId);
            statement.setInt(3, userId);
            statement.setInt(4, projectId);

            try (var resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw Log.giveUp(logger, e,
                    "Failed to check project membership for user: " + user + ", project: " + projectId);
        }
    }

    @Override
    public Project read(Project project) {
        try {
            return new Project("t", "");
        } catch (Project.InvalidProject e) {

        }
        return null;
    }

    /**
     * Grants a chosen user a specified role.
     * This method insert a user_id and role_id into database so the user becomes
     * a member of the specified role.
     *
     * @return a {@link List} of {@link UserInfo} objects representing all users.
     */
    @Override
    public void grantRole(User user, Roles roleToGrant) {
        var stmt = """
                INSERT INTO user_roles (user_id, role_id)
                VALUES (
                    (SELECT id FROM users WHERE email = ?),
                    (SELECT id FROM roles WHERE name = ?)
                )
                """;

        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setString(1, user.getEmail().asString());
            statement.setString(2, roleToGrant.name().toLowerCase().replace("_", " "));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw Log.giveUp(logger, e, "Could not add role to user");
        }
    }

    /**
     * Remove a role from a user.
     * This method delete a user_id and role_id from the database
     * so the user long is a member of the specified role.
     *
     */
    @Override
    public void removeRole(User user, Roles roleToRemove) {
        var stmt = """
                DELETE FROM user_roles
                       WHERE USER_ID = (SELECT id FROM users WHERE email = ?)
                         and ROLE_ID = (SELECT id FROM roles WHERE name = ?)
                """;

        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setString(1, user.getEmail().asString());
            statement.setString(2, roleToRemove.name().toLowerCase().replace("_", " "));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw Log.giveUp(logger, e, "Could not add role to user");
        }
    }

    /**
     * Gets a {@link List} of {@link Roles} for the specified user.
     * This method looks up all assigned roles for a specified user in the database.
     *
     *
     * @return a {@link List} of {@link Roles} objects representing a user roles.
     */
    @Override
    public List<Roles> getUserRoles(User user) {
        List<Roles> roles = new ArrayList<>();
        var stmt2 = """
                    SELECT r.name FROM roles r
                        JOIN user_roles ur ON r.id = ur.role_id
                                  WHERE ur.user_id = (SELECT id FROM users WHERE email = ?)
                """;
        try (var statement = getConnection().prepareStatement(stmt2)) {
            statement.setString(1, user.getEmail().asString());
            var resultSet = statement.executeQuery();
            while (resultSet.next()) {
                String roleName = resultSet.getString("name");
                roles.add(Roles.valueOf(roleName.toUpperCase().replace(" ", "_")));
            }
        } catch (SQLException e) {
            Log.giveUp(logger, e, "Failed to read user: " + user);
        }
        return roles;
    }

    /**
     * Retrieves a list of all users currently in the system.
     * This method ask the database for all relevant user information which includes
     * ID, email, name, assigned roles and study line.
     *
     * @return a {@link List} of {@link UserInfo} objects representing all users.
     */
    @Override
    public List<UserInfo> getAllUsers() {
        var users = new ArrayList<UserInfo>();
        var stmt = "SELECT id, email, name FROM users";
        try (var statement = getConnection().prepareStatement(stmt)) {
            var resultSet = statement.executeQuery();
            while (resultSet.next()) {
                var currUser = read(User.of(resultSet.getString("email")));

                users.add(new UserInfo(
                        resultSet.getString("id"),
                        resultSet.getString("email"),
                        resultSet.getString("name"),
                        currUser.roles(),
                        currUser.studyLine()));

            }
            return users;
        } catch (SQLException e) {
            throw Log.giveUp(logger, e, "Failed to find all users");
        } catch (UserDoesNotExist e) {
            throw new RuntimeException(e);
        } catch (Email.InvalidEmail e) {
            throw new RuntimeException(e);
        }
    }

    // INVITATION
    @Override
    public void create(User invited, Project project) {
        int invitedUserId = getUserIdByEmail(invited.getEmail());
        int projectId = project.getProjectID();
        var stmt = "INSERT INTO advisor_invitations (advisor_id, project_id) VALUES (?, ?)";
        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setInt(1, invitedUserId);
            statement.setInt(2, projectId);
            statement.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == 23505) {
                throw new AdvisorAlreadyInvited("Advisor already invited");
            }
            throw Log.giveUp(logger, e, "Failed to create invitation");
        }
    }

    @Override
    public List<AdvisorInvitation> getAllInvitesReceived(User invited) {
        int userId = getUserIdByEmail(invited.getEmail());
        List<AdvisorInvitation> invitations = new ArrayList<AdvisorInvitation>();
        var stmt = """
                SELECT advisor_id, project_id, status
                FROM advisor_invitations
                WHERE advisor_id = (?)
                """;
        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setInt(1, userId);
            var resultSet = statement.executeQuery();
            while (resultSet.next()) {
                var invitation = new AdvisorInvitation(
                        resultSet.getInt("advisor_id"),
                        resultSet.getInt("project_id"));
                String status = resultSet.getString("status");
                if (status.equals("accepted"))
                    invitation.accept();
                if (status.equals("declined"))
                    invitation.decline();
                invitations.add(invitation);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return invitations;
    }

    @Override
    public void update(AdvisorInvitation advisorInvitation) throws AdvisorAlreadyInvited {
        String status = advisorInvitation.getStatusAsString();
        int advisor_id = advisorInvitation.getAdvisorId();
        int project_id = advisorInvitation.getProjectId();
        var stmt = """
                UPDATE advisor_invitations SET status = (?)
                WHERE advisor_id = (?)
                AND project_id = (?)
                """;
        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setString(1, status);
            statement.setInt(2, advisor_id);
            statement.setInt(3, project_id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw Log.giveUp(logger, e, "Failed to update advisor invitations table");
        }
    }

    @Override
    public void delete() {

    }

    @Override
    public void create(StudyLine studyLine) throws StudyLineAlreadyExists {
        var stmt = "INSERT INTO study_lines (name) VALUES (?)";
        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setString(1, studyLine.getLineName());
            statement.executeUpdate();
        } catch (SQLException e) {
            if (23505 == e.getErrorCode()) { // Unique constraint violation
                throw new StudyLineAlreadyExists(studyLine);
            }
            throw Log.giveUp(logger, e, "Failed to create study line: " + studyLine);
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
