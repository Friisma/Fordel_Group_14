package dk.utd.fordel.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dk.utd.fordel.domain.Project;
import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.services.Authenticator;
import dk.utd.fordel.services.InvitationManager;
import dk.utd.fordel.services.ProjectManager;
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
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class ProjectControllerPostTests {

    private FakeProjectRepository projectRepository;
    private FakeUserRepository userRepository;
    private FakeInvitationRepository invitationRepository;
    private FakeUnitOfWork unitOfWork;
    private RightsService rightsService;
    private ProjectManager projectManager;
    private UserManager userManager;
    private InvitationManager invitationManager;
    private ProjectController controller;

    @BeforeEach
    void setUp() {
        projectRepository = new FakeProjectRepository();
        userRepository = new FakeUserRepository();
        invitationRepository = new FakeInvitationRepository();
        unitOfWork = new FakeUnitOfWork();
        rightsService = new RightsService();
        projectManager = new ProjectManager(projectRepository);
        userManager = new UserManager(userRepository);
        invitationManager = new InvitationManager(invitationRepository);
        controller = new ProjectController(rightsService, projectManager, unitOfWork, userManager, invitationManager);
    }

    private static MockHttpSession sessionWith(String email, Roles... roles)
            throws dk.utd.fordel.domain.Email.InvalidEmail {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("auth", new Authenticator.Auth(User.of(email)));
        session.setAttribute("userData", new User.Data("Name", List.of(roles), null, null));
        return session;
    }

    @Test
    void post_update_project_succeeds() throws Exception {
        MockHttpSession session = sessionWith("user@fordel.dk", Roles.STUDENT);
        Model model = new ExtendedModelMap();
        var flash = new RedirectAttributesModelMap();

        String view = controller.updateProject(5, "New title", "New description", session, model, flash);

        assertEquals("redirect:/project/5", view);
        assertEquals("New title", projectRepository.lastUpdatedProject.getProjectName());
        assertTrue(flash.getFlashAttributes().containsKey("message"));
    }

    @Test
    void post_update_project_rejects_empty_fields() throws Exception {
        MockHttpSession session = sessionWith("user@fordel.dk", Roles.STUDENT);
        Model model = new ExtendedModelMap();
        var flash = new RedirectAttributesModelMap();

        String view = controller.updateProject(7, "", "", session, model, flash);

        assertEquals("redirect:/project/7", view);
        assertTrue(flash.getFlashAttributes().containsKey("error"));
        assertNull(projectRepository.lastUpdatedProject);
    }

    @Test
    void post_start_project_succeeds() throws Exception {
        MockHttpSession session = sessionWith("advisor@fordel.dk", Roles.ADVISOR);
        var flash = new RedirectAttributesModelMap();

        String view = controller.startProject(session, 11, flash);

        assertEquals("redirect:/project/11", view);
        assertEquals(11, projectRepository.lastStartedProjectId);
    }

    @Test
    void post_start_project_handles_already_finished() throws Exception {
        MockHttpSession session = sessionWith("advisor@fordel.dk", Roles.ADVISOR);
        var flash = new RedirectAttributesModelMap();
        projectRepository.projectToStartThatIsFinished = 12;

        String view = controller.startProject(session, 12, flash);

        assertEquals("redirect:/project/12", view);
        assertTrue(flash.getFlashAttributes().containsKey("error"));
    }

    @Test
    void post_invite_advisor_succeeds() throws Exception {
        MockHttpSession session = sessionWith("creator@fordel.dk", Roles.ADVISOR);
        var flash = new RedirectAttributesModelMap();
        Project project = new Project("p", "d");
        project.setProjectID(21);
        projectRepository.projectsByUser.put("creator@fordel.dk", List.of(project));
        projectRepository.canViewByUserAndProject.put("creator@fordel.dk|21", true);
        userRepository.checkMaxWorkLoadResult = true;

        String view = controller.inviteAdvisor(21, "advisor@fordel.dk", session, flash);

        assertEquals("redirect:/project/21", view);
        assertEquals(User.of("advisor@fordel.dk"), invitationRepository.lastCreatedAdvisorInviteUser);
        assertTrue(flash.getFlashAttributes().containsKey("message"));
    }

    @Test
    void post_invite_advisor_rejects_duplicate_invite() throws Exception {
        MockHttpSession session = sessionWith("creator@fordel.dk", Roles.ADVISOR);
        var flash = new RedirectAttributesModelMap();
        Project project = new Project("p", "d");
        project.setProjectID(22);
        projectRepository.projectsByUser.put("creator@fordel.dk", List.of(project));
        projectRepository.canViewByUserAndProject.put("creator@fordel.dk|22", true);
        invitationRepository.invitesForProject
                .add(new InvitationManager.InviteView("n", "existing@fordel.dk", "pending", "s"));

        String view = controller.inviteAdvisor(22, "existing@fordel.dk", session, flash);

        assertEquals("redirect:/project/22", view);
        assertTrue(flash.getFlashAttributes().containsKey("error"));
        assertNull(invitationRepository.lastCreatedAdvisorInviteUser);
    }

    @Test
    void post_invite_advisor_is_forbidden_without_project_view_rights() throws Exception {
        MockHttpSession session = sessionWith("creator@fordel.dk");
        var flash = new RedirectAttributesModelMap();

        String view = controller.inviteAdvisor(23, "advisor@fordel.dk", session, flash);

        assertEquals("redirect:/forbidden", view);
    }

    @Test
    void post_invite_student_succeeds() throws Exception {
        MockHttpSession session = sessionWith("creator@fordel.dk", Roles.ADVISOR);
        var flash = new RedirectAttributesModelMap();
        Project project = new Project("p", "d");
        project.setProjectID(31);
        projectRepository.projectsByUser.put("creator@fordel.dk", List.of(project));
        projectRepository.canViewByUserAndProject.put("creator@fordel.dk|31", true);
        projectRepository.canViewByUserAndProject.put("student@fordel.dk|31", false);
        userRepository.rolesByEmail.put("student@fordel.dk", List.of(Roles.STUDENT));

        String view = controller.inviteStudent(31, "student@fordel.dk", session, flash);

        assertEquals("redirect:/project/31", view);
        assertEquals(User.of("student@fordel.dk"), invitationRepository.lastCreatedStudentInviteUser);
        assertTrue(flash.getFlashAttributes().containsKey("message"));
    }

    @Test
    void post_invite_student_is_forbidden_without_project_view_rights() throws Exception {
        MockHttpSession session = sessionWith("creator@fordel.dk");
        var flash = new RedirectAttributesModelMap();

        String view = controller.inviteStudent(32, "student@fordel.dk", session, flash);

        assertEquals("redirect:/forbidden", view);
    }

    @Test
    void post_kick_member_succeeds_for_creator() throws Exception {
        MockHttpSession session = sessionWith("creator@fordel.dk", Roles.STUDENT);
        var flash = new RedirectAttributesModelMap();
        projectRepository.creatorEmailByProjectId.put(41, "creator@fordel.dk");
        projectRepository.canViewByUserAndProject.put("creator@fordel.dk|41", true);
        projectRepository.canViewByUserAndProject.put("member@fordel.dk|41", true);

        String view = controller.kickMember(41, "member@fordel.dk", session, flash);

        assertEquals("redirect:/project/41", view);
        assertEquals(41, projectRepository.lastKickedProjectId);
        assertEquals(User.of("member@fordel.dk"), projectRepository.lastKickedMember);
        assertTrue(flash.getFlashAttributes().containsKey("message"));
    }

    @Test
    void post_kick_member_denied_for_non_creator() throws Exception {
        MockHttpSession session = sessionWith("other@fordel.dk", Roles.STUDENT);
        var flash = new RedirectAttributesModelMap();
        projectRepository.creatorEmailByProjectId.put(42, "creator@fordel.dk");
        projectRepository.canViewByUserAndProject.put("other@fordel.dk|42", true);

        String view = controller.kickMember(42, "member@fordel.dk", session, flash);

        assertEquals("redirect:/project/42", view);
        assertTrue(flash.getFlashAttributes().containsKey("error"));
    }

    @Test
    void post_assign_advisor_succeeds() throws Exception {
        MockHttpSession session = sessionWith("admin@fordel.dk", Roles.ADVISOR);
        Model model = new ExtendedModelMap();

        String view = controller.assignAdvisor(session, model, "p", "d", 51);

        assertEquals("redirect:/", view);
        assertEquals(51, projectRepository.lastAssignedProject.getProjectID());
        assertEquals(User.of("admin@fordel.dk"), projectRepository.lastAssignedAdvisor);
    }

    @Test
    void post_change_max_work_load_succeeds() throws Exception {
        MockHttpSession session = sessionWith("advisor@fordel.dk", Roles.ADVISOR);
        Model model = new ExtendedModelMap();
        projectRepository.countOfAdvisorProjectsReturn = 0;

        String view = controller.changeMaxWorkLoad(model, session, 5);

        assertEquals("advisor-dashboard", view);
        assertEquals(5, userRepository.lastUpdatedMaxWorkLoad);
        assertEquals(User.of("advisor@fordel.dk"), userRepository.lastUpdatedMaxWorkLoadUser);
    }
}