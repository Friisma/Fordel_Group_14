package dk.utd.fordel.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dk.utd.fordel.domain.AdvisorInvitation;
import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.domain.StudentInvitation;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.services.InvitationManager;
import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.services.StudentProgressService;
import dk.utd.fordel.services.UserManager;
import dk.utd.fordel.services.rights.RightsContext;
import dk.utd.fordel.services.rights.RightsService;
import dk.utd.fordel.utils.UnitOfWork;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

@ExtendWith(MockitoExtension.class)
class HomepageControllerInvitationTests {

    @Mock
    private RightsService rightsService;

    @Mock
    private ProjectManager projectManager;

    @Mock
    private InvitationManager invitationManager;

    @Mock
    private UserManager userManager;

    @Mock
    private StudentProgressService studentProgressService;

    @Mock
    private UnitOfWork unitOfWork;

    @Mock
    private UnitOfWork activeUnitOfWork;

    private HomepageController controller;

    @BeforeEach
    void setUp() throws Exception {
        controller = new HomepageController(rightsService, projectManager, invitationManager, userManager,
                studentProgressService, unitOfWork);
        lenient().when(unitOfWork.begin()).thenReturn(activeUnitOfWork);
    }

    private static MockHttpSession advisorSession(String email) throws dk.utd.fordel.domain.Email.InvalidEmail {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("auth", new dk.utd.fordel.services.Authenticator.Auth(User.of(email)));
        session.setAttribute("userData", new User.Data("Advisor", List.of(Roles.ADVISOR), null, null));
        return session;
    }

    private static MockHttpSession studentSession(String email) throws dk.utd.fordel.domain.Email.InvalidEmail {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("auth", new dk.utd.fordel.services.Authenticator.Auth(User.of(email)));
        session.setAttribute("userData", new User.Data("Student", List.of(Roles.STUDENT), null, null));
        return session;
    }

    private static RightsContext rightsWith(Roles... roles) {
        return new RightsService().from(new User.Data("Name", List.of(roles), null, null));
    }

    @Test
    void accept_advisor_invitation_updates_status_and_redirects_home() throws Exception {
        MockHttpSession session = advisorSession("advisor@fordel.dk");
        RedirectAttributes flash = new RedirectAttributesModelMap();
        AdvisorInvitation invitation = new AdvisorInvitation(7, 44);

        when(rightsService.from(any())).thenReturn(rightsWith(Roles.ADVISOR));
        when(invitationManager.getAllInvitesReceived(any())).thenReturn(List.of(invitation));

        String view = controller.acceptInvitation(44, session, flash);

        assertEquals("redirect:/", view);
        assertTrue(flash.getFlashAttributes().containsKey("message"));
        verify(invitationManager).updateInvitation(invitation);
    }

    @Test
    void decline_advisor_invitation_updates_status_and_redirects_home() throws Exception {
        MockHttpSession session = advisorSession("advisor@fordel.dk");
        RedirectAttributes flash = new RedirectAttributesModelMap();
        AdvisorInvitation invitation = new AdvisorInvitation(7, 45);

        when(rightsService.from(any())).thenReturn(rightsWith(Roles.ADVISOR));
        when(invitationManager.getAllInvitesReceived(any())).thenReturn(List.of(invitation));

        String view = controller.declineInvitation(45, session, flash);

        assertEquals("redirect:/", view);
        assertTrue(flash.getFlashAttributes().containsKey("message"));
        verify(invitationManager).updateInvitation(invitation);
    }

    @Test
    void accept_advisor_invitation_rejects_already_handled_invitation() throws Exception {
        MockHttpSession session = advisorSession("advisor@fordel.dk");
        RedirectAttributes flash = new RedirectAttributesModelMap();
        AdvisorInvitation invitation = new AdvisorInvitation(7, 46);
        invitation.accept();

        when(rightsService.from(any())).thenReturn(rightsWith(Roles.ADVISOR));
        when(invitationManager.getAllInvitesReceived(any())).thenReturn(List.of(invitation));

        String view = controller.acceptInvitation(46, session, flash);

        assertEquals("redirect:/", view);
        assertTrue(flash.getFlashAttributes().containsKey("error"));
        verify(invitationManager, never()).updateInvitation(any());
    }

    @Test
    void accept_student_invitation_updates_status_and_redirects_home() throws Exception {
        MockHttpSession session = studentSession("student@fordel.dk");
        RedirectAttributes flash = new RedirectAttributesModelMap();
        StudentInvitation invitation = new StudentInvitation(9, 55);

        when(rightsService.from(any())).thenReturn(rightsWith(Roles.STUDENT));
        when(invitationManager.getAllStudentInvitesReceived(any())).thenReturn(List.of(invitation));

        String view = controller.acceptStudentInvitation(55, session, flash);

        assertEquals("redirect:/", view);
        assertTrue(flash.getFlashAttributes().containsKey("message"));
        verify(invitationManager).updateStudentInvitation(invitation);
    }

    @Test
    void decline_student_invitation_updates_status_and_redirects_home() throws Exception {
        MockHttpSession session = studentSession("student@fordel.dk");
        RedirectAttributes flash = new RedirectAttributesModelMap();
        StudentInvitation invitation = new StudentInvitation(9, 56);

        when(rightsService.from(any())).thenReturn(rightsWith(Roles.STUDENT));
        when(invitationManager.getAllStudentInvitesReceived(any())).thenReturn(List.of(invitation));

        String view = controller.declineStudentInvitation(56, session, flash);

        assertEquals("redirect:/", view);
        assertTrue(flash.getFlashAttributes().containsKey("message"));
        verify(invitationManager).updateStudentInvitation(invitation);
    }

    @Test
    void accept_student_invitation_rejects_already_handled_invitation() throws Exception {
        MockHttpSession session = studentSession("student@fordel.dk");
        RedirectAttributes flash = new RedirectAttributesModelMap();
        StudentInvitation invitation = new StudentInvitation(9, 57);
        invitation.accept();

        when(rightsService.from(any())).thenReturn(rightsWith(Roles.STUDENT));
        when(invitationManager.getAllStudentInvitesReceived(any())).thenReturn(List.of(invitation));

        String view = controller.acceptStudentInvitation(57, session, flash);

        assertEquals("redirect:/", view);
        assertTrue(flash.getFlashAttributes().containsKey("error"));
        verify(invitationManager, never()).updateStudentInvitation(any());
    }

    @Test
    void advisor_invitation_is_forbidden_for_non_advisor() throws Exception {
        MockHttpSession session = studentSession("student@fordel.dk");
        RedirectAttributes flash = new RedirectAttributesModelMap();

        when(rightsService.from(any())).thenReturn(rightsWith(Roles.STUDENT));

        String view = controller.acceptInvitation(60, session, flash);

        assertEquals("redirect:/forbidden", view);
        verify(invitationManager, never()).updateInvitation(any());
    }

    @Test
    void student_invitation_is_forbidden_for_non_student() throws Exception {
        MockHttpSession session = advisorSession("advisor@fordel.dk");
        RedirectAttributes flash = new RedirectAttributesModelMap();

        when(rightsService.from(any())).thenReturn(rightsWith(Roles.ADVISOR));

        String view = controller.acceptStudentInvitation(61, session, flash);

        assertEquals("redirect:/forbidden", view);
        verify(invitationManager, never()).updateStudentInvitation(any());
    }
}
