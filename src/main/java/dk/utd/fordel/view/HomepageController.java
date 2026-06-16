package dk.utd.fordel.view;

import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.domain.Project;
import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.utils.UnitOfWork;
import dk.utd.fordel.services.Authenticator.Auth;
import dk.utd.fordel.services.rights.RightsContext;
import dk.utd.fordel.services.rights.RightsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class HomepageController {

    private final RightsService rightsService;
    private final ProjectManager projectManager;
    private final UnitOfWork unitOfWork;

    public HomepageController(RightsService rightsService, ProjectManager projectManager, UnitOfWork unitOfWork) {
        this.rightsService = rightsService;
        this.projectManager = projectManager;
        this.unitOfWork = unitOfWork;
    }

    @GetMapping("/")
    public String getHomepage(Model model, HttpSession session) {
        if (session.getAttribute("auth") == null) {
            return "redirect:/login";
        }

        User.Data userData = sessionUserData(session);
        RightsContext rights = rightsService.from(userData);

        model.addAttribute("userData", userData);
        model.addAttribute("title", "Fordel");
        model.addAttribute("roleBadge", buildRoleBadge(rights));
        model.addAttribute("sectionVisibility", new SectionVisibility(
                rightsService.canViewProject(rights),
                canShowInvitations(rights),
                false));
        // load actual projects for this user
        Object authObject = session.getAttribute("auth");
        if (authObject instanceof Auth auth) {
            try (var uow = unitOfWork.begin()) {
                var projects = projectManager.getAllProjectsRelatedToUser(auth.user());
                var projectCards = projects.stream().map(p -> new ProjectCard(
                        String.valueOf(p.getProjectID()),
                        p.getProjectName(),
                        p.getDescription() != null ? p.getDescription() : "",
                        p.getStudyLine() != null ? p.getStudyLine() : "")).toList();
                model.addAttribute("projects", projectCards);
                uow.commit();
            } catch (Project.InvalidProject e) {
                model.addAttribute("projects", placeholderProjects());
            } catch (UnitOfWork.UnitOfWorkException e) {
                model.addAttribute("projects", placeholderProjects());
            }
        } else {
            model.addAttribute("projects", placeholderProjects());
        }
        model.addAttribute("invitations", placeholderInvitations());
        model.addAttribute("advisorsMessage", "Advisors Section (not done)");
        return "home";
    }

    private User.Data sessionUserData(HttpSession session) {
        Object userData = session.getAttribute("userData");
        if (userData instanceof User.Data data) {
            return data;
        }

        return null;
    }

    private List<ProjectCard> placeholderProjects() {
        return List.of(
                new ProjectCard(
                        "1",
                        "Project 1",
                        "Placeholder project description.",
                        "Software Technology"),
                new ProjectCard(
                        "2",
                        "Project 2",
                        "Placeholder project description.",
                        "Software Technology"));
    }

    private List<InvitationCard> placeholderInvitations() {
        return List.of(
                new InvitationCard("Peter Sterner", "Pending", "pending"),
                new InvitationCard("Hans Henrik", "Accepted", "accepted"),
                new InvitationCard("Mišo Kovač", "Declined", "declined"));
    }

    private String buildRoleBadge(RightsContext rights) {
        if (rights == null || rights.roles().isEmpty()) {
            return "No role";
        }

        return rights.roles().stream()
                .sorted(Comparator.comparingInt(Roles::getClearanceLevel))
                .map(HomepageController::displayName)
                .collect(Collectors.joining(", "));
    }

    private boolean canShowInvitations(RightsContext rights) {
        return rights != null && (rights.isStudent()
                || rights.isAdvisor()
                || rights.isTeacher()
                || rights.isStudyLeader()
                || rights.isAdministrator());
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

    public record ProjectCard(String id, String title, String description, String studyLine) {
    }

    public record InvitationCard(String name, String status, String statusClass) {
    }

    public record SectionVisibility(boolean projectsVisible, boolean invitationsVisible, boolean advisorsVisible) {
    }
}