package dk.utd.fordel.view;

import dk.utd.fordel.view.utils.Field;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UpdatePasswordController {

        private static final String PASSWORD_DESCRIPTION = "Current password";
        private static final String NEW_PASSWORD_DESCRIPTION = "New password";
        private static final String CONFIRM_PASSWORD_DESCRIPTION = "Confirm new password";

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
                        @RequestParam("confirmNewPassword") String confirmNewPassword) {
                if (!isAuthenticated(session)) {
                        return "redirect:/login";
                }

                model.addAttribute("title", "Change Password");

                String confirmNewPasswordMessage = null;
                if (!newPassword.equals(confirmNewPassword)) {
                        confirmNewPasswordMessage = "Passwords do not match";
                }

                addPasswordFields(model, currentPassword, newPassword, confirmNewPassword, confirmNewPasswordMessage);

                return confirmNewPasswordMessage == null ? "redirect:/" : "update-password";
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
}