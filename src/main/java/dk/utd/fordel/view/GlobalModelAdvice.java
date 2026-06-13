package dk.utd.fordel.view;

import dk.utd.fordel.domain.User;
import dk.utd.fordel.services.rights.RightsContext;
import dk.utd.fordel.services.rights.RightsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    private final RightsService rightsService;

    public GlobalModelAdvice(RightsService rightsService) {
        this.rightsService = rightsService;
    }

    @ModelAttribute("userData")
    public User.Data userData(HttpSession session) {
        Object userData = session.getAttribute("userData");
        if (userData instanceof User.Data data) {
            return data;
        }

        return null;
    }

    @ModelAttribute("rights")
    public RightsContext rights(HttpSession session) {
        Object userData = session.getAttribute("userData");
        if (userData instanceof User.Data data) {
            return rightsService.from(data);
        }

        return RightsContext.anonymous();
    }
}