package dk.utd.fordel.view;

import dk.utd.fordel.domain.Email;
import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.services.UserManager;
import org.slf4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import dk.utd.fordel.utils.Log;
import dk.utd.fordel.utils.UnitOfWork;
import dk.utd.fordel.utils.UnitOfWork.UnitOfWorkException;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserController {
    private static final Logger logger = Log.getLogger(UserController.class);

    private final UserManager userManager;
    private final UnitOfWork unitOfWork;

    public UserController(UserManager userManager, UnitOfWork unitOfWork) {
        this.userManager = userManager;
        this.unitOfWork = unitOfWork;
    }

    @GetMapping("/adminpage")
    public String getAllUsers(Model model, HttpSession session) {
        if (session.getAttribute("auth") == null) {
            return "redirect:/login";
        }

        try (var uow = unitOfWork.begin()){
            var usersList = userManager.getAllUsers();
            model.addAttribute("title", "Alle Brugere");
            model.addAttribute("users", usersList);
            uow.commit();
        } catch (UnitOfWorkException e) {
            throw Log.giveUp(logger, e);
        }
        return "adminpage";
    }

    @PostMapping("/adminpage/grant-role")
    public String grantRole(HttpSession session,
                            @RequestParam("email") String email,
                            @RequestParam("role") String roleString) {
        if (session.getAttribute("auth") == null) {
            return "redirect:/login";
        }

        try (var uow = unitOfWork.begin()) {

            User user = User.of(email);
            Roles roleToGrant = Roles.valueOf(roleString);

            userManager.grantRole(user, roleToGrant);

            uow.commit();

        } catch (Email.InvalidEmail e) {
            throw Log.giveUp(logger, e, "Invalid user");
        } catch (UnitOfWorkException e) {
            throw Log.giveUp(logger, e, "Could not grant user role");
        }

        return "redirect:/users";
    }
}
