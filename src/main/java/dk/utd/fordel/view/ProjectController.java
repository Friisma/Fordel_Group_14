package dk.utd.fordel.view;

import dk.utd.fordel.domain.Project;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.services.Authenticator;
import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.services.UserManager;
import dk.utd.fordel.services.rights.RightsContext;
import dk.utd.fordel.services.rights.RightsService;
import dk.utd.fordel.utils.Log;
import dk.utd.fordel.utils.UnitOfWork;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ProjectController {
    private static final Logger logger = Log.getLogger(UserController.class);

    private final ProjectManager projectManager;
    private final UserManager userManager;
    private final RightsService rightsService;
    private final UnitOfWork unitOfWork;

    public ProjectController(ProjectManager projectManager, RightsService rightsService, UnitOfWork unitOfWork, UserManager userManager) {
        this.projectManager = projectManager;
        this.rightsService = rightsService;
        this.unitOfWork = unitOfWork;
        this.userManager = userManager;
    }

    @GetMapping("/advisor-projects-view")
    public String getAllProjects(Model model, HttpSession session) {
        if (session.getAttribute("auth") == null) {
            return "redirect:/login";
        }
        Object userData = session.getAttribute("userData");
        RightsContext rights = rightsService.from(userData instanceof User.Data data ? data : null);
        if (!rightsService.canViewAllProjects(rights)) {
            return "redirect:/forbidden";
        }

        try (var uow = unitOfWork.begin()) {
            var projectList = projectManager.getAllProjects();
            model.addAttribute("title", "Projects");
            model.addAttribute("projects", projectList);
        } catch (Project.InvalidProject e) {
            throw new RuntimeException(e);
        } catch (UnitOfWork.UnitOfWorkException e) {
            throw Log.giveUp(logger, e);
        }

        return "advisor-projects-view";
    }

    @PostMapping("/advisor-projects-view/assignAdvisor")
    public String assignAdvisor(HttpSession session,
                                @RequestParam("project_name") String projectName,
                                @RequestParam("project_description") String projectDescription,
                                @RequestParam("project_id") int projectId) {
        //Check if user is logged in
        if (session.getAttribute("auth") == null) {
            return "redirect:/login";
        }
        //Check if user has the right to perform action.
        Object userData = session.getAttribute("userData");
        RightsContext rights = rightsService.from(userData instanceof User.Data data ? data : null);
        if (!rightsService.canViewAllProjects(rights)) {
            return "redirect:/forbidden";
        }

        try (var uow = unitOfWork.begin()) {
            Authenticator.Auth auth = (Authenticator.Auth)session.getAttribute("auth");
            User user = auth.user();
            int userId = userManager.getUserIdByEmail(user.getEmail());
            Project project = new Project(projectName,projectDescription,projectId);
            projectManager.assignAdvisor(project,user);
            uow.commit();

        } catch (UnitOfWork.UnitOfWorkException e) {
            throw Log.giveUp(logger, e, "Could not assign advisor");
        } catch (Project.InvalidProject e) {
            throw Log.giveUp(logger, e, "Could not assign advisor - invalid project" + projectName);
        }

        return "redirect:/";
    }
}
