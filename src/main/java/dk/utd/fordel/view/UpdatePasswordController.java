package dk.utd.fordel.view;

import dk.utd.fordel.view.utils.Field;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dk.utd.fordel.domain.UserDoesNotExist;
import dk.utd.fordel.services.Authenticator;
import dk.utd.fordel.services.Authenticator.Auth;
import dk.utd.fordel.services.Authenticator.BadCredentials;
import dk.utd.fordel.services.UserManager;
import dk.utd.fordel.utils.UnitOfWork;
import dk.utd.fordel.utils.UnitOfWork.UnitOfWorkException;

import java.time.LocalDateTime;

@Controller
public class UpdatePasswordController {

        private static final String PASSWORD_DESCRIPTION = "Current password";
        private static final String NEW_PASSWORD_DESCRIPTION = "New password";
        private static final String CONFIRM_PASSWORD_DESCRIPTION = "Confirm new password";
        private static final String PASSWORDS_DO_NOT_MATCH = "Passwords do not match";
        private static final String CURRENT_PASSWORD_INCORRECT = "Current password is incorrect";

        private final UnitOfWork unitOfWork;
        private final Authenticator authenticator;
        private final UserManager userManager;

        public UpdatePasswordController(UnitOfWork unitOfWork, Authenticator authenticator, UserManager userManager) {
                this.unitOfWork = unitOfWork;
                this.authenticator = authenticator;
                this.userManager = userManager;
        }

        @GetMapping("/update-password")
        public String getChangePassword(Model model, HttpSession session) {
                if (!isAuthenticated(session)) {
                        return "redirect:/login";
                }

                model.addAttribute("title", "Change Password");
                addPasswordFields(model, "", "", "", null);
                return "update-password";
        }

        @PostMapping("/change-password")
        public String postChangePassword(Model model,
                        HttpSession session,
                        @RequestParam("currentPassword") String currentPassword,
                        @RequestParam("newPassword") String newPassword,
                        @RequestParam("confirmNewPassword") String confirmNewPassword,
                        RedirectAttributes redirectAttributes) {
                if (!isAuthenticated(session)) {
                        return "redirect:/login";
                }

                model.addAttribute("title", "Change Password");

                if (!newPassword.equals(confirmNewPassword)) {
                        model.addAttribute("error", PASSWORDS_DO_NOT_MATCH);
                        addPasswordFields(model, currentPassword, newPassword, confirmNewPassword,
                                        PASSWORDS_DO_NOT_MATCH);
                        return "update-password";
                }

                Auth auth = currentAuth(session);
                if (auth == null) {
                        return "redirect:/login";
                }

                try (var uow = unitOfWork.begin()) {
                        authenticator.authenticate(auth.user(), currentPassword);
                        authenticator.register(auth.user(), newPassword);
                        userManager.setPasswordChangedAt(auth.user(), LocalDateTime.now());
                        uow.commit();
                } catch (BadCredentials e) {
                        addPasswordFields(model, currentPassword, newPassword, confirmNewPassword, null);
                        model.addAttribute("error", CURRENT_PASSWORD_INCORRECT);
                        return "update-password";
                } catch (UserDoesNotExist e) {
                        addPasswordFields(model, currentPassword, newPassword, confirmNewPassword, null);
                        model.addAttribute("error", CURRENT_PASSWORD_INCORRECT);
                        return "update-password";
                } catch (UnitOfWorkException e) {
                        throw new RuntimeException("Failed to update password", e);
                }

                redirectAttributes.addFlashAttribute("message", "Password changed");
                return "redirect:/";
        }

        private void addPasswordFields(Model model, String currentPassword, String newPassword,
                        String confirmNewPassword, String confirmNewPasswordMessage) {
                model.addAttribute("currentPassword", new Field(
                                "password", "currentPassword", PASSWORD_DESCRIPTION,
                                currentPassword, null));
                model.addAttribute("newPassword", new Field(
                                "password", "newPassword", NEW_PASSWORD_DESCRIPTION,
                                newPassword, null));
                model.addAttribute("confirmNewPassword", new Field(
                                "password", "confirmNewPassword", CONFIRM_PASSWORD_DESCRIPTION,
                                confirmNewPassword, confirmNewPasswordMessage));
        }

        private boolean isAuthenticated(HttpSession session) {
                return session.getAttribute("auth") != null;
        }

        private Auth currentAuth(HttpSession session) {
                Object authObject = session.getAttribute("auth");
                return authObject instanceof Auth auth ? auth : null;
        }
}