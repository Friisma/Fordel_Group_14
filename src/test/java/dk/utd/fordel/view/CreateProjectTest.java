package dk.utd.fordel.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dk.utd.fordel.domain.Project;
import dk.utd.fordel.domain.ProjectTitleAlreadyInUse;
import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.services.Authenticator;
import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.services.StudyLineManager;
import dk.utd.fordel.services.UserManager;
import dk.utd.fordel.utils.UnitOfWork;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

@ExtendWith(MockitoExtension.class)
class CreateProjectTest {

    @Mock
    private UnitOfWork unitOfWork;

    @Mock
    private UnitOfWork activeUnitOfWork;

    @Mock
    private ProjectManager projectManager;

    @Mock
    private Authenticator authenticator;

    @Mock
    private UserManager userManager;

    @Mock
    private StudyLineManager studyLineManager;

    private CreateProject controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new CreateProject(unitOfWork, projectManager, authenticator, userManager, studyLineManager);
        lenient().when(unitOfWork.begin()).thenReturn(activeUnitOfWork);
        lenient().when(studyLineManager.getAllStudyLines())
                .thenReturn(List.of("Software Technology", "Computer Science"));
    }

    @Test
    void post_create_project_succeeds_for_a_valid_student_project() throws Exception {
        MockHttpSession session = sessionWith("student@fordel.dk");
        Model model = new ExtendedModelMap();

        when(userManager.determineStudyLineSpecificRole(any(), eq("Software Technology"))).thenReturn(Roles.STUDENT);
        when(userManager.getUserIdByEmail(any())).thenReturn(42);

        String view = controller.postCreateProject(model, "Thesis tool", "Tooling for thesis work",
                "Software Technology", session);

        assertEquals("redirect:/", view);
        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(projectManager).createProject(projectCaptor.capture(), any(User.class), eq("Software Technology"),
                eq(42));
        Project project = projectCaptor.getValue();
        assertEquals("Thesis tool", project.getProjectName());
        assertEquals("Tooling for thesis work", project.getDescription());
        assertEquals(Project.Status.DRAFT, project.getStatus());
        assertEquals("", project.getStudyLine());
    }

    @Test
    void post_create_project_rejects_empty_name() throws Exception {
        MockHttpSession session = sessionWith("student@fordel.dk");
        Model model = new ExtendedModelMap();

        when(userManager.determineStudyLineSpecificRole(any(), eq("Software Technology"))).thenReturn(Roles.STUDENT);

        String view = controller.postCreateProject(model, "", "Tooling for thesis work", "Software Technology",
                session);

        assertEquals("create-project", view);
        assertTrue(model.asMap().containsKey("project_name"));
        verify(projectManager, never()).createProject(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void post_create_project_rejects_empty_description() throws Exception {
        MockHttpSession session = sessionWith("student@fordel.dk");
        Model model = new ExtendedModelMap();

        when(userManager.determineStudyLineSpecificRole(any(), eq("Software Technology"))).thenReturn(Roles.STUDENT);

        String view = controller.postCreateProject(model, "Thesis tool", "", "Software Technology", session);

        assertEquals("create-project", view);
        assertTrue(model.asMap().containsKey("project_description"));
        verify(projectManager, never()).createProject(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void post_create_project_rejects_missing_study_line() throws Exception {
        MockHttpSession session = sessionWith("student@fordel.dk");
        Model model = new ExtendedModelMap();

        String view = controller.postCreateProject(model, "Thesis tool", "Tooling for thesis work", "", session);

        assertEquals("create-project", view);
        assertTrue(model.asMap().containsKey("project_name"));
        verify(userManager, never()).determineStudyLineSpecificRole(any(), any());
        verify(projectManager, never()).createProject(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void post_create_project_rejects_study_line_mismatch() throws Exception {
        MockHttpSession session = sessionWith("student@fordel.dk");
        Model model = new ExtendedModelMap();

        when(userManager.determineStudyLineSpecificRole(any(), eq("Software Technology"))).thenReturn(null);

        String view = controller.postCreateProject(model, "Thesis tool", "Tooling for thesis work",
                "Software Technology", session);

        assertEquals("create-project", view);
        verify(projectManager, never()).createProject(any(), any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void post_create_project_rejects_duplicate_title() throws Exception {
        MockHttpSession session = sessionWith("student@fordel.dk");
        Model model = new ExtendedModelMap();

        when(userManager.determineStudyLineSpecificRole(any(), eq("Software Technology"))).thenReturn(Roles.STUDENT);
        when(userManager.getUserIdByEmail(any())).thenReturn(42);
        org.mockito.Mockito.doThrow(new ProjectTitleAlreadyInUse("duplicate"))
                .when(projectManager)
                .createProject(any(Project.class), any(User.class), eq("Software Technology"), eq(42));

        String view = controller.postCreateProject(model, "Thesis tool", "Tooling for thesis work",
                "Software Technology", session);

        assertEquals("create-project", view);
        assertTrue(model.asMap().containsKey("project_name"));
        verify(projectManager).createProject(any(Project.class), any(User.class), eq("Software Technology"), eq(42));
    }

    @Test
    void post_create_advisor_project_succeeds() throws Exception {
        MockHttpSession session = sessionWith("advisor@fordel.dk");
        Model model = new ExtendedModelMap();

        when(userManager.getUserIdByEmail(any())).thenReturn(7);

        String view = controller.postCreateProjectAdvisor(model, "Advisor project", "Advisor-only project",
                "Software Technology", session);

        assertEquals("redirect:/", view);
        verify(projectManager).createAdvisorProject(any(Project.class), eq("Software Technology"), eq(7));
    }

    @Test
    void post_create_advisor_project_rejects_duplicate_title() throws Exception {
        MockHttpSession session = sessionWith("advisor@fordel.dk");
        Model model = new ExtendedModelMap();

        when(userManager.getUserIdByEmail(any())).thenReturn(7);
        org.mockito.Mockito.doThrow(new ProjectTitleAlreadyInUse("duplicate"))
                .when(projectManager)
                .createAdvisorProject(any(Project.class), eq("Software Technology"), eq(7));

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