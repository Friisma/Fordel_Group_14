package dk.utd.fordel.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.services.StudyLineManager;
import dk.utd.fordel.services.UserManager;
import dk.utd.fordel.services.Authenticator;
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

import java.util.List;

@ExtendWith(MockitoExtension.class)
class StudyLineControllerRightsTest {

    @Mock
    private StudyLineManager studyLineManager;

    @Mock
    private UnitOfWork unitOfWork;

    @Mock
    private UnitOfWork activeUnitOfWork;

    @Mock
    private UserManager userManager;

    @Mock
    private ProjectManager projectManager;

    private RightsService rightsService;
    private StudyLineController controller;

    @BeforeEach
    void setUp() throws Exception {
        rightsService = new RightsService();
        controller = new StudyLineController(rightsService, studyLineManager, unitOfWork, userManager, projectManager);
        lenient().when(unitOfWork.begin()).thenReturn(activeUnitOfWork);
    }

    @Test
    void study_leader_can_open_the_manage_study_line_page() throws Exception {
        MockHttpSession session = sessionWith(new User.Data("Leader", List.of(Roles.STUDY_LEADER), null, null),
                "leader@fordel.dk");
        Model model = new ExtendedModelMap();

        when(studyLineManager.canManageStudyLine(any(), eq(4))).thenReturn(true);
        when(userManager.getAllStudentsRelatedToStudyLine(4)).thenReturn(List.of());
        when(userManager.getAllLeadersRelatedToStudyLine(4)).thenReturn(List.of());

        String view = controller.getStudyLineMange(4, session, model);

        assertEquals("manage-study-line", view);
        assertEquals(4, model.asMap().get("studyLineId"));
        verify(unitOfWork).begin();
        verify(studyLineManager).canManageStudyLine(any(), eq(4));
    }

    @Test
    void student_is_denied_access_to_manage_study_line() throws Exception {
        MockHttpSession session = sessionWith(new User.Data("Student", List.of(Roles.STUDENT), null, null),
                "student@fordel.dk");

        String view = controller.getStudyLineMange(4, session, new ExtendedModelMap());

        assertEquals("redirect:/forbidden", view);
        verifyNoInteractions(unitOfWork);
    }

    private static MockHttpSession sessionWith(User.Data data, String email) throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("auth", new Authenticator.Auth(User.of(email)));
        session.setAttribute("userData", data);
        return session;
    }
}