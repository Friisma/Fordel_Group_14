package dk.utd.fordel.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dk.utd.fordel.domain.Email;
import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.services.Authenticator;
import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.services.StudyLineManager;
import dk.utd.fordel.services.UserManager;
import dk.utd.fordel.services.rights.RightsService;
import dk.utd.fordel.testsupport.TestFakes.FakeProjectRepository;
import dk.utd.fordel.testsupport.TestFakes.FakeStudyLineRepository;
import dk.utd.fordel.testsupport.TestFakes.FakeUnitOfWork;
import dk.utd.fordel.testsupport.TestFakes.FakeUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.time.LocalDate;
import java.util.List;

class UserControllerRightsTest {

    private FakeUserRepository userRepository;
    private FakeProjectRepository projectRepository;
    private FakeStudyLineRepository studyLineRepository;
    private FakeUnitOfWork unitOfWork;
    private RightsService rightsService;
    private UserManager userManager;
    private ProjectManager projectManager;
    private StudyLineManager studyLineManager;
    private UserController controller;

    @BeforeEach
    void setUp() {
        userRepository = new FakeUserRepository();
        projectRepository = new FakeProjectRepository();
        studyLineRepository = new FakeStudyLineRepository();
        unitOfWork = new FakeUnitOfWork();
        rightsService = new RightsService();
        userManager = new UserManager(userRepository);
        projectManager = new ProjectManager(projectRepository);
        studyLineManager = new StudyLineManager(studyLineRepository);
        controller = new UserController(userManager, projectManager, rightsService, unitOfWork, studyLineManager);
    }

    @Test
    void administrator_can_open_admin_page() throws Exception {
        MockHttpSession session = sessionWith(new User.Data("Admin", List.of(Roles.ADMINISTRATOR), null, null),
                "admin@fordel.dk");
        Model model = new ExtendedModelMap();

        userRepository.allUsers.clear();
        studyLineRepository.studyLines.clear();
        studyLineRepository.studyLines.add("Software Technology");

        String view = controller.getAllUsers(model, session);

        assertEquals("adminpage", view);
        assertEquals("Admin Dashboard", model.asMap().get("title"));
        assertEquals(1, unitOfWork.beginCount);
        assertEquals(1, unitOfWork.commitCount);
    }

    @Test
    void non_admin_is_blocked_from_deleting_users() throws Exception {
        MockHttpSession session = sessionWith(new User.Data("Student", List.of(Roles.STUDENT), null, null),
                "student@fordel.dk");

        String view = controller.deleteUser(session, "victim@fordel.dk");

        assertEquals("redirect:/forbidden", view);
        assertEquals(0, unitOfWork.beginCount);
        assertEquals(0, userRepository.deletedUsers.size());
    }

    @Test
    void administrator_can_delete_users() throws Exception {
        MockHttpSession session = sessionWith(new User.Data("Admin", List.of(Roles.ADMINISTRATOR), null, null),
                "admin@fordel.dk");

        String view = controller.deleteUser(session, "victim@fordel.dk");

        assertEquals("redirect:/adminpage", view);
        assertEquals(1, unitOfWork.beginCount);
        assertEquals(1, userRepository.deletedUsers.size());
        assertEquals(user("victim@fordel.dk"), userRepository.deletedUsers.getFirst());
    }

    @Test
    void update_username_succeeds_for_logged_in_user() throws Exception {
        MockHttpSession session = sessionWith(new User.Data("OldName", List.of(Roles.STUDENT), "Software Technology", null), "student@fordel.dk");
        Model model = new ExtendedModelMap();

        String view = controller.updateUsername(session, "NewName");

        assertEquals("redirect:/", view);

        User.Data updatedData = (User.Data) session.getAttribute("userData");
        assertEquals("NewName", updatedData.name());
    }

    @Test
    void update_username_redirects_to_login_if_not_authenticated() throws Exception {
        MockHttpSession session = new MockHttpSession();

        String view = controller.updateUsername(session, "NewName");

        assertEquals("redirect:/login", view);

    }

    @Test
    void update_username_rejects_blank_name() throws Exception {
        MockHttpSession session = sessionWith(new User.Data("OldName", List.of(Roles.STUDENT), null, null), "student@fordel.dk");

        String view = controller.updateUsername(session, "   ");

        assertEquals("redirect:/", view);

    }

    @Test
    void update_study_start_succeeds_for_student() throws Exception {
        MockHttpSession session = sessionWith(new User.Data("Peter", List.of(Roles.STUDENT), "Software Technology", null), "student@fordel.dk");
        LocalDate date = LocalDate.of(2023, 9, 1);

        String view = controller.updateStudyStart(session, date);

        assertEquals("redirect:/", view);

        User.Data updatedData = (User.Data) session.getAttribute("userData");
        assertEquals(date, updatedData.studyStart());
    }

    @Test
    void update_study_start_redirects_to_login_if_not_authenticated() throws Exception {
        MockHttpSession session = new MockHttpSession();

        String view = controller.updateStudyStart(session, LocalDate.of(2023, 9, 1));

        assertEquals("redirect:/login", view);

    }

    @Test
    void update_study_start_blocked_for_non_student() throws Exception {
        MockHttpSession session = sessionWith(new User.Data("Peter", List.of(Roles.ADVISOR), null, null), "advisor@fordel.dk");

        String view = controller.updateStudyStart(session, LocalDate.of(2023, 9, 1));

        assertEquals("redirect:/forbidden", view);

    }

    private static MockHttpSession sessionWith(User.Data data, String email) throws Email.InvalidEmail {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("auth", new Authenticator.Auth(User.of(email)));
        session.setAttribute("userData", data);
        return session;
    }

    private static User user(String email) {
        try {
            return User.of(email);
        } catch (Email.InvalidEmail e) {
            throw new IllegalArgumentException(e);
        }
    }
}