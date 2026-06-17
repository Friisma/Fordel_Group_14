package dk.utd.fordel.view;

import dk.utd.fordel.domain.Email;
import dk.utd.fordel.domain.StudyLine;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.services.Authenticator;
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

    public StudyLineController(RightsService rightsService, StudyLineManager studyLineManager, UnitOfWork unitOfWork,
                               UserManager userManager) {
        this.rightsService = rightsService;
        this.studyLineManager = studyLineManager;
        this.unitOfWork = unitOfWork;
        this.userManager = userManager;
    }

    @GetMapping("/studyline/{id}/manage")
    public String getStudyLine(@PathVariable int id, HttpSession session, Model model) {
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

            var studyLineLeaders = userManager.getAllLeadersRelatedToStudyLine(id);
            model.addAttribute("studyLineLeaders", studyLineLeaders);

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
