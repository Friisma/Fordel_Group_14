package dk.utd.fordel.view;

import dk.utd.fordel.services.rights.RightsContext;
import dk.utd.fordel.services.rights.RightsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Objects;

@Controller
public class ProjectController {

    private final RightsService rightsService;

    public ProjectController(RightsService rightsService) {
        this.rightsService = rightsService;
    }

    @GetMapping("/project/{id}")
    public String getProject(@PathVariable int id, @ModelAttribute("rights") RightsContext rights, Model model) {
        if (!rightsService.canViewProject(Objects.requireNonNullElseGet(rights, RightsContext::anonymous))) {
            return "redirect:/forbidden";
        }

        model.addAttribute("title", "Project " + id);
        model.addAttribute("projectId", id);
        return "project";
    }

}
