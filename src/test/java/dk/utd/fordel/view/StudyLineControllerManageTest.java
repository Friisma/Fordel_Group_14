package dk.utd.fordel.view;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

import java.util.List;

class StudyLineControllerManageTest {

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
    void manage_study_line_page_adds_members_and_available_students_to_model() throws Exception {
        MockHttpSession session = sessionWith(new User.Data("Leader", List.of(Roles.STUDY_LEADER), null, null),
                "leader@fordel.dk");
        Model model = new ExtendedModelMap();

        studyLineRepository.canManageByStudyLineId.put(4, true);

        var existingStudent = new UserManager.UserRepository.UserInfo("1", "student1@test.dk", "Student One",
                List.of(Roles.STUDENT), null);
        var availableStudent = new UserManager.UserRepository.UserInfo("2", "student2@test.dk", "Student Two",
                List.of(Roles.STUDENT), null);
        var advisor = new UserManager.UserRepository.UserInfo("3", "advisor@test.dk", "Advisor",
                List.of(Roles.ADVISOR), null);
        var leader = new UserManager.UserRepository.UserInfo("4", "leader@test.dk", "Leader",
                List.of(Roles.STUDY_LEADER), null);

        userRepository.students.add(existingStudent);
        userRepository.advisors.add(advisor);
        userRepository.leaders.add(leader);

        userRepository.allUsers.add(existingStudent);
        userRepository.allUsers.add(availableStudent);
        userRepository.allUsers.add(advisor);
        userRepository.allUsers.add(leader);

        String view = controller.getStudyLineMange(4, session, model);

        assertEquals("manage-study-line", view);
        assertEquals(4, model.asMap().get("studyLineId"));
        assertEquals(List.of(existingStudent), model.asMap().get("studyLineStudents"));
        assertEquals(List.of(advisor), model.asMap().get("studyLineAdvisors"));
        assertEquals(List.of(leader), model.asMap().get("studyLineLeaders"));
        assertEquals(List.of(availableStudent), model.asMap().get("availableStudents"));
        assertEquals(1, unitOfWork.beginCount);
        assertEquals(1, unitOfWork.commitCount);
    }

    private static MockHttpSession sessionWith(User.Data data, String email) throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("auth", new Authenticator.Auth(User.of(email)));
        session.setAttribute("userData", data);
        return session;
    }
}