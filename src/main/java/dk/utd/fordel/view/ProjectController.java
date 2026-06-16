package dk.utd.fordel.view;

import dk.utd.fordel.domain.User;
import dk.utd.fordel.domain.Project;
import dk.utd.fordel.services.Authenticator.Auth;
import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.services.UserManager;
import dk.utd.fordel.services.InvitationManager;
import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.services.rights.RightsContext;
import dk.utd.fordel.services.rights.RightsService;
import dk.utd.fordel.utils.UnitOfWork;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.domain.AdvisorAlreadyInvited;

@Controller
public class ProjectController {

    private final RightsService rightsService;
    private final ProjectManager projectManager;
    private final UserManager userManager;
    private final InvitationManager invitationManager;
    private final UnitOfWork unitOfWork;

    public ProjectController(RightsService rightsService, ProjectManager projectManager, UnitOfWork unitOfWork,
            UserManager userManager, InvitationManager invitationManager) {
        this.rightsService = rightsService;
        this.projectManager = projectManager;
        this.unitOfWork = unitOfWork;
        this.userManager = userManager;
        this.invitationManager = invitationManager;
    }

    @GetMapping("/project/{id}")
    public String getProject(@PathVariable int id, HttpSession session, Model model) {
        Object authObject = session.getAttribute("auth");
        if (!(authObject instanceof Auth auth)) {
            return "redirect:/login";
        }

        Object userData = session.getAttribute("userData");
        RightsContext rights = rightsService.from(userData instanceof User.Data data ? data : null);
        if (!rightsService.canViewProject(rights)) {
            return "redirect:/forbidden";
        }

        Project project = null;
        try (var uow = unitOfWork.begin()) {
            if (!projectManager.canViewProject(auth.user(), id)) {
                return "redirect:/forbidden";
            }
            // load project details for the view
            try {
                var projects = projectManager.getAllProjectsRelatedToUser(auth.user());
                for (var p : projects) {
                    if (p.getProjectID() == id) {
                        project = p;
                        break;
                    }
                }
            } catch (Project.InvalidProject e) {
                // ignore and leave project null
            }
            model.addAttribute("project", project);
            // populate available advisors for the invite modal
            try {
                var allUsers = userManager.getAllUsers();
                String currentEmail = auth.user().getEmail().asString();
                var advisors = allUsers.stream()
                        .filter(u -> u.roles().contains(Roles.ADVISOR))
                        .filter(u -> !currentEmail.equalsIgnoreCase(u.email()))
                        .toList();
                model.addAttribute("availableAdvisors", advisors);
            } catch (Exception e) {
                model.addAttribute("availableAdvisors", java.util.List.of());
            }
            // load invitations for this project (to show in the student/advisor sections)
            try {
                var inviteViews = invitationManager.getInvitesForProject(id);
                // Capitalize first letter of status for display (controller-level)
                var normalized = inviteViews.stream().map(iv -> {
                    String s = iv.status();
                    String cap = s == null || s.isEmpty() ? s : (Character.toUpperCase(s.charAt(0)) + s.substring(1));
                    return new dk.utd.fordel.services.InvitationManager.InviteView(iv.name(), iv.email(), cap,
                            iv.statusClass());
                }).toList();
                model.addAttribute("projectInvitations", normalized);
                // for advisors, filter invites relevant to them
                String current = auth.user().getEmail().asString();
                var advisorOwnInvites = normalized.stream().filter(iv -> iv.email().equalsIgnoreCase(current)).toList();
                model.addAttribute("advisorInvitations", advisorOwnInvites);
            } catch (Exception e) {
                model.addAttribute("projectInvitations", java.util.List.of());
                model.addAttribute("advisorInvitations", java.util.List.of());
            }
            uow.commit();
        } catch (UnitOfWork.UnitOfWorkException e) {
            throw new RuntimeException("Could not open project access transaction", e);
        }

        if (project != null) {
            model.addAttribute("title", project.getProjectName());
        } else {
            model.addAttribute("title", "Project " + id);
        }
        model.addAttribute("projectId", id);
        return "project";
    }

    @PostMapping("/project/{id}/invite-advisor")
    public String inviteAdvisor(@PathVariable int id,
            @RequestParam("advisorEmail") String advisorEmail,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Object authObject = session.getAttribute("auth");
        if (!(authObject instanceof Auth auth)) {
            return "redirect:/login";
        }

        // basic rights check
        Object userData = session.getAttribute("userData");
        RightsContext rights = rightsService.from(userData instanceof User.Data data ? data : null);
        if (!rightsService.canViewProject(rights)) {
            return "redirect:/forbidden";
        }

        try (var uow = unitOfWork.begin()) {
            if (!projectManager.canViewProject(auth.user(), id)) {
                return "redirect:/forbidden";
            }

            // find project object
            Project project = null;
            try {
                var projects = projectManager.getAllProjectsRelatedToUser(auth.user());
                for (var p : projects) {
                    if (p.getProjectID() == id) {
                        project = p;
                        break;
                    }
                }
            } catch (Project.InvalidProject e) {
                // fall through
            }

            if (project == null) {
                redirectAttributes.addFlashAttribute("error", "Project not found");
                return "redirect:/project/" + id;
            }

            try {
                var invited = User.of(advisorEmail);
                // prevent duplicate invites even before hitting DB unique constraint
                var existingInvites = invitationManager.getInvitesForProject(id);
                boolean alreadyInvited = existingInvites.stream()
                        .anyMatch(iv -> iv.email() != null && iv.email().equalsIgnoreCase(advisorEmail));
                if (alreadyInvited) {
                    uow.rollback();
                    redirectAttributes.addFlashAttribute("error", "Advisor already invited");
                    return "redirect:/project/" + id;
                }
                try {
                    invitationManager.createInvitaion(invited, project);
                    uow.commit();
                    redirectAttributes.addFlashAttribute("message", "Invitation sent");
                } catch (AdvisorAlreadyInvited ex) {
                    uow.rollback();
                    redirectAttributes.addFlashAttribute("error", "Advisor already invited");
                }
            } catch (dk.utd.fordel.domain.Email.InvalidEmail invalidEmail) {
                try {
                    uow.rollback();
                } catch (Exception ignore) {
                }
                redirectAttributes.addFlashAttribute("error", "Invalid advisor email");
            }

        } catch (UnitOfWork.UnitOfWorkException e) {
            throw new RuntimeException("Could not open project transaction", e);
        }

        return "redirect:/project/" + id;
    }

}
