package dk.utd.fordel.view;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ProjectController {

    @GetMapping("/project/{id}")
    public String getProject(@PathVariable int id, Model model) {
        model.addAttribute("title", "Project " + id);
        model.addAttribute("projectId", id);
        return "project";
    }

}
