package dk.utd.fordel.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.services.StudyLineManager;
import dk.utd.fordel.services.UserManager;
import dk.utd.fordel.services.Authenticator;
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

import java.util.List;

class StudyLineControllerRightsTest {

    private FakeStudyLineRepository studyLineRepository;
    private FakeUnitOfWork unitOfWork;
    private FakeUserRepository userRepository;
    private FakeProjectRepository projectRepository;
    private RightsService rightsService;
    private StudyLineManager studyLineManager;
    private UserManager userManager;
    private ProjectManager projectManager;
    private StudyLineController controller;

    @BeforeEach
    void setUp() {
        studyLineRepository = new FakeStudyLineRepository();
        unitOfWork = new FakeUnitOfWork();
        userRepository = new FakeUserRepository();
        projectRepository = new FakeProjectRepository();
        rightsService = new RightsService();
        studyLineManager = new StudyLineManager(studyLineRepository);
        userManager = new UserManager(userRepository);
        projectManager = new ProjectManager(projectRepository);
        controller = new StudyLineController(rightsService, studyLineManager, unitOfWork, userManager, projectManager);
    }

    @Test
    void study_leader_can_open_the_manage_study_line_page() throws Exception {
        MockHttpSession session = sessionWith(new User.Data("Leader", List.of(Roles.STUDY_LEADER), null, null),
                "leader@fordel.dk");
        Model model = new ExtendedModelMap();

        studyLineRepository.canManageByStudyLineId.put(4, true);
        userRepository.students.clear();
        userRepository.leaders.clear();

        String view = controller.getStudyLineMange(4, session, model);

        assertEquals("manage-study-line", view);
        assertEquals(4, model.asMap().get("studyLineId"));
        assertEquals(1, unitOfWork.beginCount);
        assertEquals(1, unitOfWork.commitCount);
    }

    @Test
    void student_is_denied_access_to_manage_study_line() throws Exception {
        MockHttpSession session = sessionWith(new User.Data("Student", List.of(Roles.STUDENT), null, null),
                "student@fordel.dk");

        String view = controller.getStudyLineMange(4, session, new ExtendedModelMap());

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