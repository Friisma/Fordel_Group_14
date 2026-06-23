package dk.utd.fordel.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.services.Authenticator;
import dk.utd.fordel.services.InvitationManager;
import dk.utd.fordel.services.UserManager;
import dk.utd.fordel.services.rights.RightsService;
import dk.utd.fordel.testsupport.TestFakes.FakeInvitationRepository;
import dk.utd.fordel.testsupport.TestFakes.FakeProjectRepository;
import dk.utd.fordel.testsupport.TestFakes.FakeUnitOfWork;
import dk.utd.fordel.testsupport.TestFakes.FakeUserRepository;
import dk.utd.fordel.utils.UnitOfWork;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class ProjectControllerRightsTest {

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

    @Test
    void advisor_can_open_the_advisor_dashboard() throws Exception {
        MockHttpSession session = sessionWith(new User.Data("Advisor", List.of(Roles.ADVISOR), null, null),
                "advisor@fordel.dk");
        Model model = new ExtendedModelMap();

        userRepository.maxWorkLoads.put("advisor@fordel.dk", 3);
        projectRepository.countOfAdvisorProjectsReturn = 1;
        projectRepository.advisorProjects.clear();

        String view = controller.getAllAdviserProjects(model, session);

        assertEquals("advisor-dashboard", view);
        assertEquals(3, model.asMap().get("maxProjectCount"));
        assertEquals(1, model.asMap().get("projectCount"));
        assertEquals(1, unitOfWork.beginCount);
        assertEquals(1, unitOfWork.commitCount);
    }

    @Test
    void student_is_denied_access_to_the_advisor_dashboard() throws Exception {
        MockHttpSession session = sessionWith(new User.Data("Student", List.of(Roles.STUDENT), null, null),
                "student@fordel.dk");

        String view = controller.getAllAdviserProjects(new ExtendedModelMap(), session);

        assertEquals("redirect:/forbidden", view);
        assertEquals(0, unitOfWork.beginCount);
    }

    @Test
    void advisor_can_start_project() throws Exception {
        MockHttpSession session = sessionWith(new User.Data("Advisor", List.of(Roles.ADVISOR), null, null),
                "advisor@fordel.dk");

        String view = controller.startProject(session, 7, new RedirectAttributesModelMap());

        assertEquals("redirect:/project/7", view);
        assertEquals(1, unitOfWork.beginCount);
        assertEquals(7, projectRepository.lastStartedProjectId);
    }

    @Test
    void student_is_denied_starting_a_project() throws Exception {
        MockHttpSession session = sessionWith(new User.Data("Student", List.of(Roles.STUDENT), null, null),
                "student@fordel.dk");

        String view = controller.startProject(session, 7, new RedirectAttributesModelMap());

        assertEquals("redirect:/forbidden", view);
        assertEquals(0, unitOfWork.beginCount);
    }

    private static MockHttpSession sessionWith(User.Data data, String email) throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("auth", new Authenticator.Auth(User.of(email)));
        session.setAttribute("userData", data);
        return session;
    }
}