package dk.utd.fordel.view;

import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.services.rights.RightsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
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

        model.addAttribute("title", "Fordel");
        model.addAttribute("roleBadge", roleBadge(session));
        return "home";
    }

    private String roleBadge(HttpSession session) {
        Object userData = session.getAttribute("userData");
        if (!(userData instanceof User.Data data)) {
            return "No role";
        }

        var rights = rightsService.from(data);
        if (rights.roles().isEmpty()) {
            return "No role";
        }

        return rights.roles().stream()
                .sorted(Comparator.comparingInt(Roles::getClearanceLevel))
                .map(HomeController::displayName)
                .collect(Collectors.joining(", "));
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