package dk.utd.fordel.view;

import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.utils.Log;
import dk.utd.fordel.utils.UnitOfWork;
import org.slf4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

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



        return "create-Project";
    }

}
