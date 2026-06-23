package dk.utd.fordel.view;

import dk.utd.fordel.domain.Email;
import dk.utd.fordel.domain.StudyLine;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.services.Authenticator;
import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.services.StudyLineManager;
import dk.utd.fordel.services.UserManager;
import dk.utd.fordel.services.rights.RightsContext;
import dk.utd.fordel.services.rights.RightsService;
import dk.utd.fordel.utils.UnitOfWork;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class StudyLineController {
    private final RightsService rightsService;
    private final StudyLineManager studyLineManager;
    private final UnitOfWork unitOfWork;
    private final UserManager userManager;
    private final ProjectManager projectManager;

    public StudyLineController(RightsService rightsService, StudyLineManager studyLineManager, UnitOfWork unitOfWork,
                               UserManager userManager, ProjectManager projectManager) {
        this.rightsService = rightsService;
        this.studyLineManager = studyLineManager;
        this.unitOfWork = unitOfWork;
        this.userManager = userManager;
        this.projectManager = projectManager;
    }

    @GetMapping("/studyline/{id}")
    public String getStudyLine(@PathVariable int id, HttpSession session, Model model) {
        Object authObject = session.getAttribute("auth");
        if (!(authObject instanceof Authenticator.Auth auth)) {
            return "redirect:/login";
        }

        Object userData = session.getAttribute("userData");
        RightsContext rights = rightsService.from(userData instanceof User.Data data ? data : null);
        if (!rightsService.canViewStudyLine(rights)) {
            return "redirect:/forbidden";
        }

        try (var uow = unitOfWork.begin()){
            if (!studyLineManager.canViewStudyLine(auth.user(), id)) {
                return "redirect:/forbidden";
            }

            model.addAttribute("studyLineId", id);

            var studyLineName = studyLineManager.getStudyLineNameById(id);
            model.addAttribute("studyLineName", studyLineName);

            var studyLineAllProjects = projectManager.getAllProjectsRelatedToStudyLine(id);
            model.addAttribute("studyLineAllProjects", studyLineAllProjects.stream().map(ProjectsController::toProjectCard).toList());

            model.addAttribute("sectionVisibility", new SectionVisibility(
                    rightsService.canManageStudyLine(rights),
                    rights != null && rights.isAdvisor()));

            var studyLineLoggedInUserProjects = projectManager.getAllProjectsRelatedToStudyLineAndUser(id, auth.user());
            model.addAttribute("studyLineLoggedInUserProjects", studyLineLoggedInUserProjects.stream().map(ProjectsController::toProjectCard).toList());

            uow.commit();

        } catch (UnitOfWork.UnitOfWorkException e) {
            throw new RuntimeException(e);
        }
        return "studyline";
    }

    public record SectionVisibility(boolean projectsVisible, boolean advisorsVisible) {

    }

    @GetMapping("/studyline/{id}/members")
    public String getStudyLineMembers(@PathVariable int id, HttpSession session, Model model) {
        Object authObject = session.getAttribute("auth");
        if (!(authObject instanceof Authenticator.Auth auth)) {
            return "redirect:/login";
        }

        Object userData = session.getAttribute("userData");
        RightsContext rights = rightsService.from(userData instanceof User.Data data ? data : null);
        if (!rightsService.canViewStudyLine(rights)) {
            return "redirect:/forbidden";
        }

        try (var uow = unitOfWork.begin()){
            if (!studyLineManager.canViewStudyLine(auth.user(), id)) {
                return "redirect:/forbidden";
            }

            model.addAttribute("studyLineId", id);

            var studyLineName = studyLineManager.getStudyLineNameById(id);
            model.addAttribute("studyLineName", studyLineName);

            var studyLineMembers = userManager.getAllUsersRelatedToStudyLine(id);
            model.addAttribute("studyLineMembers", studyLineMembers);

            uow.commit();

        } catch (UnitOfWork.UnitOfWorkException e) {
            throw new RuntimeException(e);
        }
        return "studyline-members";
    }

    @GetMapping("/studyline/{id}/manage")
    public String getStudyLineMange(@PathVariable int id, HttpSession session, Model model) {
        Object authObject = session.getAttribute("auth");
        if (!(authObject instanceof Authenticator.Auth auth)) {
            return "redirect:/login";
        }

        Object userData = session.getAttribute("userData");
        RightsContext rights = rightsService.from(userData instanceof User.Data data ? data : null);
        if (!rightsService.canManageStudyLine(rights)) {
            return "redirect:/forbidden";
        }

        try (var uow = unitOfWork.begin()){
            if (!studyLineManager.canManageStudyLine(auth.user(), id)) {
                return "redirect:/forbidden";
            }

            var studyLineStudents = userManager.getAllStudentsRelatedToStudyLine(id);
            model.addAttribute("studyLineStudents", studyLineStudents);

            var availableStudents = userManager.getAllUsers().stream()
                    .filter(user -> user.roles().contains(Roles.STUDENT))
                    .filter(user -> studyLineStudents.stream()
                            .noneMatch(student -> student.email().equalsIgnoreCase(user.email())))
                    .toList();
            model.addAttribute("availableStudents", availableStudents);

            var studyLineLeaders = userManager.getAllLeadersRelatedToStudyLine(id);
            model.addAttribute("studyLineLeaders", studyLineLeaders);

            var studyLineAdvisors = userManager.getAllAdvisorsRelatedToStudyLine(id);
            model.addAttribute("studyLineAdvisors", studyLineAdvisors);

            model.addAttribute("studyLineId", id);
            uow.commit();

        } catch (UnitOfWork.UnitOfWorkException e) {
            throw new RuntimeException(e);
        }
        return "manage-study-line";
    }

    @PostMapping("/studyline/{id}/manage/remove-user")
    public String removeUser(@PathVariable int id, @RequestParam("emailToRemove") String emailToRemove, HttpSession session, Model model) {
        Object authObject = session.getAttribute("auth");
        if (!(authObject instanceof Authenticator.Auth auth)) {
            return "redirect:/login";
        }

        Object userData = session.getAttribute("userData");
        RightsContext rights = rightsService.from(userData instanceof User.Data data ? data : null);
        if (!rightsService.canManageStudyLine(rights)) {
            return "redirect:/forbidden";
        }

        try (var uow = unitOfWork.begin()){
            if (!studyLineManager.canManageStudyLine(auth.user(), id)) {
                return "redirect:/forbidden";
            }

            var userToAdd = User.of(emailToRemove);
            studyLineManager.removeUser(userToAdd, id);

            uow.commit();

        } catch (UnitOfWork.UnitOfWorkException e) {
            throw new RuntimeException(e);
        } catch (Email.InvalidEmail e) {
            throw new RuntimeException(e);
        }
        return "redirect:/studyline/" + id + "/manage";
    }

    @PostMapping("/studyline/{id}/manage/add-student")
    public String addStudent(@PathVariable int id, @RequestParam String emailToAdd, HttpSession session, Model model) {
        Object authObject = session.getAttribute("auth");
        if (!(authObject instanceof Authenticator.Auth auth)) {
            return "redirect:/login";
        }

        Object userData = session.getAttribute("userData");
        RightsContext rights = rightsService.from(userData instanceof User.Data data ? data : null);
        if (!rightsService.canManageStudyLine(rights)) {
            return "redirect:/forbidden";
        }

        try (var uow = unitOfWork.begin()){
            if (!studyLineManager.canManageStudyLine(auth.user(), id)) {
                return "redirect:/forbidden";
            }

            var userToAdd = User.of(emailToAdd);
            studyLineManager.addStudent(userToAdd, id);

            uow.commit();

        } catch (UnitOfWork.UnitOfWorkException e) {
            throw new RuntimeException(e);
        } catch (Email.InvalidEmail e) {
            throw new RuntimeException(e);
        }
        return "redirect:/studyline/" + id + "/manage";
    }
}
