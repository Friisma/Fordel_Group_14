package dk.utd.fordel.view;

import org.slf4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dk.utd.fordel.domain.Email;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.domain.UserDoesNotExist;
import dk.utd.fordel.services.Authenticator;
import dk.utd.fordel.services.Authenticator.Auth;
import dk.utd.fordel.services.Authenticator.BadCredentials;
import dk.utd.fordel.services.UserManager;
import dk.utd.fordel.utils.Log;
import dk.utd.fordel.utils.UnitOfWork;
import dk.utd.fordel.utils.UnitOfWork.UnitOfWorkException;
import jakarta.servlet.http.HttpSession;

@Controller
public class Login {

    private static final Logger logger = Log.getLogger(Login.class);
    private final UnitOfWork unitOfWork;
    private final UserManager userManager;

    public Login(UnitOfWork unitOfWork, UserManager userManager, Authenticator authenticator) {
        this.unitOfWork = unitOfWork;
        this.userManager = userManager;
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
            return "login";
        }

        Auth auth;
        User.Data userData;
        try (var uow = unitOfWork.begin()) {
            auth = authenticator.authenticate(user, password);
            userData = userManager.readUser(user);
            uow.commit();
        } catch (BadCredentials e) {
            model.addAttribute("error_password", "Du har indtastet forkert password eller email");
            logger.atWarn().setCause(e).log("Bad credentials");
            return "login";
        } catch (UserDoesNotExist e) {
            throw Log.giveUp(logger, e, "Authenticated user could not be loaded");
        } catch (UnitOfWorkException e) {
            throw Log.giveUp(logger, e);
        }

        session.setAttribute("auth", auth);
        session.setAttribute("userData", userData);

        return "redirect:/";

    }
}
