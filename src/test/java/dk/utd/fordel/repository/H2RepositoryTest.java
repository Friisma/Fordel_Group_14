package dk.utd.fordel.repository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dk.utd.fordel.domain.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;

import dk.utd.fordel.domain.Email.InvalidEmail;
import dk.utd.fordel.services.Authenticator.DefaultPasswordEncoder;
import dk.utd.fordel.services.Authenticator.PasswordEncoding;
import dk.utd.fordel.utils.UnitOfWork.UnitOfWorkException;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class H2RepositoryTest {

    static H2Repository h2Repository;
    static DataSource dataSource;

    @BeforeAll
    static void setUp() {
        // Set up the test environment

        dataSource = DataSourceBuilder.create()
                .url("jdbc:h2:mem:testdb")
                .driverClassName("org.h2.Driver")
                .username("sa")
                .password("")
                .build();

        h2Repository = new H2Repository(dataSource);
    }

    @Test
    void creating_a_project_with_an_existing_name_should_fail()
            throws ProjectTitleAlreadyInUse, UnitOfWorkException, Project.InvalidProject, EmailAlreadyInUse,
            StudyLineAlreadyExists {
        Email email = new Email("test", "test");
        User user = new User(email);
        StudyLine studyLine = new StudyLine("onlyForTest");

        User.Data data = new User.Data("name", new ArrayList<>(), "line", LocalDate.of(2001, 1, 1));
        Project project = new Project("test", "test");

        try (var ignored = h2Repository.begin()) {
            h2Repository.create(studyLine);
            h2Repository.create(user, data);
            h2Repository.assignStudyLine(user, studyLine.getLineName());

            int userId = h2Repository.getUserIdByEmail(user.getEmail());

            h2Repository.create(project, user, userId);
            assertThrows(ProjectTitleAlreadyInUse.class,
                    () -> h2Repository.create(project, user, userId));
        }
    }

    @Test
    void creatingAProjectWithAStudyLine()
            throws UserDoesNotExist, EmailAlreadyInUse, UnitOfWorkException, StudyLineAlreadyExists,
            Project.InvalidProject {
        StudyLine studyLine = new StudyLine("testing");
        Email email = new Email("test", "test");
        User user = new User(email);
        Project project = new Project("test1", "test1");

        try (var ignored = h2Repository.begin()) {
            h2Repository.create(studyLine);
            h2Repository.create(user, new User.Data("Peter", List.of(), "testing", null));
            int userid = h2Repository.getUserIdByEmail(email);
            h2Repository.create(project, user, "testing", userid);
        }

    }

    @Test
    void adviserCantBeAssignedAsAdviserAgain()
            throws EmailAlreadyInUse, UnitOfWorkException, Project.InvalidProject, StudyLineAlreadyExists {
        Email email = new Email("test", "test");
        User user = new User(email);
        Project project = new Project("test", "test");
        StudyLine studyLine = new StudyLine("testing");
        try (var ignored = h2Repository.begin()) {
            h2Repository.create(studyLine);
            h2Repository.create(user, new User.Data("Peter", List.of(), "testing", null));
            h2Repository.assignStudyLine(user, studyLine.getLineName());
            h2Repository.create(project, user, h2Repository.getUserIdByEmail(user.getEmail()));

            h2Repository.assignAdvisor(user, project);
            assertThrows(AdvisorAlreadyAdvises.class,
                    () -> h2Repository.assignAdvisor(user, project));

        }
    }

    @Test
    void invitingTwiceShouldFail()
            throws EmailAlreadyInUse, UnitOfWorkException, Project.InvalidProject, StudyLineAlreadyExists {
        Email email = new Email("test", "test");
        User user = new User(email);

        Email emailAdvisor = new Email("test2", "test2");
        User advisor = new User(emailAdvisor);

        Project project = new Project("test", "test");
        StudyLine studyLine = new StudyLine("testing");
        try (var ignored = h2Repository.begin()) {
            h2Repository.create(studyLine);
            h2Repository.create(user, new User.Data("Peter", List.of(), "testing", null));
            h2Repository.create(advisor, new User.Data("PeterAdvisor", List.of(), "testing", null));

            h2Repository.assignStudyLine(user, studyLine.getLineName());
            h2Repository.create(project, user, h2Repository.getUserIdByEmail(user.getEmail()));

            // creates project invitation
            h2Repository.create(advisor, project);
            assertThrows(AdvisorAlreadyInvited.class,
                    () -> h2Repository.create(advisor, project));
        }
    }

    @Test
    void cannotAssignAdvisorMoreThanItsMaxWorkLoad()
            throws EmailAlreadyInUse, UnitOfWorkException, Project.InvalidProject, StudyLineAlreadyExists {
        Email email = new Email("test", "test");
        User user = new User(email);

        Email emailAdvisor = new Email("test2", "test2");
        User advisor = new User(emailAdvisor);

        Project project = new Project("test", "test");
        Project project2 = new Project("test2", "test2");
        StudyLine studyLine = new StudyLine("testing");
        try (var ignored = h2Repository.begin()) {
            h2Repository.create(studyLine);
            h2Repository.create(user, new User.Data("Peter", List.of(), "testing", null));
            h2Repository.create(advisor, new User.Data("PeterAdvisor", List.of(), "testing", null));

            h2Repository.assignStudyLine(user, studyLine.getLineName());
            h2Repository.create(project, user, h2Repository.getUserIdByEmail(user.getEmail()));
            h2Repository.create(project2, user, h2Repository.getUserIdByEmail(user.getEmail()));

            h2Repository.updateMaxWorkLoad(advisor, 1);

            h2Repository.assignAdvisor(advisor, project);

            assertThrows(AdvisorReachedMaxWorkLoad.class,
                    () -> h2Repository.assignAdvisor(advisor, project2));

        }
    }

    @Test
    void getAllProjects_returnsAllProjects() throws Exception {
        Email email = new Email("test", "test");
        User user = new User(email);
        Project project = new Project("test", "test");
        StudyLine studyLine = new StudyLine("testing");

        try (var ignored = h2Repository.begin()) {
            h2Repository.create(studyLine);
            h2Repository.create(user, new User.Data("Peter", List.of(), "testing", null));

            h2Repository.assignStudyLine(user, studyLine.getLineName());
            h2Repository.create(project, user, h2Repository.getUserIdByEmail(user.getEmail()));

            List<Project> projects = h2Repository.getAllProjects();

            assertEquals(project.getProjectName(), projects.getFirst().getProjectName());
            assertEquals(project.getDescription(), projects.getFirst().getDescription());
        }
    }

    @Test
    void startingProject() throws Exception {
        Email email = new Email("test", "test");
        User user = new User(email);
        Project project = new Project("test", "test");
        StudyLine studyLine = new StudyLine("testing");

        try (var ignored = h2Repository.begin()) {
            h2Repository.create(studyLine);
            h2Repository.create(user, new User.Data("Peter", List.of(), "testing", null));

            h2Repository.assignStudyLine(user, studyLine.getLineName());
            h2Repository.create(project, user, h2Repository.getUserIdByEmail(user.getEmail()));
            h2Repository.startProject(project.getProjectID());
            List<Project> projects = h2Repository.getAllProjectsRelatedToUser(user);
            // need to re-fetch from DB.
            Project projectTest = projects.getFirst();

            assertEquals(Project.Status.STARTED, projectTest.getStatus());
        }
    }

    @Test
    void updatingProject() throws Exception {
        Email email = new Email("test", "test");
        User user = new User(email);
        Project project = new Project("test", "test");
        StudyLine studyLine = new StudyLine("testing");

        try (var ignored = h2Repository.begin()) {
            h2Repository.create(studyLine);
            h2Repository.create(user, new User.Data("Peter", List.of(), "testing", null));

            h2Repository.assignStudyLine(user, studyLine.getLineName());
            h2Repository.create(project, user, h2Repository.getUserIdByEmail(user.getEmail()));
            project.setProjectName("other test");
            h2Repository.update(project);

            List<Project> projects = h2Repository.getAllProjectsRelatedToUser(user);
            Project projectTest = projects.getFirst();

            assertEquals("other test", projectTest.getProjectName());

        }
    }

    @Test
    void updatingNoProjectFails() throws Exception {
        Project project = new Project("test", "test");
        try (var ignored = h2Repository.begin()) {
            assertThrows(ProjectDoesNotExist.class,
                    () -> h2Repository.update(project));
        }

    }

    @Test
    void setting_the_password_for_a_non_existent_user_should_fail() throws InvalidEmail, UnitOfWorkException {
        var user = User.of("unknown@user.dk");
        try (var ignored = h2Repository.begin()) {
            assertThrows(UserDoesNotExist.class,
                    () -> h2Repository.setPassword(user, new PasswordEncoding("password")));
        }
    }

    @Test
    void setting_the_password_for_an_existing_user_should_succeed()
            throws InvalidEmail, UserDoesNotExist, EmailAlreadyInUse, UnitOfWorkException, SQLException {
        var user = User.of("known@user.dk");
        try (var uow = h2Repository.begin()) {
            h2Repository.create(user, new User.Data("Peter", List.of(), null, null));
            var encoding = new DefaultPasswordEncoder().encode("password");
            h2Repository.setPassword(user, encoding);
            assertEquals(encoding, h2Repository.getPassword(user));
            uow.commit();
        }

        try (var connection = dataSource.getConnection();
                var statement = connection.prepareStatement("SELECT password_changed_at FROM users WHERE email = ?")) {
            statement.setString(1, user.getEmail().asString());
            try (var resultSet = statement.executeQuery()) {
                assertTrue(resultSet.next());
                LocalDateTime passwordChangedAt = resultSet.getObject("password_changed_at", LocalDateTime.class);
                assertTrue(passwordChangedAt != null);
                assertEquals(LocalDate.now(), passwordChangedAt.toLocalDate());
            }
        }

    }

    @Test
    void only_users_linked_to_a_project_can_view_it() throws Exception {
        String suffix = UUID.randomUUID().toString();
        User member = User.of("student@test.dk");
        User outsider = User.of("outsider-" + suffix + "@test.dk");
        Project project = new Project("Project " + suffix, "desc");

        try (var ignored = h2Repository.begin()) {
            h2Repository.create(outsider, new User.Data("Outsider", List.of(), null, null));

            int memberId = h2Repository.getUserIdByEmail(member.getEmail());
            h2Repository.create(project, member, memberId);

            assertTrue(h2Repository.canViewProject(member, project.getProjectID()));
            assertTrue(!h2Repository.canViewProject(outsider, project.getProjectID()));
        }
    }

    @Test
    void creating_the_same_user_twice_should_fail()
            throws InvalidEmail, UserDoesNotExist, EmailAlreadyInUse, UnitOfWorkException {
        var user = User.of("known@user.dk");
        try (var uow = h2Repository.begin()) {
            h2Repository.create(user, new User.Data("Peter", List.of(), null, null));
            assertThrows(EmailAlreadyInUse.class,
                    () -> h2Repository.create(user, new User.Data("Peter", List.of(), null, null)));
        }

        // This is okay because the previous transaction was not committed.
        try (var uow = h2Repository.begin()) {
            assertDoesNotThrow(() -> h2Repository.create(user, new User.Data("Peter", List.of(), null, null)));
            uow.commit();
        }

        // Since the transaction was commited, trying to create the same user again
        // fails
        try (var uow = h2Repository.begin()) {
            assertThrows(EmailAlreadyInUse.class,
                    () -> h2Repository.create(user, new User.Data("Peter", List.of(), null, null)));
        }

        // But now we need to cleanup the database
        try (var uow = h2Repository.begin()) {
            h2Repository.delete(user);
            uow.commit();
        }
    }

}
