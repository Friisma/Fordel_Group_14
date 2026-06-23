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
import dk.utd.fordel.domain.ProjectAlreadyFinished;
import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.services.InvitationManager;
import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.services.UserManager;
import dk.utd.fordel.services.rights.RightsService;
import dk.utd.fordel.utils.UnitOfWork;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

@ExtendWith(MockitoExtension.class)
class ProjectControllerPostTests {

    @Mock
    private RightsService rightsService;

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

    private ProjectController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new ProjectController(rightsService, projectManager, unitOfWork, userManager,
                invitationManager);
        lenient().when(unitOfWork.begin()).thenReturn(activeUnitOfWork);
    }

    private static MockHttpSession sessionWith(String email, Roles... roles)
            throws dk.utd.fordel.domain.Email.InvalidEmail {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("auth", new dk.utd.fordel.services.Authenticator.Auth(User.of(email)));
        session.setAttribute("userData", new User.Data("Name", List.of(roles), null, null));
        return session;
    }

    @Test
    void post_update_project_succeeds() throws Exception {
        MockHttpSession session = sessionWith("user@fordel.dk", Roles.STUDENT);
        Model model = new ExtendedModelMap();
        RedirectAttributes flash = new RedirectAttributesModelMap();

        when(rightsService.canEditPage(any())).thenReturn(true);

        String view = controller.updateProject(5, "New title", "New description", session, model, flash);

        assertEquals("redirect:/project/5", view);
        verify(projectManager).update(any(Project.class));
        assertTrue(flash.getFlashAttributes().containsKey("message"));
    }

    @Test
    void post_update_project_rejects_empty_fields() throws Exception {
        MockHttpSession session = sessionWith("user@fordel.dk", Roles.STUDENT);
        Model model = new ExtendedModelMap();
        RedirectAttributes flash = new RedirectAttributesModelMap();

        when(rightsService.canEditPage(any())).thenReturn(true);

        String view = controller.updateProject(7, "", "", session, model, flash);

        assertEquals("redirect:/project/7", view);
        assertTrue(flash.getFlashAttributes().containsKey("error"));
        verify(projectManager, never()).update(any(Project.class));
    }

    @Test
    void post_start_project_succeeds() throws Exception {
        MockHttpSession session = sessionWith("advisor@fordel.dk", Roles.ADVISOR);
        RedirectAttributes flash = new RedirectAttributesModelMap();

        when(rightsService.canStartProject(any())).thenReturn(true);

        String view = controller.startProject(session, 11, flash);

        assertEquals("redirect:/project/11", view);
        verify(projectManager).startProject(11);
    }

    @Test
    void post_start_project_handles_already_finished() throws Exception {
        MockHttpSession session = sessionWith("advisor@fordel.dk", Roles.ADVISOR);
        RedirectAttributes flash = new RedirectAttributesModelMap();

        when(rightsService.canStartProject(any())).thenReturn(true);
        org.mockito.Mockito.doThrow(new ProjectAlreadyFinished("done")).when(projectManager).startProject(12);

        String view = controller.startProject(session, 12, flash);

        assertEquals("redirect:/project/12", view);
        assertTrue(flash.getFlashAttributes().containsKey("error"));
    }

    @Test
    void post_invite_advisor_succeeds() throws Exception {
        MockHttpSession session = sessionWith("creator@fordel.dk", Roles.ADVISOR);
        RedirectAttributes flash = new RedirectAttributesModelMap();

        when(rightsService.canViewProject(any())).thenReturn(true);
        when(projectManager.canViewProject(any(), eq(21))).thenReturn(true);
        Project p = new Project("p", "d");
        p.setProjectID(21);
        when(projectManager.getAllProjectsRelatedToUser(any())).thenReturn(List.of(p));
        when(invitationManager.getInvitesForProject(21)).thenReturn(List.of());
        when(userManager.checkMaxWorkLoad(any())).thenReturn(true);

        String view = controller.inviteAdvisor(21, "advisor@fordel.dk", session, flash);

        assertEquals("redirect:/project/21", view);
        verify(invitationManager).createInvitaion(any(User.class), any(Project.class));
        assertTrue(flash.getFlashAttributes().containsKey("message"));
    }

    @Test
    void post_invite_advisor_rejects_duplicate_invite() throws Exception {
        MockHttpSession session = sessionWith("creator@fordel.dk", Roles.ADVISOR);
        RedirectAttributes flash = new RedirectAttributesModelMap();

        when(rightsService.canViewProject(any())).thenReturn(true);
        when(projectManager.canViewProject(any(), eq(22))).thenReturn(true);
        Project p = new Project("p", "d");
        p.setProjectID(22);
        when(projectManager.getAllProjectsRelatedToUser(any())).thenReturn(List.of(p));
        when(invitationManager.getInvitesForProject(22)).thenReturn(
                List.of(new InvitationManager.InviteView("n", "existing@fordel.dk", "pending", "s")));

        String view = controller.inviteAdvisor(22, "existing@fordel.dk", session, flash);

        assertEquals("redirect:/project/22", view);
        assertTrue(flash.getFlashAttributes().containsKey("error"));
        verify(invitationManager, never()).createInvitaion(any(User.class), any(Project.class));
    }

    @Test
    void post_invite_student_succeeds() throws Exception {
        MockHttpSession session = sessionWith("creator@fordel.dk", Roles.ADVISOR);
        RedirectAttributes flash = new RedirectAttributesModelMap();

        when(rightsService.canViewProject(any())).thenReturn(true);
        when(projectManager.canViewProject(any(User.class), eq(31))).thenReturn(true).thenReturn(false);
        Project p = new Project("p", "d");
        p.setProjectID(31);
        when(projectManager.getAllProjectsRelatedToUser(any())).thenReturn(List.of(p));
        when(invitationManager.getStudentInvitesForProject(31)).thenReturn(List.of());
        when(userManager.getUserRoles(any(User.class))).thenReturn(List.of(Roles.STUDENT));

        String view = controller.inviteStudent(31, "student@fordel.dk", session, flash);

        assertEquals("redirect:/project/31", view);
        verify(invitationManager).createStudentInvitation(any(User.class), any(Project.class));
        assertTrue(flash.getFlashAttributes().containsKey("message"));
    }

    @Test
    void post_kick_member_succeeds_for_creator() throws Exception {
        MockHttpSession session = sessionWith("creator@fordel.dk", Roles.STUDENT);
        RedirectAttributes flash = new RedirectAttributesModelMap();

        when(rightsService.canViewProject(any())).thenReturn(true);
        lenient().when(projectManager.canViewProject(any(), eq(41))).thenReturn(true);
        lenient().when(projectManager.getProjectCreatorEmail(41)).thenReturn("creator@fordel.dk");
        lenient().when(projectManager.canViewProject(any(User.class), eq(41))).thenReturn(true);

        String view = controller.kickMember(41, "member@fordel.dk", session, flash);

        assertEquals("redirect:/project/41", view);
        verify(projectManager).kickMemberFromProject(eq(41), any(User.class));
        assertTrue(flash.getFlashAttributes().containsKey("message"));
    }

    @Test
    void post_kick_member_denied_for_non_creator() throws Exception {
        MockHttpSession session = sessionWith("other@fordel.dk", Roles.STUDENT);
        RedirectAttributes flash = new RedirectAttributesModelMap();

        when(rightsService.canViewProject(any())).thenReturn(true);
        when(projectManager.canViewProject(any(), eq(42))).thenReturn(true);
        when(projectManager.getProjectCreatorEmail(42)).thenReturn("creator@fordel.dk");

        String view = controller.kickMember(42, "member@fordel.dk", session, flash);

        assertEquals("redirect:/project/42", view);
        assertTrue(flash.getFlashAttributes().containsKey("error"));
        verify(projectManager, never()).kickMemberFromProject(eq(42), any(User.class));
    }

    @Test
    void post_assign_advisor_succeeds() throws Exception {
        MockHttpSession session = sessionWith("admin@fordel.dk", Roles.ADVISOR);
        Model model = new ExtendedModelMap();

        when(rightsService.canViewAllProjects(any())).thenReturn(true);
        when(userManager.checkMaxWorkLoad(any())).thenReturn(true);

        String view = controller.assignAdvisor(session, model, "p", "d", 51);

        assertEquals("redirect:/", view);
        verify(projectManager).assignAdvisor(any(Project.class), any(User.class));
    }

    @Test
    void post_change_max_work_load_succeeds() throws Exception {
        MockHttpSession session = sessionWith("advisor@fordel.dk", Roles.ADVISOR);
        Model model = new ExtendedModelMap();

        when(rightsService.canViewAdvisorDashboard(any())).thenReturn(true);
        when(projectManager.getCountOfAdvisedProjects(any())).thenReturn(0);

        String view = controller.changeMaxWorkLoad(model, session, 5);

        assertEquals("advisor-dashboard", view);
        verify(userManager).updateMaxWorkLoad(any(User.class), eq(5));
    }
}
