package dk.utd.fordel.view;

import dk.utd.fordel.domain.Project;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.services.Authenticator.Auth;
import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.services.rights.RightsContext;
import dk.utd.fordel.services.rights.RightsService;
import dk.utd.fordel.utils.UnitOfWork;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ProjectsController {

    private final ProjectManager projectManager;
    private final RightsService rightsService;
    private final UnitOfWork unitOfWork;

    public ProjectsController(ProjectManager projectManager, RightsService rightsService, UnitOfWork unitOfWork) {
        this.projectManager = projectManager;
        this.rightsService = rightsService;
        this.unitOfWork = unitOfWork;
    }

    @GetMapping("/projects")
    public String getProjects(HttpSession session, Model model) {
        Object authObject = session.getAttribute("auth");
        if (!(authObject instanceof Auth auth)) {
            return "redirect:/login";
        }

        Object userDataObject = session.getAttribute("userData");
        RightsContext rights = rightsService.from(userDataObject instanceof User.Data data ? data : null);
        if (!rightsService.canViewProject(rights)) {
            return "redirect:/forbidden";
        }

        List<Project> projects;
        try (var uow = unitOfWork.begin()) {
            projects = projectManager.getAllProjectsRelatedToUser(auth.user());
            uow.commit();
        } catch (Project.InvalidProject e) {
            throw new RuntimeException("Could not load projects for user", e);
        } catch (UnitOfWork.UnitOfWorkException e) {
            throw new RuntimeException("Could not open project listing transaction", e);
        }
        model.addAttribute("title", "Projects");
        model.addAttribute("projects", projects.stream().map(ProjectsController::toProjectCard).toList());
        return "projects";
    }

    public static ProjectCard toProjectCard(Project project) {
        return new ProjectCard(
                String.valueOf(project.getProjectID()),
                project.getProjectName(),
                project.getDescription() != null ? project.getDescription() : "",
                project.getStudyLine() != null ? project.getStudyLine() : "",
                project.getStatus() != null ? project.getStatus().toString() : ""
        );
    }

    public record ProjectCard(String id, String title, String description, String studyLine, String status) {
    }
}