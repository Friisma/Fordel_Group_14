package dk.utd.fordel.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.services.InvitationManager;
import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.services.Authenticator;
import dk.utd.fordel.services.UserManager;
import dk.utd.fordel.services.rights.RightsService;
import dk.utd.fordel.utils.UnitOfWork;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class ProjectControllerRightsTest {

    @Mock
    private ProjectManager projectManager;

    @Mock
    private UserManager userManager;

    @Mock
    private InvitationManager invitationManager;

    @Mock
    private UnitOfWork unitOfWork;

    @Mock
    private UnitOfWork activeUnitOfWork;

    private RightsService rightsService;
    private ProjectController controller;

    @BeforeEach
    void setUp() throws Exception {
        rightsService = new RightsService();
        controller = new ProjectController(rightsService, projectManager, unitOfWork, userManager, invitationManager);
        lenient().when(unitOfWork.begin()).thenReturn(activeUnitOfWork);
    }

    @Test
    void advisor_can_open_the_advisor_dashboard() throws Exception {
        MockHttpSession session = sessionWith(new User.Data("Advisor", List.of(Roles.ADVISOR), null, null),
                "advisor@fordel.dk");
        Model model = new ExtendedModelMap();

        when(userManager.getMaxWorkLoad(any())).thenReturn(3);
        when(projectManager.getCountOfAdvisedProjects(any())).thenReturn(1);
        when(projectManager.getAllAdvisorProjects(any())).thenReturn(List.of());

        String view = controller.getAllAdviserProjects(model, session);

        assertEquals("advisor-dashboard", view);
        assertEquals(3, model.asMap().get("maxProjectCount"));
        assertEquals(1, model.asMap().get("projectCount"));
        verify(unitOfWork).begin();
        verify(projectManager).getAllAdvisorProjects(any());
    }

    @Test
    void student_is_denied_access_to_the_advisor_dashboard() throws Exception {
        MockHttpSession session = sessionWith(new User.Data("Student", List.of(Roles.STUDENT), null, null),
                "student@fordel.dk");

        String view = controller.getAllAdviserProjects(new ExtendedModelMap(), session);

        assertEquals("redirect:/forbidden", view);
        verifyNoInteractions(unitOfWork);
    }

    @Test
    void advisor_can_start_project() throws Exception {
        MockHttpSession session = sessionWith(new User.Data("Advisor", List.of(Roles.ADVISOR), null, null),
                "advisor@fordel.dk");

        String view = controller.startProject(session, 7, new RedirectAttributesModelMap());

        assertEquals("redirect:/project/7", view);
        verify(unitOfWork).begin();
        verify(projectManager).startProject(7);
    }

    @Test
    void student_is_denied_starting_a_project() throws Exception {
        MockHttpSession session = sessionWith(new User.Data("Student", List.of(Roles.STUDENT), null, null),
                "student@fordel.dk");

        String view = controller.startProject(session, 7, new RedirectAttributesModelMap());

        assertEquals("redirect:/forbidden", view);
        verifyNoInteractions(unitOfWork);
        verify(projectManager, never()).startProject(any(Integer.class));
    }

    private static MockHttpSession sessionWith(User.Data data, String email) throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("auth", new Authenticator.Auth(User.of(email)));
        session.setAttribute("userData", data);
        return session;
    }
}