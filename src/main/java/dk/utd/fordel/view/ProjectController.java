package dk.utd.fordel.view;

import dk.utd.fordel.domain.User;
import dk.utd.fordel.services.Authenticator.Auth;
import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.services.rights.RightsContext;
import dk.utd.fordel.services.rights.RightsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import jakarta.servlet.http.HttpSession;

@Controller
public class ProjectController {

    private final RightsService rightsService;
    private final ProjectManager projectManager;

    public ProjectController(RightsService rightsService, ProjectManager projectManager) {
        this.rightsService = rightsService;
        this.projectManager = projectManager;
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

        if (!projectManager.canViewProject(auth.user(), id)) {
            return "redirect:/forbidden";
        }

        model.addAttribute("title", "Project " + id);
        model.addAttribute("projectId", id);
        return "project";
    }

}
