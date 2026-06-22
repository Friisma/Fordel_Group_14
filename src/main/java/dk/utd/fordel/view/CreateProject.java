package dk.utd.fordel.view;

import dk.utd.fordel.domain.Project;
import dk.utd.fordel.domain.ProjectTitleAlreadyInUse;
import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.services.Authenticator;
import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.services.StudyLineManager;
import dk.utd.fordel.services.UserManager;
import dk.utd.fordel.utils.Log;
import dk.utd.fordel.utils.UnitOfWork;
import dk.utd.fordel.view.utils.Field;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class CreateProject {

    private static final Logger logger = Log.getLogger(CreateProject.class);
    private final UnitOfWork unitOfWork;
    private final ProjectManager projectManager;
    private final Authenticator authenticator;
    private final UserManager userManager;
    private final StudyLineManager studyLineManager;

    public CreateProject(UnitOfWork unitOfWork, ProjectManager projectManager, Authenticator authenticator,
                         UserManager userManager, StudyLineManager studyLineManager) {
        this.unitOfWork = unitOfWork;
        this.projectManager = projectManager;
        this.authenticator = authenticator;
        this.userManager = userManager;
        this.studyLineManager = studyLineManager;
    }

    /**
     * GetMapping for the create-project side.
     *
     */
    @GetMapping("/create-project")
    public String getCreateProject(Model model, HttpSession session) {
        model.addAttribute("title", "Opret projekt");
        model.addAttribute("project_name", new Field("text", "project_name", "Project name", null, null));
        model.addAttribute("project_description", new Field("text", "project_description", "Description", null, null));

        Authenticator.Auth auth = (Authenticator.Auth) session.getAttribute("auth");
        User user = auth.user();

        try (var uow = unitOfWork.begin()) {
            List<String> studyLines = studyLineManager.getAllStudyLines();
            model.addAttribute("studyLines", studyLines);
            List<Roles> roles = userManager.getUserRoles(user);
            model.addAttribute("isAdvisor", roles.contains(Roles.ADVISOR));
            uow.commit();
        } catch (UnitOfWork.UnitOfWorkException e) {
            Log.giveUp(logger, e, "Failed to load study lines");
        }

        return "create-project";
    }

    /**
     * Post method for creating a project.
     *
     */

    @PostMapping("/create-project")
    public String postCreateProject(Model model,
                                    @RequestParam("project_name") String projectName,
                                    @RequestParam("project_description") String projectDescription,
                                    @RequestParam("study_line") String studyLineName,
                                    HttpSession session){
        model.addAttribute("title", "Opret projekt");

        boolean isValid = true;



        String projectNameMessage = null;
        if (projectName.isEmpty()){
            projectNameMessage = "Project needs a name";
            isValid = false;
        }

        String projectDescriptionMessage = null;
        if (projectDescription.isEmpty()){
            projectDescriptionMessage = "Project needs a description";
            isValid = false;
        }

        Object userDataObject = session.getAttribute("userData");
        User.Data userData = userDataObject instanceof User.Data data ? data : null;

        if (studyLineName == null || studyLineName.isBlank()) {
            projectNameMessage = "You must select a study line";
            isValid = false;
        } else {
            Authenticator.Auth auth = (Authenticator.Auth) session.getAttribute("auth");
            User user = auth.user();
            try (var uow = unitOfWork.begin()) {
                Roles role = userManager.determineStudyLineSpecificRole(user, studyLineName);
                uow.commit();
                if (role == null) {
                    projectNameMessage = "You do not belong to the selected study line";
                    isValid = false;
                }
            } catch (UnitOfWork.UnitOfWorkException e) {
                Log.giveUp(logger, e, "Failed to check study line membership");
                isValid = false;
            }
        }

        Project project = null;
        try {
             project = new Project(projectName, projectDescription);
        } catch (Project.InvalidProject e) {
            isValid = false;
            projectNameMessage = "Cant create project";
        }

        if (isValid){
            try (var uow = unitOfWork.begin()){
                logger.info("UnitOfWork started: {}", uow);
                Authenticator.Auth auth = (Authenticator.Auth)session.getAttribute("auth");
                User user = auth.user();
                int userId = userManager.getUserIdByEmail(user.getEmail());
                projectManager.createProject(project, user, studyLineName, userId);
                uow.commit();

            } catch (ProjectTitleAlreadyInUse e) {
                isValid = false;
                projectNameMessage = "Invalid project title, already in use";


            } catch (UnitOfWork.UnitOfWorkException e) {
                Log.giveUp(logger, e, "Failed to create Project");
                isValid = false;
                projectNameMessage = "Failed to create project";

            }
        }

        if(!isValid){
            model.addAttribute("title", "Opret projekt");
            model.addAttribute("project_name", new Field(
                    "text", "project_name", "Project name",
                    "", projectNameMessage));
            model.addAttribute("project_description", new Field(
                    "text", "project_description", "Description",
                    "", projectDescriptionMessage));

            try (var uow = unitOfWork.begin()) {
                model.addAttribute("studyLines", studyLineManager.getAllStudyLines());
                uow.commit();
            } catch (UnitOfWork.UnitOfWorkException e) {
                Log.giveUp(logger, e, "Failed to reload study lines");
            }

            return "create-project";
        } else {
            logger.info("project {} created", project);
            return "redirect:/";
        }
    }


    @PostMapping("/create-advisor-project")
    public String postCreateProjectAdvisor(Model model,
                                    @RequestParam("project_name") String projectName,
                                    @RequestParam("project_description") String projectDescription,
                                           @RequestParam("study_line") String studyLineName,
                                    HttpSession session){
        model.addAttribute("title", "Opret projekt");

        boolean isValid = true;

        String projectNameMessage = null;
        if (projectName.isEmpty()){
            projectNameMessage = "Project needs a name";
            isValid = false;
        }

        String projectDescriptionMessage = null;
        if (projectDescription.isEmpty()){
            projectDescriptionMessage = "Project needs a description";
            isValid = false;
        }

        Object userDataObject = session.getAttribute("userData");
        User.Data userData = userDataObject instanceof User.Data data ? data : null;

        if (studyLineName == null || studyLineName.isBlank()) {
            projectNameMessage = "You must belong to a study line before creating a project";
            isValid = false;
        }

        Project project = null;
        try {
            project = new Project(projectName, projectDescription);
        } catch (Project.InvalidProject e) {
            isValid = false;
            projectNameMessage = "Cant create project";
        }

        if (isValid){
            try (var uow = unitOfWork.begin()){
                logger.info("UnitOfWork started: {}", uow);
                Authenticator.Auth auth = (Authenticator.Auth)session.getAttribute("auth");
                User user = auth.user();
                int userId = userManager.getUserIdByEmail(user.getEmail());
                projectManager.createAdvisorProject(project,studyLineName,userId);
                uow.commit();

            } catch (ProjectTitleAlreadyInUse e) {
                isValid = false;
                projectNameMessage = "Invalid project title, already in use";


            } catch (UnitOfWork.UnitOfWorkException e) {
                Log.giveUp(logger, e, "Failed to create Project");
                isValid = false;
                projectNameMessage = "Failed to create project";

            }
        }

        if(!isValid){
            model.addAttribute("title", "Opret projekt");
            model.addAttribute("project_name", new Field(
                    "text", "project_name", "Project name",
                    "", projectNameMessage));
            model.addAttribute("project_description", new Field(
                    "text", "project_description", "Description",
                    "", projectDescriptionMessage));

            try (var uow = unitOfWork.begin()) {
                model.addAttribute("studyLines", studyLineManager.getAllStudyLines());
                uow.commit();
            } catch (UnitOfWork.UnitOfWorkException e) {
                Log.giveUp(logger, e, "Failed to reload study lines");
            }

            return "create-project";
        } else {
            logger.info("project {} created", project);
            return "redirect:/";
        }
    }

}
