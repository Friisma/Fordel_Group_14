package dk.utd.fordel.view;

import dk.utd.fordel.domain.Email;
import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.domain.UserDoesNotExist;
import dk.utd.fordel.services.UserManager;
import dk.utd.fordel.services.StudyLineManager;
import dk.utd.fordel.services.rights.RightsContext;
import dk.utd.fordel.services.rights.RightsService;
import org.slf4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

import dk.utd.fordel.utils.Log;
import dk.utd.fordel.utils.UnitOfWork;
import dk.utd.fordel.utils.UnitOfWork.UnitOfWorkException;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserController {
    private static final Logger logger = Log.getLogger(UserController.class);

    private final UserManager userManager;
    private final RightsService rightsService;
    private final UnitOfWork unitOfWork;
    private final StudyLineManager studyLineManager;

    public UserController(UserManager userManager, RightsService rightsService, UnitOfWork unitOfWork, StudyLineManager studyLineManager) {
        this.userManager = userManager;
        this.rightsService = rightsService;
        this.unitOfWork = unitOfWork;
        this.studyLineManager = studyLineManager;
    }

    @GetMapping("/adminpage")
    public String getAllUsers(Model model, HttpSession session) {
        if (session.getAttribute("auth") == null) {
            return "redirect:/login";
        }

        Object userData = session.getAttribute("userData");
        RightsContext rights = rightsService.from(userData instanceof User.Data data ? data : null);
        if (!rightsService.canViewAdminPage(rights)) {
            return "redirect:/forbidden";
        }

        try (var uow = unitOfWork.begin()) {
            var usersList = userManager.getAllUsers();
            var studyLines = studyLineManager.getAllStudyLines();

            model.addAttribute("title", "Admin Dashboard");
            model.addAttribute("users", usersList);
            model.addAttribute("studyLines", studyLines);
            uow.commit();
        } catch (UnitOfWorkException e) {
            throw Log.giveUp(logger, e);
        }
        return "adminpage";
    }

    @DeleteMapping("/adminpage/remove-role")
    public String removeRole(HttpSession session,
                            @RequestParam("email") String email,
                            @RequestParam("role") String roleString) {
        if (session.getAttribute("auth") == null) {
            return "redirect:/login";
        }

        try (var uow = unitOfWork.begin()) {

            User user = User.of(email);
            Roles roleToGrant = Roles.valueOf(roleString);

            userManager.removeRole(user, roleToGrant);

            uow.commit();

        } catch (Email.InvalidEmail e) {
            throw Log.giveUp(logger, e, "Invalid user");
        } catch (UnitOfWorkException e) {
            throw Log.giveUp(logger, e, "Could not grant user role");
        }

        return "redirect:/adminpage";
    }

    @PostMapping("/adminpage/assign-study-line")
    public String assignStudyLine(HttpSession session,
                                  @RequestParam("email") String email,
                                  @RequestParam("studyLine") String studyLineName) {
        if (session.getAttribute("auth") == null) {
            return "redirect:/login";
        }

        Object userData = session.getAttribute("userData");
        RightsContext rights = rightsService.from(userData instanceof User.Data data ? data : null);
        if (!rightsService.canViewAdminPage(rights)) {
            return "redirect:/forbidden";
        }

        try (var uow = unitOfWork.begin()) {
            User user = User.of(email);
            userManager.assignStudyLine(user, studyLineName);
            uow.commit();
        } catch (Email.InvalidEmail e) {
            throw Log.giveUp(logger, e, "Invalid user");
        } catch (UnitOfWorkException e) {
            throw Log.giveUp(logger, e, "Could not assign study line");
        }

        return "redirect:/adminpage";
    }

    @PostMapping("/adminpage/grant-role")
    public String grantRole(HttpSession session,
            @RequestParam("email") String email,
            @RequestParam("role") String roleString) {
        if (session.getAttribute("auth") == null) {
            return "redirect:/login";
        }

        Object userData = session.getAttribute("userData");
        RightsContext rights = rightsService.from(userData instanceof User.Data data ? data : null);
        if (!rightsService.canGrantRoles(rights)) {
            return "redirect:/forbidden";
        }

        try (var uow = unitOfWork.begin()) {

            User user = User.of(email);
            Roles roleToGrant = Roles.valueOf(roleString);

            userManager.grantRole(user, roleToGrant);

            uow.commit();

        } catch (Email.InvalidEmail e) {
            throw Log.giveUp(logger, e, "Invalid user");
        } catch (UnitOfWorkException e) {
            throw Log.giveUp(logger, e, "Could not grant user role");
        }

        return "redirect:/adminpage";
    }

    @PostMapping("/user/update-username")
    public String updateUsername(HttpSession session,
            @RequestParam("name") String name) {
        Object authObj = session.getAttribute("auth");
        if (authObj == null) {
            return "redirect:/login";
        }

        if (!(authObj instanceof dk.utd.fordel.services.Authenticator.Auth auth)) {
            return "redirect:/login";
        }

        if (name == null || name.isBlank()) {
            return "redirect:/";
        }

        User user = auth.user();
        User.Data currentData = (User.Data) session.getAttribute("userData");
        User.Data updatedData = currentData == null
                ? new User.Data(name, java.util.List.of(), null, null)
                : new User.Data(name, currentData.roles(), currentData.studyLine(), currentData.studyStart());

        try (var uow = unitOfWork.begin()) {
            userManager.updateUser(user, updatedData);
            uow.commit();
        } catch (UserDoesNotExist e) {
            throw Log.giveUp(logger, e, "Could not update username");
        } catch (UnitOfWorkException e) {
            throw Log.giveUp(logger, e, "Could not update username");
        }

        session.setAttribute("userData", updatedData);
        return "redirect:/";
    }
}
