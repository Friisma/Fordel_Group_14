package dk.utd.fordel.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dk.utd.fordel.domain.Email;
import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.services.Authenticator;
import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.services.StudyLineManager;
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

import java.util.List;

@ExtendWith(MockitoExtension.class)
class UserControllerRightsTest {

    @Mock
    private UserManager userManager;

    @Mock
    private ProjectManager projectManager;

    @Mock
    private StudyLineManager studyLineManager;

    @Mock
    private UnitOfWork unitOfWork;

    @Mock
    private UnitOfWork activeUnitOfWork;

    private RightsService rightsService;
    private UserController controller;

    @BeforeEach
    void setUp() throws Exception {
        rightsService = new RightsService();
        controller = new UserController(userManager, projectManager, rightsService, unitOfWork, studyLineManager);
        lenient().when(unitOfWork.begin()).thenReturn(activeUnitOfWork);
    }

    @Test
    void administrator_can_open_admin_page() throws Exception {
        MockHttpSession session = sessionWith(new User.Data("Admin", List.of(Roles.ADMINISTRATOR), null, null),
                "admin@fordel.dk");
        Model model = new ExtendedModelMap();

        when(userManager.getAllUsers()).thenReturn(List.of());
        when(studyLineManager.getAllStudyLines()).thenReturn(List.of("Software Technology"));

        String view = controller.getAllUsers(model, session);

        assertEquals("adminpage", view);
        assertEquals("Admin Dashboard", model.asMap().get("title"));
        verify(unitOfWork).begin();
        verify(userManager).getAllUsers();
        verify(studyLineManager).getAllStudyLines();
    }

    @Test
    void non_admin_is_blocked_from_deleting_users() throws Exception {
        MockHttpSession session = sessionWith(new User.Data("Student", List.of(Roles.STUDENT), null, null),
                "student@fordel.dk");

        String view = controller.deleteUser(session, "victim@fordel.dk");

        assertEquals("redirect:/forbidden", view);
        verifyNoInteractions(unitOfWork);
        verify(userManager, never()).deleteUser(any());
    }

    @Test
    void administrator_can_delete_users() throws Exception {
        MockHttpSession session = sessionWith(new User.Data("Admin", List.of(Roles.ADMINISTRATOR), null, null),
                "admin@fordel.dk");

        String view = controller.deleteUser(session, "victim@fordel.dk");

        assertEquals("redirect:/adminpage", view);
        verify(unitOfWork).begin();
        verify(userManager).deleteUser(eq(user("victim@fordel.dk")));
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