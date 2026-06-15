package dk.utd.fordel.view;

import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.domain.User;
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
public class HomeController {

    private final RightsService rightsService;

    public HomeController(RightsService rightsService) {
        this.rightsService = rightsService;
    }

    @GetMapping("/")
    public String getHome(Model model, HttpSession session) {
        if (session.getAttribute("auth") == null) {
            return "redirect:/login";
        }

        User.Data userData = sessionUserData(session);
        RightsContext rights = rightsService.from(userData);

        model.addAttribute("title", "Fordel");
        model.addAttribute("roleBadge", buildRoleBadge(rights));
        model.addAttribute("sectionVisibility", buildSectionVisibility(rights));
        model.addAttribute("projects", buildProjects());
        model.addAttribute("invitations", buildInvitations());
        model.addAttribute("advisorsMessage", buildAdvisorsMessage());
        return "home";
    }

    private User.Data sessionUserData(HttpSession session) {
        Object userData = session.getAttribute("userData");
        if (userData instanceof User.Data data) {
            return data;
        }

        return null;
    }

    private String buildRoleBadge(RightsContext rights) {
        if (rights == null || rights.roles().isEmpty()) {
            return "No role";
        }

        return rights.roles().stream()
                .sorted(Comparator.comparingInt(Roles::getClearanceLevel))
                .map(HomeController::displayName)
                .collect(Collectors.joining(", "));
    }

    private SectionVisibility buildSectionVisibility(RightsContext rights) {
        return new SectionVisibility(
                canShowProjects(rights),
                canShowInvitations(rights),
                canShowAdvisors(rights));
    }

    private boolean canShowProjects(RightsContext rights) {
        return rights != null && (rights.isStudent()
                || rights.isAdvisor()
                || rights.isTeacher()
                || rights.isStudyLeader()
                || rights.isAdministrator());
    }

    private boolean canShowInvitations(RightsContext rights) {
        return rights != null && (rights.isStudent()
                || rights.isAdvisor()
                || rights.isTeacher()
                || rights.isStudyLeader()
                || rights.isAdministrator());
    }

    private boolean canShowAdvisors(RightsContext rights) {
        return false;
    }

    private List<ProjectCard> buildProjects() {
        return List.of(
                new ProjectCard(
                        "project1",
                        "Project 1",
                        "Nullam dui arcu, malesuada et sodales eu, efficitur vitae dolor. Sed ultricies dolor non ante vulputate hendrerit. Vivamus sit amet suscipit sapien. Nulla iaculis eros a elit pharetra egestas. Nunc placerat facilisis cursus. Sed vestibulum metus eget dolor pharetra rutrum."),
                new ProjectCard(
                        "article2",
                        "Project 2",
                        "Nullam dui arcu, malesuada et sodales eu, efficitur vitae dolor. Sed ultricies dolor non ante vulputate hendrerit. Vivamus sit amet suscipit sapien. Nulla iaculis eros a elit pharetra egestas. Nunc placerat facilisis cursus. Sed vestibulum metus eget dolor pharetra rutrum."));
    }

    private List<InvitationCard> buildInvitations() {
        return List.of(
                new InvitationCard("Peter Sterner", "Pending", "pending"),
                new InvitationCard("Hans Henrik", "Accepted", "accepted"),
                new InvitationCard("Mišo Kovač", "Declined", "declined"));
    }

    private String buildAdvisorsMessage() {
        return "Advisors Section (not done)";
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

    public record ProjectCard(String id, String title, String description) {
    }

    public record InvitationCard(String name, String status, String statusClass) {
    }

    public record SectionVisibility(boolean projectsVisible, boolean invitationsVisible, boolean advisorsVisible) {
    }
}
