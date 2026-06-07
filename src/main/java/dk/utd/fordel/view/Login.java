package dk.utd.fordel.view;

import org.slf4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dk.utd.fordel.domain.Email;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.services.Authenticator;
import dk.utd.fordel.services.Authenticator.Auth;
import dk.utd.fordel.services.Authenticator.BadCredentials;
import dk.utd.fordel.utils.Log;
import dk.utd.fordel.utils.UnitOfWork;
import dk.utd.fordel.utils.UnitOfWork.UnitOfWorkException;
import jakarta.servlet.http.HttpSession;

@Controller
public class Login {

    private static final Logger logger = Log.getLogger(Login.class);
    private final UnitOfWork unitOfWork;

    public Login(UnitOfWork unitOfWork, Authenticator authenticator) {
        this.unitOfWork = unitOfWork;
        this.authenticator = authenticator;
    }

    private final Authenticator authenticator;

    @GetMapping("/login")
    public String getLogin() {
        return "login";
    }

    @PostMapping("/login")
    public String postLogin(
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            Model model,
            HttpSession session) {

        logger.info("email: " + email);
        logger.info("password: " + password);

        User user;
        try {
            user = User.of(email);
        } catch (Email.InvalidEmail e) {
            model.addAttribute("error_email", e.getMessage());
            logger.atError().setCause(e).log("Invalid email");
            return "home";
        }

        Auth auth;
        try (var uow = unitOfWork.begin()) {
            auth = authenticator.authenticate(user, password);
            uow.commit();
        } catch (BadCredentials e) {
            model.addAttribute("error_password", "Du har indtastet forkert password eller email");
            logger.atWarn().setCause(e).log("Bad credentials");
            return "home";
        } catch (UnitOfWorkException e) {
            throw Log.giveUp(logger, e);
        }

        session.setAttribute("auth", auth);

        return "redirect:/";

    }
}
