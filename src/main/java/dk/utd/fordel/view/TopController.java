package dk.utd.fordel.view;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

@Controller
public class TopController {

    private static final Logger logger = LoggerFactory.getLogger(TopController.class);

    @GetMapping("/about")
    public String getDisplay(Model model) {
        model.addAttribute("title", "About");
        return "about";
    }

    @GetMapping("/error")
    public String getError(Model model) {
        model.addAttribute("title", "Error");
        return "error";
    }

    @GetMapping("/forbidden")
    public String getForbidden(Model model) {
        model.addAttribute("title", "Access Denied");
        return "forbidden";
    }

    @GetMapping("/password-expired")
    public String getPasswordExpired(Model model) {
        model.addAttribute("title", "Password expired");
        return "password-expired";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

}
