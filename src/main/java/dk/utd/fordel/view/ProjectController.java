package dk.utd.fordel.view;

import dk.utd.fordel.domain.User;
import dk.utd.fordel.domain.Project;
import dk.utd.fordel.services.Authenticator.Auth;
import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.services.UserManager;
import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.services.rights.RightsContext;
import dk.utd.fordel.services.rights.RightsService;
import dk.utd.fordel.utils.UnitOfWork;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import jakarta.servlet.http.HttpSession;

@Controller
public class ProjectController {

    private final RightsService rightsService;
    private final ProjectManager projectManager;
    private final UserManager userManager;
    private final UnitOfWork unitOfWork;

    public ProjectController(RightsService rightsService, ProjectManager projectManager, UnitOfWork unitOfWork,
            UserManager userManager) {
        this.rightsService = rightsService;
        this.projectManager = projectManager;
        this.unitOfWork = unitOfWork;
        this.userManager = userManager;
    }

    @GetMapping("/project/{id}")
    public String getProject(@PathVariable int id, HttpSession session, Model model) {
        Object authObject = session.getAttribute("auth");
        if (!(authObject instanceof Auth auth)) {
            return "redirect:/login";
        }

        Object userData = session.getAttribute("userData");
        RightsContext rights = rightsService.from(userData instanceof User.Data data ? data : null);
        if (!rightsService.canViewProject(rights)) {
            return "redirect:/forbidden";
        }

        Project project = null;
        try (var uow = unitOfWork.begin()) {
            if (!projectManager.canViewProject(auth.user(), id)) {
                return "redirect:/forbidden";
            }
            // load project details for the view
            try {
                var projects = projectManager.getAllProjectsRelatedToUser(auth.user());
                for (var p : projects) {
                    if (p.getProjectID() == id) {
                        project = p;
                        break;
                    }
                }
            } catch (Project.InvalidProject e) {
                // ignore and leave project null
            }
            model.addAttribute("project", project);
            // populate available advisors for the invite modal
            try {
                var allUsers = userManager.getAllUsers();
                String currentEmail = auth.user().getEmail().asString();
                var advisors = allUsers.stream()
                        .filter(u -> u.roles().contains(Roles.ADVISOR))
                        .filter(u -> !currentEmail.equalsIgnoreCase(u.email()))
                        .toList();
                model.addAttribute("availableAdvisors", advisors);
            } catch (Exception e) {
                model.addAttribute("availableAdvisors", java.util.List.of());
            }
            uow.commit();
        } catch (UnitOfWork.UnitOfWorkException e) {
            throw new RuntimeException("Could not open project access transaction", e);
        }

        if (project != null) {
            model.addAttribute("title", project.getProjectName());
        } else {
            model.addAttribute("title", "Project " + id);
        }
        model.addAttribute("projectId", id);
        return "project";
    }

}
