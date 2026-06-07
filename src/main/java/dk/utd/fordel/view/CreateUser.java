package dk.utd.fordel.view;

import org.slf4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dk.utd.fordel.domain.Email;
import dk.utd.fordel.domain.EmailAlreadyInUse;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.domain.UserDoesNotExist;
import dk.utd.fordel.services.Authenticator;
import dk.utd.fordel.services.Authenticator.Auth;
import dk.utd.fordel.services.UserManager;
import dk.utd.fordel.utils.Log;
import dk.utd.fordel.utils.UnitOfWork;
import dk.utd.fordel.view.utils.Field;
import jakarta.servlet.http.HttpSession;

@Controller
public class CreateUser {

    private static final Logger logger = Log.getLogger(CreateUser.class);

    private final Authenticator authenticator;
    private final UserManager userManager;
    private final UnitOfWork unitOfWork;

    public CreateUser(UserManager userManager, UnitOfWork unitOfWork, Authenticator authenticator) {
        this.userManager = userManager;
        this.unitOfWork = unitOfWork;
        this.authenticator = authenticator;
    }

    @GetMapping("/create-user")
    public String getCreateUser(Model model) {
        model.addAttribute("title", "Opret bruger");
        model.addAttribute("name", new Field(
                "text", "name", "Dit navn",
                null, null));
        model.addAttribute("email", new Field(
                "email", "email", "Din email",
                null, null));
        model.addAttribute("password", new Field(
                "password", "password", "Dit password",
                null, null));
        return "create-user";
    }

    @PostMapping("/create-user")
    public String postCreateUser(Model model,
            HttpSession session,
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("password") String password) {
        model.addAttribute("title", "Opret bruger");

        Boolean isValid = true;

        // Validate input
        String nameMessage = null;
        if (name.isEmpty()) {
            nameMessage = "Navn er påkrævet";
            isValid = false;
        }

        String emailMessage = null;
        User user = null;
        try {
            user = User.of(email);
        } catch (Email.InvalidEmail e) {
            emailMessage = e.getMessage();
            isValid = false;
        }

        String passwordMessage = null;
        if (password.isEmpty()) {
            isValid = false;
        }

        if (isValid) {
            try (var uow = unitOfWork.begin()) {
                logger.info("UnitOfWork started: {}", uow);
                userManager.createUser(user, new User.Data(name));
                authenticator.register(user, password);
                uow.commit();
            } catch (EmailAlreadyInUse e) {
                emailMessage = "Email er allerede i brug";
                isValid = false;
            } catch (UnitOfWork.UnitOfWorkException e) {
                throw Log.giveUp(logger, e, "Failed to create user");
            } catch (UserDoesNotExist e) {
                throw Log.giveUp(logger, e, "Should not happen");
            }
        }

        if (!isValid) {
            model.addAttribute("name", new Field(
                    "text", "name", "Dit navn",
                    name, nameMessage));
            model.addAttribute("email", new Field(
                    "email", "email", "Din email",
                    email, emailMessage));
            model.addAttribute("password", new Field(
                    "password", "password", "Dit password",
                    "", passwordMessage));
            return "create-user";
        } else {
            Auth auth;
            try (var uow = unitOfWork.begin()) {
                auth = authenticator.authenticate(user, password);
                uow.commit();
            } catch (Authenticator.BadCredentials e) {
                throw Log.giveUp(logger, e, "Unexpected bad credentials");
            } catch (UnitOfWork.UnitOfWorkException e) {
                throw Log.giveUp(logger, e);
            }
            logger.info("User {} created and authenticated", user);
            session.setAttribute("auth", auth);
            return "redirect:/";
        }
    }

}
