package dk.utd.fordel.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dk.utd.fordel.domain.AdvisorInvitation;
import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.domain.StudentInvitation;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.services.Authenticator;
import dk.utd.fordel.services.InvitationManager;
import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.services.StudentProgressService;
import dk.utd.fordel.services.UserManager;
import dk.utd.fordel.services.rights.RightsService;
import dk.utd.fordel.testsupport.TestFakes.FakeInvitationRepository;
import dk.utd.fordel.testsupport.TestFakes.FakeProjectRepository;
import dk.utd.fordel.testsupport.TestFakes.FakeUnitOfWork;
import dk.utd.fordel.testsupport.TestFakes.FakeUserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class HomepageControllerInvitationTests {

    private RightsService rightsService;
    private FakeProjectRepository projectRepository;
    private FakeInvitationRepository invitationRepository;
    private FakeUserRepository userRepository;
    private FakeUnitOfWork unitOfWork;
    private HomepageController controller;

    @BeforeEach
    void setUp() {
        rightsService = new RightsService();
        projectRepository = new FakeProjectRepository();
        invitationRepository = new FakeInvitationRepository();
        userRepository = new FakeUserRepository();
        unitOfWork = new FakeUnitOfWork();
        controller = new HomepageController(
                rightsService,
                new ProjectManager(projectRepository),
                new InvitationManager(invitationRepository),
                new UserManager(userRepository),
                new StudentProgressService(),
                unitOfWork);
    }

    private static MockHttpSession advisorSession(String email) throws dk.utd.fordel.domain.Email.InvalidEmail {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("auth", new Authenticator.Auth(User.of(email)));
        session.setAttribute("userData", new User.Data("Advisor", List.of(Roles.ADVISOR), null, null));
        return session;
    }

    private static MockHttpSession studentSession(String email) throws dk.utd.fordel.domain.Email.InvalidEmail {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("auth", new Authenticator.Auth(User.of(email)));
        session.setAttribute("userData", new User.Data("Student", List.of(Roles.STUDENT), null, null));
        return session;
    }

    @Test
    void accept_advisor_invitation_updates_status_and_redirects_home() throws Exception {
        MockHttpSession session = advisorSession("advisor@fordel.dk");
        var flash = new RedirectAttributesModelMap();
        AdvisorInvitation invitation = new AdvisorInvitation(7, 44);
        invitationRepository.advisorInvitesReceived.add(invitation);

        String view = controller.acceptInvitation(44, session, flash);

        assertEquals("redirect:/", view);
        assertTrue(flash.getFlashAttributes().containsKey("message"));
        assertEquals(1, invitationRepository.lastUpdatedAdvisorInvitation.getStatus().ordinal());
    }

    @Test
    void decline_advisor_invitation_updates_status_and_redirects_home() throws Exception {
        MockHttpSession session = advisorSession("advisor@fordel.dk");
        var flash = new RedirectAttributesModelMap();
        AdvisorInvitation invitation = new AdvisorInvitation(7, 45);
        invitationRepository.advisorInvitesReceived.add(invitation);

        String view = controller.declineInvitation(45, session, flash);

        assertEquals("redirect:/", view);
        assertTrue(flash.getFlashAttributes().containsKey("message"));
        assertEquals(2, invitationRepository.lastUpdatedAdvisorInvitation.getStatus().ordinal());
    }

    @Test
    void accept_advisor_invitation_rejects_already_handled_invitation() throws Exception {
        MockHttpSession session = advisorSession("advisor@fordel.dk");
        var flash = new RedirectAttributesModelMap();
        AdvisorInvitation invitation = new AdvisorInvitation(7, 46);
        invitation.accept();
        invitationRepository.advisorInvitesReceived.add(invitation);

        String view = controller.acceptInvitation(46, session, flash);

        assertEquals("redirect:/", view);
        assertTrue(flash.getFlashAttributes().containsKey("error"));
    }

    @Test
    void accept_student_invitation_updates_status_and_redirects_home() throws Exception {
        MockHttpSession session = studentSession("student@fordel.dk");
        var flash = new RedirectAttributesModelMap();
        StudentInvitation invitation = new StudentInvitation(9, 55);
        invitationRepository.studentInvitesReceived.add(invitation);

        String view = controller.acceptStudentInvitation(55, session, flash);

        assertEquals("redirect:/", view);
        assertTrue(flash.getFlashAttributes().containsKey("message"));
        assertEquals(1, invitationRepository.lastUpdatedStudentInvitation.getStatus().ordinal());
    }

    @Test
    void decline_student_invitation_updates_status_and_redirects_home() throws Exception {
        MockHttpSession session = studentSession("student@fordel.dk");
        var flash = new RedirectAttributesModelMap();
        StudentInvitation invitation = new StudentInvitation(9, 56);
        invitationRepository.studentInvitesReceived.add(invitation);

        String view = controller.declineStudentInvitation(56, session, flash);

        assertEquals("redirect:/", view);
        assertTrue(flash.getFlashAttributes().containsKey("message"));
        assertEquals(2, invitationRepository.lastUpdatedStudentInvitation.getStatus().ordinal());
    }

    @Test
    void accept_student_invitation_rejects_already_handled_invitation() throws Exception {
        MockHttpSession session = studentSession("student@fordel.dk");
        var flash = new RedirectAttributesModelMap();
        StudentInvitation invitation = new StudentInvitation(9, 57);
        invitation.accept();
        invitationRepository.studentInvitesReceived.add(invitation);

        String view = controller.acceptStudentInvitation(57, session, flash);

        assertEquals("redirect:/", view);
        assertTrue(flash.getFlashAttributes().containsKey("error"));
    }

    @Test
    void advisor_invitation_is_forbidden_for_non_advisor() throws Exception {
        MockHttpSession session = studentSession("student@fordel.dk");
        var flash = new RedirectAttributesModelMap();

        String view = controller.acceptInvitation(60, session, flash);

        assertEquals("redirect:/forbidden", view);
    }

    @Test
    void student_invitation_is_forbidden_for_non_student() throws Exception {
        MockHttpSession session = advisorSession("advisor@fordel.dk");
        var flash = new RedirectAttributesModelMap();

        String view = controller.acceptStudentInvitation(61, session, flash);

        assertEquals("redirect:/forbidden", view);
    }
}