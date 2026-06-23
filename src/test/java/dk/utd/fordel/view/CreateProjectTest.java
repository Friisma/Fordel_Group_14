package dk.utd.fordel.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dk.utd.fordel.domain.Project;
import dk.utd.fordel.domain.ProjectTitleAlreadyInUse;
import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.services.Authenticator;
import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.services.StudyLineManager;
import dk.utd.fordel.services.UserManager;
import dk.utd.fordel.services.Authenticator.DefaultPasswordEncoder;
import dk.utd.fordel.testsupport.TestFakes.FakePasswordRepository;
import dk.utd.fordel.testsupport.TestFakes.FakeProjectRepository;
import dk.utd.fordel.testsupport.TestFakes.FakeStudyLineRepository;
import dk.utd.fordel.testsupport.TestFakes.FakeUnitOfWork;
import dk.utd.fordel.testsupport.TestFakes.FakeUserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

class CreateProjectTest {

    private FakeUnitOfWork unitOfWork;
    private FakeProjectRepository projectRepository;
    private FakeUserRepository userRepository;
    private FakeStudyLineRepository studyLineRepository;
    private Authenticator authenticator;
    private CreateProject controller;

    @BeforeEach
    void setUp() {
        unitOfWork = new FakeUnitOfWork();
        projectRepository = new FakeProjectRepository();
        userRepository = new FakeUserRepository();
        studyLineRepository = new FakeStudyLineRepository();
        studyLineRepository.studyLines.addAll(List.of("Software Technology", "Computer Science"));
        authenticator = new Authenticator(new DefaultPasswordEncoder(), new FakePasswordRepository());
        controller = new CreateProject(
                unitOfWork,
                new ProjectManager(projectRepository),
                authenticator,
                new UserManager(userRepository),
                new StudyLineManager(studyLineRepository));
    }

    @Test
    void post_create_project_succeeds_for_a_valid_student_project() throws Exception {
        MockHttpSession session = sessionWith("student@fordel.dk");
        Model model = new ExtendedModelMap();
        userRepository.studyLineRoles.put("student@fordel.dk|Software Technology", Roles.STUDENT);
        userRepository.userIds.put("student@fordel.dk", 42);

        String view = controller.postCreateProject(model, "Thesis tool", "Tooling for thesis work",
                "Software Technology", session);

        assertEquals("redirect:/", view);
        assertEquals("Thesis tool", projectRepository.lastCreatedProject.getProjectName());
        assertEquals("Software Technology", projectRepository.lastCreatedStudyLine);
        assertEquals(42, projectRepository.lastCreatedUserId);
    }

    @Test
    void post_create_project_rejects_empty_name() throws Exception {
        MockHttpSession session = sessionWith("student@fordel.dk");
        Model model = new ExtendedModelMap();
        userRepository.studyLineRoles.put("student@fordel.dk|Software Technology", Roles.STUDENT);

        String view = controller.postCreateProject(model, "", "Tooling for thesis work", "Software Technology",
                session);

        assertEquals("create-project", view);
        assertTrue(model.asMap().containsKey("project_name"));
        assertEquals(null, projectRepository.lastCreatedProject);
    }

    @Test
    void post_create_project_rejects_empty_description() throws Exception {
        MockHttpSession session = sessionWith("student@fordel.dk");
        Model model = new ExtendedModelMap();
        userRepository.studyLineRoles.put("student@fordel.dk|Software Technology", Roles.STUDENT);

        String view = controller.postCreateProject(model, "Thesis tool", "", "Software Technology", session);

        assertEquals("create-project", view);
        assertTrue(model.asMap().containsKey("project_description"));
        assertEquals(null, projectRepository.lastCreatedProject);
    }

    @Test
    void post_create_project_rejects_missing_study_line() throws Exception {
        MockHttpSession session = sessionWith("student@fordel.dk");
        Model model = new ExtendedModelMap();

        String view = controller.postCreateProject(model, "Thesis tool", "Tooling for thesis work", "", session);

        assertEquals("create-project", view);
        assertTrue(model.asMap().containsKey("project_name"));
        assertEquals(1, unitOfWork.beginCount);
    }

    @Test
    void post_create_project_rejects_study_line_mismatch() throws Exception {
        MockHttpSession session = sessionWith("student@fordel.dk");
        Model model = new ExtendedModelMap();

        String view = controller.postCreateProject(model, "Thesis tool", "Tooling for thesis work",
                "Software Technology", session);

        assertEquals("create-project", view);
        assertEquals(null, projectRepository.lastCreatedProject);
    }

    @Test
    void post_create_project_rejects_duplicate_title() throws Exception {
        MockHttpSession session = sessionWith("student@fordel.dk");
        Model model = new ExtendedModelMap();
        userRepository.studyLineRoles.put("student@fordel.dk|Software Technology", Roles.STUDENT);
        userRepository.userIds.put("student@fordel.dk", 42);
        projectRepository.throwProjectTitleAlreadyInUse = true;

        String view = controller.postCreateProject(model, "Thesis tool", "Tooling for thesis work",
                "Software Technology", session);

        assertEquals("create-project", view);
        assertTrue(model.asMap().containsKey("project_name"));
    }

    @Test
    void post_create_advisor_project_succeeds() throws Exception {
        MockHttpSession session = sessionWith("advisor@fordel.dk");
        Model model = new ExtendedModelMap();
        userRepository.userIds.put("advisor@fordel.dk", 7);

        String view = controller.postCreateProjectAdvisor(model, "Advisor project", "Advisor-only project",
                "Software Technology", session);

        assertEquals("redirect:/", view);
        assertEquals("Advisor project", projectRepository.lastCreatedAdvisorProject.getProjectName());
        assertEquals("Software Technology", projectRepository.lastCreatedAdvisorStudyLine);
        assertEquals(7, projectRepository.lastCreatedAdvisorUserId);
    }

    @Test
    void post_create_advisor_project_rejects_duplicate_title() throws Exception {
        MockHttpSession session = sessionWith("advisor@fordel.dk");
        Model model = new ExtendedModelMap();
        userRepository.userIds.put("advisor@fordel.dk", 7);
        projectRepository.throwProjectTitleAlreadyInUse = true;

        String view = controller.postCreateProjectAdvisor(model, "Advisor project", "Advisor-only project",
                "Software Technology", session);

        assertEquals("create-project", view);
        assertTrue(model.asMap().containsKey("project_name"));
    }

    private static MockHttpSession sessionWith(String email) throws dk.utd.fordel.domain.Email.InvalidEmail {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("auth", new Authenticator.Auth(User.of(email)));
        session.setAttribute("userData", new User.Data("Name", List.of(Roles.STUDENT), null, null));
        return session;
    }
}