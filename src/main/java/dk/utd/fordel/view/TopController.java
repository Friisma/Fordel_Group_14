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

    @GetMapping("/")
    public String getHome(Model model, HttpSession session) {
        if (session.getAttribute("auth") == null) {
            return "redirect:/login";
        }
        model.addAttribute("title", "Fordel - en klar fordel");
        return "home";
    }

    @GetMapping("/display")
    public String getDisplay(Model model) {
        model.addAttribute("title", "Fremvisining");
        return "display";
    }

    @GetMapping("/error")
    public String getError(Model model) {
        return "error";
    }

}
