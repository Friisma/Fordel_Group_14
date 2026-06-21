package dk.utd.fordel.repository;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    public LocalDateTime getPasswordChangedAt(User user) throws UserDoesNotExist {
        Assert.isNotNull(user, "User cannot be null");

        var stmt = "SELECT password_changed_at FROM users WHERE email = ?";
        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setString(1, user.getEmail().asString());
            var resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getObject("password_changed_at", LocalDateTime.class);
            }
        } catch (SQLException e) {
            Log.giveUp(logger, e, "Failed to get password change date for user: " + user);
        }

        throw new UserDoesNotExist(user, "User not found");
    }

    @Override
    public void setPasswordChangedAt(User user, LocalDateTime passwordChangedAt) throws UserDoesNotExist {
        Assert.isNotNull(user, "User cannot be null");
        Assert.isNotNull(passwordChangedAt, "passwordChangedAt cannot be null");

        var stmt = "UPDATE users SET password_changed_at = ? WHERE email = ? AND password_changed_at IS NULL";
        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setObject(1, passwordChangedAt);
            statement.setString(2, user.getEmail().asString());
            var affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new UserDoesNotExist(user, "User not found, while setting password change date");
            }
        } catch (SQLException e) {
            Log.giveUp(logger, e, "Failed to set password change date for user: " + user);
        }
    }

    @Override
    public void setPassword(User user, PasswordEncoding encoding) throws UserDoesNotExist {
        var stmt = "UPDATE users SET password_hash = ?, password_changed_at = CURRENT_TIMESTAMP WHERE email = ?";
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
        var stmt3 = """
                  SELECT sl.name
                  FROM study_lines sl
                  JOIN study_line_memberships slm ON sl.id = slm.study_line_id
                  JOIN roles r ON slm.role_id = r.id
                  WHERE slm.user_id = ?
                  AND r.name = 'student'
                  """;
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

    //TODO temporary fix for getting id from user table. this should maybe be changed when the domain is more complete

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
        } catch (UserDoesNotExist e){
            throw Log.giveUp(logger, e, "User does not exist");
        } catch (Email.InvalidEmail e){
            throw Log.giveUp(logger, e, "Invalid email");
        }
    }

    public User getUserByUserId(int id) {
        String stmt = "SELECT email FROM users WHERE id = ?";

        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setInt(1, id);

            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return User.of(resultSet.getString("email"));
                }
            }
            throw new UserDoesNotExist(User.of("email"), "User does not exists");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (UserDoesNotExist e){
            throw Log.giveUp(logger, e, "User does not exist");
        } catch (Email.InvalidEmail e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateStudyStart(User user, LocalDate studyStart) throws UserDoesNotExist {
        Assert.isNotNull(user, "User cannot be null");
        Assert.isNotNull(studyStart, "studyStart cannot be null");

        var stmt = "UPDATE users SET study_start_date = ? WHERE email = ?";
        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setObject(1, studyStart);
            statement.setString(2, user.getEmail().asString());
            if (statement.executeUpdate() != 1) {
                throw new UserDoesNotExist(user, "While updating study start date");
            }
        } catch (SQLException e) {
            throw Log.giveUp(logger, e, "Failed to update study start date for user: " + user);
        }
    }

    // TODO Write more proper repository methods to update the database.

    @Override
    public void update(Project project){}

    @Override
    public void delete(Project project){}

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
                Statement.RETURN_GENERATED_KEYS)) { //Henter den key der er genereret
            statement.setString(1, project.getProjectName());
            statement.setString(2, project.getDescription());
            statement.setInt(3, studyLineId);

            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {

                if (!keys.next()) {
                    throw new RuntimeException("No project ID generated");
                }

                project.setProjectID(keys.getInt(1)); //sætter den rigtige key på domain.
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
        try (var statement2 = getConnection().prepareStatement(stmt2)){
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
                JOIN study_line_memberships slm ON sl.id = slm.study_line_id
                JOIN roles r ON slm.role_id = r.id
                WHERE slm.user_id = (SELECT id FROM users WHERE email = ?)
                AND r.name = 'student'
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
    public List<StudyLine> getAllStudyLineObjects() {
        var stmt = "SELECT name FROM study_lines";
        List<StudyLine> studyLines = new ArrayList<>();
        try (var statement = getConnection().prepareStatement(stmt);
             var resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                studyLines.add(new StudyLine(
                        resultSet.getString("name")
                ));
            }
        } catch (SQLException e) {
            throw Log.giveUp(logger, e, "Failed to fetch study lines");
        }
        return studyLines;
    }

    @Override
    public List<Project> getAllProjectsRelatedToUser(User user) throws Project.InvalidProject {
        List<Project> projects = new ArrayList<>();
        int userId = getUserIdByEmail(user.getEmail());
        var stmt = """
                SELECT p.id, p.title, p.description, sl.name AS study_line_name
                FROM projects p
                LEFT JOIN study_lines sl ON p.study_line_id = sl.id
                WHERE p.id IN (
                    SELECT project_id FROM project_students WHERE user_id = ?
                    UNION
                    SELECT project_id FROM project_advisors WHERE user_id = ?
                )
                """;
        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setInt(1, userId);
            statement.setInt(2, userId);
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Project project = new Project(
                            resultSet.getString("title"),
                            resultSet.getString("description"));
                    project.setProjectID(resultSet.getInt("id"));
                    String studyLineName = resultSet.getString("study_line_name");
                    if (studyLineName != null) {
                        project.setStudyLine(studyLineName);
                    }
                    projects.add(project);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return projects;
    }

    @Override
    public List<Project> getAllProjectsRelatedToStudyLine(int studyLineId) {
        List<Project> projects = new ArrayList<>();
        var stmt = """
                SELECT id, title, description
                FROM projects
                WHERE study_line_id = ?
                """;
        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setInt(1, studyLineId);
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Project project = new Project(
                            resultSet.getString("title"),
                            resultSet.getString("description"));
                    project.setProjectID(resultSet.getInt("id"));
                    projects.add(project);
                }
            } catch (Project.InvalidProject e) {
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return projects;
    }

    @Override
    public List<Project> getAllProjectsRelatedToStudyLineAndUser(int studyLineId, User user) {
        List<Project> projects = new ArrayList<>();
        int userId = getUserIdByEmail(user.getEmail());
        var stmt = """
                SELECT p.id, p.title, p.description
                FROM projects p
                WHERE p.study_line_id = ?
                    AND (
                        EXISTS (
                            SELECT 1
                            FROM project_students ps
                            WHERE ps.project_id = p.id AND ps.user_id = ?
                        )
                        OR
                        EXISTS (
                            SELECT 1
                            FROM project_advisors pad
                            WHERE pad.project_id = p.id AND pad.user_id = ?
                        )
                    );
                """;
        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setInt(1, studyLineId);
            statement.setInt(2, userId);
            statement.setInt(3, userId);
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Project project = new Project(
                            resultSet.getString("title"),
                            resultSet.getString("description"));
                    project.setProjectID(resultSet.getInt("id"));
                    projects.add(project);
                }
            } catch (Project.InvalidProject e) {
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return projects;
    }

    @Override
    public List<Project> getAllProjects() throws Project.InvalidProject {
        List<Project> projects = new ArrayList<>();
        var stmt = """
        SELECT p.title, p.description, p.id
        FROM projects p
        """;
        try (var statement = getConnection().prepareStatement(stmt);
             var resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Project project = new Project(resultSet.getString("title"),
                        resultSet.getString("description"));
                project.setProjectID(resultSet.getInt("id"));
                projects.add(project);
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
    public String getProjectCreatorEmail(int projectId) {
        var stmt = """
                SELECT u.email
                FROM project_students ps
                JOIN users u ON u.id = ps.user_id
                WHERE ps.project_id = ?
                ORDER BY ps.user_id ASC
                LIMIT 1
                """;
        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setInt(1, projectId);
            try (var rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("email");
                }
            }
            return null;
        } catch (SQLException e) {
            throw Log.giveUp(logger, e, "Failed to read creator for project: " + projectId);
        }
    }

    @Override
    public void kickMemberFromProject(int projectId, User member) {
        int userId = getUserIdByEmail(member.getEmail());

        var deleteStudent = "DELETE FROM project_students WHERE project_id = ? AND user_id = ?";
        var deleteAdvisor = "DELETE FROM project_advisors WHERE project_id = ? AND user_id = ?";
        var deleteInvitation = "DELETE FROM advisor_invitations WHERE project_id = ? AND advisor_id = ?";

        try (var psDelete = getConnection().prepareStatement(deleteStudent);
                var paDelete = getConnection().prepareStatement(deleteAdvisor);
                var aiDelete = getConnection().prepareStatement(deleteInvitation)) {
            psDelete.setInt(1, projectId);
            psDelete.setInt(2, userId);
            psDelete.executeUpdate();

            paDelete.setInt(1, projectId);
            paDelete.setInt(2, userId);
            paDelete.executeUpdate();

            aiDelete.setInt(1, projectId);
            aiDelete.setInt(2, userId);
            aiDelete.executeUpdate();
        } catch (SQLException e) {
            throw Log.giveUp(logger, e,
                    "Failed to kick member from project: " + projectId + ", member: " + member.getEmail().asString());
        }
    }

    @Override
    public Project read(Project project) {
        try {
            return new Project( "t","");
        } catch (Project.InvalidProject e) {

        }
        return null;
    }

    @Override
    public String getStudyLineNameById(int studyLineId) {
        var stmt = """
                    SELECT name
                    FROM study_lines
                    WHERE id = ?
                    """;

        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setInt(1, studyLineId);

            var resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getString("name");
            }
        } catch (SQLException e) {
            throw Log.giveUp(logger, e, "Failed to check study line name, study line: " + studyLineId);
        }
        return "";
    }

    @Override
    public boolean canManageStudyLine(User user, int studyLineId) {
        int userId = getUserIdByEmail(user.getEmail());
        var stmt = """
                    SELECT 1
                    FROM study_line_memberships slm
                    JOIN roles r ON slm.role_id = r.id
                    WHERE slm.user_id = ? AND slm.study_line_id = ?
                    AND (r.name = 'student' OR r.name = 'study leader')
                    
                    """;

        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setInt(1, userId);
            statement.setInt(2, studyLineId);

            try (var resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw Log.giveUp(logger, e,
                    "Failed to check study line membership for user: " + user + ", study line: " + studyLineId);
        }
    }

    @Override
    public boolean canViewStudyLine(User user, int studyLineId) {
        int userId = getUserIdByEmail(user.getEmail());
        var stmt = """
                    SELECT 1
                    FROM study_line_memberships
                    WHERE user_id = ? AND study_line_id = ?
                    """;

        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setInt(1, userId);
            statement.setInt(2, studyLineId);

            try (var resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw Log.giveUp(logger, e,
                    "Failed to check study line membership for user: " + user + ", study line: " + studyLineId);
        }
    }

    @Override
    public List<UserInfo> getAllUsersRelatedToStudyLine(int studyLineId) {
        List<UserInfo> studyLineUsers = new ArrayList<>();
        var stmt = """
                    SELECT DISTINCT user_id
                    FROM study_line_memberships
                    WHERE study_line_id = ?
                    """;
        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setInt(1, studyLineId);
            var resultSet = statement.executeQuery();

            while (resultSet.next()) {
                int userId = resultSet.getInt("user_id");
                var currUser = getUserByUserId(userId);
                var currUserData = read(currUser);
                studyLineUsers.add(new UserInfo(String.valueOf(userId),
                        currUser.getEmail().asString(),
                        currUserData.name(),
                        currUserData.roles(),
                        currUserData.studyLine()));
            }
        } catch (SQLException e) {
            throw Log.giveUp(logger, e,
                    "Failed to get all users from study line: " + studyLineId);
        } catch (UserDoesNotExist e) {
            throw new RuntimeException(e);
        }
        return studyLineUsers;
    }

    @Override
    public List<UserInfo> getAllStudentsRelatedToStudyLine(int studyLineId) {
        List<UserInfo> studyLineUsers = new ArrayList<>();
        var stmt = """
                    SELECT slm.user_id AS user_id
                    FROM study_line_memberships slm
                    JOIN roles r ON slm.role_id = r.id
                    WHERE slm.study_line_id = ?
                    AND r.name = 'student'
                    """;
        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setInt(1, studyLineId);
            var resultSet = statement.executeQuery();

            while (resultSet.next()) {
                int userId = resultSet.getInt("user_id");
                var currUser = getUserByUserId(userId);
                var currUserData = read(currUser);
                studyLineUsers.add(new UserInfo(String.valueOf(userId),
                        currUser.getEmail().asString(),
                        currUserData.name(),
                        currUserData.roles(),
                        currUserData.studyLine()));
            }
        } catch (SQLException e) {
            throw Log.giveUp(logger, e,
                    "Failed to get all users from study line: " + studyLineId);
        } catch (UserDoesNotExist e) {
            throw new RuntimeException(e);
        }
        return studyLineUsers;
    }

    @Override
    public List<UserInfo> getAllLeadersRelatedToStudyLine(int studyLineId) {
        List<UserInfo> studyLineUsers = new ArrayList<>();
        var stmt = """
                    SELECT slm.user_id AS user_id
                    FROM study_line_memberships slm
                    JOIN roles r ON slm.role_id = r.id
                    WHERE slm.study_line_id = ?
                    AND r.name = 'study leader'
                    """;
        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setInt(1, studyLineId);
            var resultSet = statement.executeQuery();

            while (resultSet.next()) {
                int userId = resultSet.getInt("user_id");
                var currUser = getUserByUserId(userId);
                var currUserData = read(currUser);
                studyLineUsers.add(new UserInfo(String.valueOf(userId),
                        currUser.getEmail().asString(),
                        currUserData.name(),
                        currUserData.roles(),
                        currUserData.studyLine()));
            }
        } catch (SQLException e) {
            throw Log.giveUp(logger, e,
                    "Failed to get all users from study line: " + studyLineId);
        } catch (UserDoesNotExist e) {
            throw new RuntimeException(e);
        }
        return studyLineUsers;
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

    /**
     * creates an advisor invitation from a project to an advisor.
     * This method posts relevant data to the database to create an invitation.
     */
    @Override
    public void create (User invited, Project project){
        int invitedUserId = getUserIdByEmail(invited.getEmail());
        int projectId = project.getProjectID();
        var stmt = "INSERT INTO advisor_invitations (advisor_id, project_id) VALUES (?, ?)";
        try (var statement = getConnection().prepareStatement(stmt)){
            statement.setInt(1, invitedUserId);
            statement.setInt(2, projectId);
            statement.executeUpdate();
        } catch (SQLException e){
            if (e.getErrorCode() == 23505){
                throw new AdvisorAlreadyInvited("Advisor already invited");
            }
            throw Log.giveUp(logger, e, "Failed to create invitation");
        }
    }

    /**
     * Assigns a user (who can advise) to a specific project.
     * A user and a project is passed in as parameters, and the database is updated accordingly.
     */
    @Override
    public void assignAdvisor(User advisor, Project project) throws AdvisorAlreadyAdvises{
        int userId = getUserIdByEmail(advisor.getEmail());
        int projectId = project.getProjectID();
        var stmt = "INSERT INTO project_advisors (user_id, project_id) VALUES (?,?)";
        try (var statement = getConnection().prepareStatement(stmt)){
            statement.setInt(1, userId);
            statement.setInt(2, projectId);
            statement.executeUpdate();
        } catch (SQLException e){
            if (e.getErrorCode() == 23505){
                throw new AdvisorAlreadyAdvises("Advisor already assigned");
            }
            throw Log.giveUp(logger, e, "Failed to assign advisor");
        }
    }

    /**
     * Gets all invites received by a specific user from the DB
     */
    @Override
    public List<AdvisorInvitation> getAllInvitesReceived(User invited){
        int userId = getUserIdByEmail(invited.getEmail());
        List<AdvisorInvitation> invitations = new ArrayList<AdvisorInvitation>();
        var stmt = """
            SELECT advisor_id, project_id, status
            FROM advisor_invitations
            WHERE advisor_id = (?)
            """;
        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setInt(1,userId);
            var resultSet = statement.executeQuery();
            while (resultSet.next()) {
                var invitation = new AdvisorInvitation(
                        resultSet.getInt("advisor_id"),
                        resultSet.getInt("project_id")
                );
                String status = resultSet.getString("status");
                if (status.equals("accepted")) invitation.accept();
                if (status.equals("declined")) invitation.decline();
                invitations.add(invitation);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return invitations;
    }

    @Override
    public List<dk.utd.fordel.services.InvitationManager.AdvisorInviteView> getInvitesReceivedForAdvisor(
            int advisorId) {
        List<dk.utd.fordel.services.InvitationManager.AdvisorInviteView> views = new ArrayList<>();
        var stmt = """
                SELECT p.id AS project_id, p.title AS project_title,
                    (SELECT u.name FROM users u JOIN project_students ps ON ps.user_id = u.id WHERE ps.project_id = p.id LIMIT 1) AS inviter_name,
                    ai.status
                FROM advisor_invitations ai
                JOIN projects p ON ai.project_id = p.id
                WHERE ai.advisor_id = ?
                """;
        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setInt(1, advisorId);
            try (var rs = statement.executeQuery()) {
                while (rs.next()) {
                    int projectId = rs.getInt("project_id");
                    String projectTitle = rs.getString("project_title");
                    String inviterName = rs.getString("inviter_name");
                    String status = rs.getString("status");
                    String statusClass = status != null ? status.toLowerCase() : "pending";
                    String displayStatus = status != null && !status.isBlank()
                            ? (Character.toUpperCase(status.charAt(0)) + status.substring(1))
                            : "Pending";
                    views.add(new dk.utd.fordel.services.InvitationManager.AdvisorInviteView(
                            projectTitle != null ? projectTitle : "Project " + projectId,
                            inviterName != null ? inviterName : "", displayStatus, statusClass, projectId));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return views;
    }

    @Override
    public List<dk.utd.fordel.services.InvitationManager.InviteView> getInvitesForProject(int projectId) {
        List<dk.utd.fordel.services.InvitationManager.InviteView> views = new ArrayList<>();
        var stmt = """
                SELECT u.email, u.name, ai.status
                FROM advisor_invitations ai
                JOIN users u ON ai.advisor_id = u.id
                WHERE ai.project_id = ?
                """;
        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setInt(1, projectId);
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String email = resultSet.getString("email");
                    String name = resultSet.getString("name");
                    String status = resultSet.getString("status");
                    String statusClass = status != null ? status : "pending";
                    views.add(new dk.utd.fordel.services.InvitationManager.InviteView(name, email,
                            status != null ? status : "pending", statusClass));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return views;
    }

    @Override
    public void update(AdvisorInvitation advisorInvitation) throws  AdvisorAlreadyInvited{
        String status = advisorInvitation.getStatusAsString();
        int advisor_id = advisorInvitation.getAdvisorId();
        int project_id = advisorInvitation.getProjectId();
        var stmt = """
                UPDATE advisor_invitations SET status = (?)
                WHERE advisor_id = (?)
                AND project_id = (?)
                """;
        try (var statement = getConnection().prepareStatement(stmt)){
            statement.setString(1,status);
            statement.setInt(2,advisor_id);
            statement.setInt(3,project_id);
            statement.executeUpdate();

            // Keep project_advisors in sync with invitation decision
            if ("accepted".equals(status)) {
                var advisorStmt = "INSERT INTO project_advisors (user_id, project_id) VALUES (?, ?)";
                try (var advisorInsert = getConnection().prepareStatement(advisorStmt)) {
                    advisorInsert.setInt(1, advisor_id);
                    advisorInsert.setInt(2, project_id);
                    advisorInsert.executeUpdate();
                } catch (SQLException e) {
                    // Ignore duplicate key if advisor already linked
                    if (e.getErrorCode() != 23505) {
                        throw e;
                    }
                }
            } else if ("declined".equals(status)) {
                var removeStmt = "DELETE FROM project_advisors WHERE user_id = ? AND project_id = ?";
                try (var remove = getConnection().prepareStatement(removeStmt)) {
                    remove.setInt(1, advisor_id);
                    remove.setInt(2, project_id);
                    remove.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw Log.giveUp(logger, e, "Failed to update advisor invitations table");
        }
    }
    @Override
    public void delete(){

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
    public List<String> getAllStudyLines(){
        List<String> studyLines = new ArrayList<>();

        var stmt = "SELECT name FROM study_lines ORDER BY name";

        try (var statement = getConnection().prepareStatement(stmt)) {
            var resultSet = statement.executeQuery();

            while (resultSet.next()) {
                studyLines.add(resultSet.getString("name"));
            }

            return studyLines;
        } catch (SQLException e) {
            throw Log.giveUp(logger, e, "Failed to get study lines");
        }
    }

    @Override
    public void assignStudyLine(User user, String studyLineName) {
        var stmt = """
            MERGE INTO study_line_memberships (user_id, study_line_id, role_id)
            KEY (user_id, study_line_id, role_id)
            SELECT u.id, sl.id, r.id
            FROM users u, study_lines sl, roles r
            WHERE u.email = ?
            AND sl.name = ?
            AND r.name = 'student'
            """;

        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setString(1, user.getEmail().asString());
            statement.setString(2, studyLineName);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw Log.giveUp(logger, e, "Failed to assign study line to user");
        }
    }

    @Override
    public void addStudent(User user, int studyLineId) {
        var stmt = """
                MERGE INTO study_line_memberships (user_id, study_line_id, role_id)
                KEY (user_id, study_line_id, role_id)
                SELECT u.id, ?, r.id
                FROM users u, roles r
                WHERE u.email = ?
                AND r.name = 'student'
                """;

        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setInt(1, studyLineId);
            statement.setString(2, user.getEmail().asString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw Log.giveUp(logger, e, "Could not add student to study line");
        }
    }

    @Override
    public void addStudyLeader(User user, int studyLineId) {
        var stmt = """
                MERGE INTO study_line_memberships (user_id, study_line_id, role_id)
                KEY (user_id, study_line_id, role_id)
                SELECT u.id, ?, r.id
                FROM users u, roles r
                WHERE u.email = ?
                AND r.name = 'study leader'
                """;

        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setInt(1, studyLineId);
            statement.setString(2, user.getEmail().asString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw Log.giveUp(logger, e, "Could not add study leader to study line");
        }
    }

    @Override
    public void removeUser(User user, int studyLineId) {
        var stmt = """
               DELETE FROM study_line_memberships
                    WHERE user_id = ?
                    AND study_line_id = ?
        """;

        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setInt(1, getUserIdByEmail(user.getEmail()));
            statement.setInt(2, studyLineId);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw Log.giveUp(logger, e, "Could not remove user from study line");
        }

    }

    @Override
    public List<Project> getAllAdvisorProjects(User advisor){
        List<Project> projects = new ArrayList<>();
        int userId = getUserIdByEmail(advisor.getEmail());
        var stmt = """
                SELECT p.id, p.title, p.description, sl.name
                FROM projects p JOIN project_advisors pa ON p.id = pa.project_id
                    JOIN study_lines sl on p.study_line_id = sl.id
                WHERE pa.user_id = ?
                """;
        try (var statement = getConnection().prepareStatement(stmt)) {
            statement.setInt(1, userId);
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Project project = new Project(
                            resultSet.getString("title"),
                            resultSet.getString("description"));
                    project.setProjectID(resultSet.getInt("id"));
                    String studyLineName = resultSet.getString("name");
                    if (studyLineName != null) {
                        project.setStudyLine(studyLineName);
                    }
                    projects.add(project);

                }
            } catch (Project.InvalidProject e){
                throw Log.giveUp(logger, e, "Failed to create projects from database. invalid project");
            }
        }catch (SQLException e){
            throw Log.giveUp(logger, e, "Failed to fetch projects");
        }

        return projects;
    }

    @Override
    public int getCountOfAdvisorProjects(User advisor){

        var stmt = """
                SELECT COUNT(*)
                FROM project_advisors pa JOIN projects p 
                    ON pa.project_id = p.id 
                WHERE user_id = ? AND p.STATUS = 'DRAFT'
                """;
        try (var statement = getConnection().prepareStatement(stmt)) {
            int userId = getUserIdByEmail(advisor.getEmail());
            statement.setInt(1,userId);
            try (var resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        } catch (SQLException e){
            throw Log.giveUp(logger, e, "Failed to fetch advisor/project relation");
        }
        return 0;
    }

    @Override
    public void StartProject(User advisor, Project project){
        var stmt = """
                UPDATE projects
                SET status = 'PUBLISHED'
                WHERE project_id = ?
                """;
        if (project.getStatus() == Project.Status.DRAFT) {
            try (var statement = getConnection().prepareStatement(stmt)) {
                project.setStatus(Project.Status.STARTED);
            } catch (SQLException e) {
                throw Log.giveUp(logger, e, "Failed to publish project");
            }
        } else {
            throw new ProjectAlreadyFinished("Project already published");
        }
    }

    @Override
    public int getMaxWorkLoad(User user){
        var stmt = "SELECT max_workload FROM users WHERE id = ?";
        try (var statement = getConnection().prepareStatement(stmt)){
            int userId = getUserIdByEmail(user.getEmail());
            statement.setInt(1,userId);
            try (var rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e){
            throw Log.giveUp(logger, e, "Failed to fetch maximum workload");
        }
        return 0;
    }

    @Override
    public void updateMaxWorkLoad(User user, int newMaxLoad) {
        var stmt = """
                UPDATE users
                SET max_workload = ?
                WHERE id = ?
                """;
        try (var statement = getConnection().prepareStatement(stmt)){
            int userid = getUserIdByEmail(user.getEmail());
            statement.setInt(1,newMaxLoad);
            statement.setInt(2,userid);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw Log.giveUp(logger, e, "Failed to update maximum workload");
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

