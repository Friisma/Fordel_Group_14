package dk.utd.fordel.view;

import dk.utd.fordel.domain.Email;
import dk.utd.fordel.domain.Project;
import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.domain.UserDoesNotExist;
import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.services.UserManager;
import dk.utd.fordel.services.StudyLineManager;
import dk.utd.fordel.services.rights.RightsContext;
import dk.utd.fordel.services.rights.RightsService;
import org.slf4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import dk.utd.fordel.utils.Log;
import dk.utd.fordel.utils.UnitOfWork;
import dk.utd.fordel.utils.UnitOfWork.UnitOfWorkException;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
public class UserController {
    private static final Logger logger = Log.getLogger(UserController.class);

    private final UserManager userManager;
    private final ProjectManager projectManager;
    private final RightsService rightsService;
    private final UnitOfWork unitOfWork;
    private final StudyLineManager studyLineManager;

    public UserController(UserManager userManager, ProjectManager projectManager, RightsService rightsService,
            UnitOfWork unitOfWork, StudyLineManager studyLineManager) {
        this.userManager = userManager;
        this.projectManager = projectManager;
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

    @GetMapping("/users")
    public String getUsers(HttpSession session, Model model) {
        if (session.getAttribute("auth") == null) {
            return "redirect:/login";
        }

        Object userData = session.getAttribute("userData");
        RightsContext rights = rightsService.from(userData instanceof User.Data data ? data : null);
        if (!rightsService.canViewStudent(rights)) {
            return "redirect:/forbidden";
        }

        try (var uow = unitOfWork.begin()) {
            var usersList = userManager.getAllUsers().stream()
                    .map(user -> new UserCard(
                            user.id(),
                            user.name(),
                            user.roles(),
                            user.email(),
                            null,
                            false))
                    .toList();

            model.addAttribute("title", "Users");
            model.addAttribute("users", usersList);
            uow.commit();
        } catch (UnitOfWorkException e) {
            throw Log.giveUp(logger, e, "Could not load users");
        }

        return "users";
    }

    @GetMapping("/user/{id}")
    public String getUser(@PathVariable int id, HttpSession session, Model model) {
        if (session.getAttribute("auth") == null) {
            return "redirect:/login";
        }

        Object userData = session.getAttribute("userData");
        RightsContext rights = rightsService.from(userData instanceof User.Data data ? data : null);
        if (!rightsService.canViewStudent(rights)) {
            return "redirect:/forbidden";
        }

        boolean canViewProjects = rightsService.canViewStudentProjects(rights);
        model.addAttribute("projectsVisible", canViewProjects);
        boolean canEditStudent = rightsService.canEditStudent(rights);
        model.addAttribute("canEditStudent", canEditStudent);

        try (var uow = unitOfWork.begin()) {
            var user = userManager.getAllUsers().stream()
                    .filter(userInfo -> String.valueOf(id).equals(userInfo.id()))
                    .findFirst()
                    .orElse(null);

            if (user == null) {
                return "redirect:/error";
            }

            model.addAttribute("title", "User " + id);
            model.addAttribute("userId", id);
            model.addAttribute("userName", user.name());
            model.addAttribute("userEmail", user.email());
            model.addAttribute("userRoles", user.roles());
            model.addAttribute("userStudyLine", user.studyLine());

            if (canEditStudent) {
                model.addAttribute("studyLines", studyLineManager.getAllStudyLines());
            }

            if (canViewProjects) {
                try {
                    var targetUser = User.of(user.email());
                    var userProjects = projectManager.getAllProjectsRelatedToUser(targetUser);
                    model.addAttribute("projects", userProjects.stream().map(UserController::toProjectCard).toList());
                } catch (Project.InvalidProject | Email.InvalidEmail e) {
                    model.addAttribute("projects", List.of());
                }
            }

            uow.commit();
        } catch (UnitOfWorkException e) {
            throw Log.giveUp(logger, e, "Could not load user");
        }

        return "user";
    }

    private static ProjectCard toProjectCard(Project project) {
        return new ProjectCard(
                String.valueOf(project.getProjectID()),
                project.getProjectName(),
                project.getDescription() != null ? project.getDescription() : "",
                project.getStudyLine() != null ? project.getStudyLine() : "",
                project.getStatus().toString());
    }

    @PostMapping("/adminpage/remove-role")
    public String removeRole(HttpSession session,
            @RequestParam("email") String email,
            @RequestParam("role") String roleString) {
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
            @RequestParam("studyLine") String studyLine,
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

            userManager.assignStudyLineRole(user, studyLine, roleToGrant);

            uow.commit();

        } catch (Email.InvalidEmail e) {
            throw Log.giveUp(logger, e, "Invalid user");
        } catch (UnitOfWorkException e) {
            throw Log.giveUp(logger, e, "Could not grant user role");
        }

        return "redirect:/adminpage";
    }

    @PostMapping("/adminpage/grant-admin")
    public String grantRole(HttpSession session,
            @RequestParam("email") String email,
            RedirectAttributes redirectAttributes) {
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
            if (userManager.getUserRoles(user).contains(Roles.ADMINISTRATOR)) {
                redirectAttributes.addFlashAttribute("error", "User is already administrator");
                uow.rollback();
                return "redirect:/adminpage";
            }
            userManager.grantRole(user, Roles.ADMINISTRATOR);

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

    @PostMapping("/user/update-study-start")
    public String updateStudyStart(HttpSession session,
            @RequestParam("studyStart") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate studyStart) {
        Object authObj = session.getAttribute("auth");
        if (authObj == null) {
            return "redirect:/login";
        }

        if (!(authObj instanceof dk.utd.fordel.services.Authenticator.Auth auth)) {
            return "redirect:/login";
        }

        Object userData = session.getAttribute("userData");
        RightsContext rights = rightsService.from(userData instanceof User.Data data ? data : null);
        if (!rightsService.canUpdateStudyStart(rights)) {
            return "redirect:/forbidden";
        }

        if (studyStart == null) {
            return "redirect:/";
        }

        User user = auth.user();
        User.Data currentData = userData instanceof User.Data data ? data : null;
        User.Data updatedData = currentData == null
                ? new User.Data("", java.util.List.of(), null, studyStart)
                : new User.Data(currentData.name(), currentData.roles(), currentData.studyLine(), studyStart);

        try (var uow = unitOfWork.begin()) {
            userManager.updateStudyStart(user, studyStart);
            uow.commit();
        } catch (UserDoesNotExist e) {
            throw Log.giveUp(logger, e, "Could not update study start date");
        } catch (UnitOfWorkException e) {
            throw Log.giveUp(logger, e, "Could not update study start date");
        }

        session.setAttribute("userData", updatedData);
        return "redirect:/";
    }

    @PostMapping("/adminpage/delete-user")
    public String deleteUser(HttpSession session,
            @RequestParam("email") String email) {
        Object authObj = session.getAttribute("auth");
        if (authObj == null) {
            return "redirect:/login";
        }

        Object userData = session.getAttribute("userData");
        RightsContext rights = rightsService.from(userData instanceof User.Data data ? data : null);
        if (!rightsService.canViewAdminPage(rights)) {
            return "redirect:/forbidden";
        }

        if (!(authObj instanceof dk.utd.fordel.services.Authenticator.Auth)) {
            return "redirect:/login";
        }

        try (var uow = unitOfWork.begin()) {
            User userToDelete = User.of(email);
            userManager.deleteUser(userToDelete);
            uow.commit();
        } catch (UserDoesNotExist e) {
            throw Log.giveUp(logger, e, "Could not update username");
        } catch (UnitOfWorkException e) {
            throw Log.giveUp(logger, e, "Could not update username");
        } catch (Email.InvalidEmail e) {
            throw Log.giveUp(logger, e, "Invalid email");
        }

        return "redirect:/adminpage";
    }

    public record ProjectCard(String id, String title, String description, String studyLine, String status) {
    }

    public record UserCard(String id, String name, List<Roles> roles, String email, Integer projectId,
            boolean kickable) {
        public String rolesDisplay() {
            if (roles == null || roles.isEmpty()) {
                return "No role";
            }

            return roles.stream()
                    .map(UserController::displayName)
                    .collect(java.util.stream.Collectors.joining(", "));
        }
    }

    private static String displayName(Roles role) {
        return switch (role) {
            case STUDENT -> "Student";
            case ADVISOR -> "Advisor";
            case TEACHER -> "Teacher";
            case STUDY_LEADER -> "Study Leader";
            case ADMINISTRATOR -> "Administrator";
        };
    }
}
