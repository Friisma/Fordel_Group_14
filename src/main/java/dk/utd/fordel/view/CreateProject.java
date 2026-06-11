package dk.utd.fordel.view;

import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.utils.Log;
import dk.utd.fordel.utils.UnitOfWork;
import dk.utd.fordel.view.utils.Field;
import org.slf4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class CreateProject {

    private static final Logger logger = Log.getLogger(CreateProject.class);
    private final UnitOfWork unitOfWork;
    private final ProjectManager projectManager;

    public CreateProject(UnitOfWork unitOfWork, ProjectManager projectManager){
        this.unitOfWork = unitOfWork;
        this.projectManager = projectManager;
    }

    @GetMapping("/create-Project")
    public String getCreateProject(Model model){
        model.addAttribute("title", "Opret projekt");
        model.addAttribute("project_name", new Field(
                "text", "name", "Navn på projekt",
                null, null));
        model.addAttribute("description", new Field(
                "text", "description", "Din beskrivelse",
                null, null));

        return "create-Project";
    }

}
