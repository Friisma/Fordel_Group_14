package dk.utd.fordel.view;

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

            var studyLineUsers = userManager.getAllUsersRelatedToStudyLine(id);
            model.addAttribute("studyLineUsers", studyLineUsers);
            uow.commit();

        } catch (UnitOfWork.UnitOfWorkException e) {
            throw new RuntimeException(e);
        }
        return "manage-study-line";
    }
}
