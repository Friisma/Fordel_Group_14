package dk.utd.fordel.view;

import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.domain.UserDoesNotExist;
import dk.utd.fordel.domain.Project;
import dk.utd.fordel.domain.AdvisorInvitation;
import dk.utd.fordel.domain.StudentInvitation;
import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.services.InvitationManager;
import dk.utd.fordel.services.UserManager;
import dk.utd.fordel.services.StudentProgressService;
import dk.utd.fordel.utils.UnitOfWork;
import dk.utd.fordel.services.Authenticator.Auth;
import dk.utd.fordel.services.rights.RightsContext;
import dk.utd.fordel.services.rights.RightsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Controller
public class HomepageController {

    private final RightsService rightsService;
    private final ProjectManager projectManager;
    private final InvitationManager invitationManager;
    private final UserManager userManager;
    private final StudentProgressService studentProgressService;
    private final UnitOfWork unitOfWork;

    public HomepageController(RightsService rightsService, ProjectManager projectManager,
            InvitationManager invitationManager, UserManager userManager,
            StudentProgressService studentProgressService, UnitOfWork unitOfWork) {
        this.rightsService = rightsService;
        this.projectManager = projectManager;
        this.invitationManager = invitationManager;
        this.userManager = userManager;
        this.studentProgressService = studentProgressService;
        this.unitOfWork = unitOfWork;
    }

    @GetMapping("/")
    public String getHomepage(Model model, HttpSession session) {
        if (session.getAttribute("auth") == null) {
            return "redirect:/login";
        }

        User.Data userData = sessionUserData(session);
        RightsContext rights = rightsService.from(userData);
        model.addAttribute("passwordChangedAt", null);
        model.addAttribute("passwordExpiresAt", null);
        model.addAttribute("showPasswordExpiryReminder", false);

        Object authObject = session.getAttribute("auth");
        if (authObject instanceof Auth auth) {
            try (var uow = unitOfWork.begin()) {
                LocalDateTime passwordChangedAt = userManager.getPasswordChangedAt(auth.user());
                LocalDateTime passwordExpiresAt = passwordChangedAt != null ? passwordChangedAt.plusMonths(3) : null;
                model.addAttribute("passwordChangedAt", passwordChangedAt);
                model.addAttribute("passwordExpiresAt", passwordExpiresAt);
                model.addAttribute("showPasswordExpiryReminder",
                        passwordExpiresAt != null
                                && !passwordExpiresAt.isAfter(LocalDateTime.now().plusDays(7))
                                && passwordExpiresAt.isAfter(LocalDateTime.now()));
                uow.commit();
            } catch (UserDoesNotExist e) {
                model.addAttribute("passwordChangedAt", null);
                model.addAttribute("passwordExpiresAt", null);
                model.addAttribute("showPasswordExpiryReminder", false);
            } catch (UnitOfWork.UnitOfWorkException e) {
                throw new RuntimeException("Could not load password change date", e);
            }
        }

        model.addAttribute("userData", userData);
        model.addAttribute("title", "Fordel");
        model.addAttribute("roleBadge", buildRoleBadge(rights));
        if (rightsService.canViewStudentProgress(rights)) {
            model.addAttribute("studentProgress", studentProgressService.build(userData));
        }
        model.addAttribute("sectionVisibility", new SectionVisibility(
                rightsService.canViewProject(rights),
                rights != null && rights.isAdvisor()));
        // load actual projects for this user
        if (authObject instanceof Auth auth) {
            try (var uow = unitOfWork.begin()) {
                var projects = projectManager.getAllProjectsRelatedToUser(auth.user());
                var projectCards = projects.stream().map(p -> new ProjectCard(
                        String.valueOf(p.getProjectID()),
                        p.getProjectName(),
                        p.getDescription() != null ? p.getDescription() : "",
                        p.getStudyLine() != null ? p.getStudyLine() : "",
                        p.getStatus().toString())).toList();
                model.addAttribute("projects", projectCards);
                uow.commit();
            } catch (Project.InvalidProject e) {
                model.addAttribute("projects", placeholderProjects());
            } catch (UnitOfWork.UnitOfWorkException e) {
                model.addAttribute("projects", placeholderProjects());
            }
        } else {
            model.addAttribute("projects", placeholderProjects());
        }
        model.addAttribute("advisorsMessage", "Advisors Section (not done)");
        // load advisor invitations for advisors
        if (rights != null && rights.isAdvisor()) {
            if (authObject instanceof Auth auth) {
                try (var uow = unitOfWork.begin()) {
                    try {
                        int advisorId = userManager.getUserIdByEmail(auth.user().getEmail());
                        var inviteViews = invitationManager.getInvitesReceivedForAdvisor(advisorId);
                        var inviteCards = inviteViews.stream().map(iv -> new InvitationCard(
                                iv.projectTitle(),
                                iv.status(),
                                iv.statusClass(),
                                String.valueOf(iv.projectId()),
                                iv.inviterName())).toList();
                        model.addAttribute("advisorInvitations", inviteCards);
                    } catch (Exception e) {
                        model.addAttribute("advisorInvitations", java.util.List.of());
                    }
                    uow.commit();
                } catch (UnitOfWork.UnitOfWorkException e) {
                    model.addAttribute("advisorInvitations", java.util.List.of());
                }
            } else {
                model.addAttribute("advisorInvitations", java.util.List.of());
            }
        }

        if (rights != null && rights.isStudent()) {
            if (authObject instanceof Auth auth) {
                try (var uow = unitOfWork.begin()) {
                    try {
                        var inviteViews = invitationManager.getAllStudentInvitesReceived(auth.user());
                        var inviteCards = inviteViews.stream().map(inv -> new InvitationCard(
                                "Project " + inv.getProjectId(),
                                inv.getStatusAsString(),
                                inv.getStatusAsString(),
                                String.valueOf(inv.getProjectId()),
                                "")).toList();
                        model.addAttribute("studentInvitations", inviteCards);
                    } catch (Exception e) {
                        model.addAttribute("studentInvitations", java.util.List.of());
                    }
                    uow.commit();
                } catch (UnitOfWork.UnitOfWorkException e) {
                    model.addAttribute("studentInvitations", java.util.List.of());
                }
            } else {
                model.addAttribute("studentInvitations", java.util.List.of());
            }
        }
        return "home";
    }

    @PostMapping("/invitations/{projectId}/accept")
    public String acceptInvitation(@PathVariable int projectId, HttpSession session,
            RedirectAttributes redirectAttributes) {
        return handleInvitationDecision(projectId, session, redirectAttributes, true);
    }

    @PostMapping("/invitations/{projectId}/decline")
    public String declineInvitation(@PathVariable int projectId, HttpSession session,
            RedirectAttributes redirectAttributes) {
        return handleInvitationDecision(projectId, session, redirectAttributes, false);
    }

    @PostMapping("/student-invitations/{projectId}/accept")
    public String acceptStudentInvitation(@PathVariable int projectId, HttpSession session,
            RedirectAttributes redirectAttributes) {
        return handleStudentInvitationDecision(projectId, session, redirectAttributes, true);
    }

    @PostMapping("/student-invitations/{projectId}/decline")
    public String declineStudentInvitation(@PathVariable int projectId, HttpSession session,
            RedirectAttributes redirectAttributes) {
        return handleStudentInvitationDecision(projectId, session, redirectAttributes, false);
    }

    private String handleInvitationDecision(int projectId, HttpSession session, RedirectAttributes redirectAttributes,
            boolean accept) {
        Object authObject = session.getAttribute("auth");
        if (!(authObject instanceof Auth auth)) {
            return "redirect:/login";
        }

        Object userData = session.getAttribute("userData");
        RightsContext rights = rightsService.from(userData instanceof User.Data data ? data : null);
        if (rights == null || !rights.isAdvisor()) {
            return "redirect:/forbidden";
        }

        try (var uow = unitOfWork.begin()) {
            var invites = invitationManager.getAllInvitesReceived(auth.user());
            AdvisorInvitation target = null;
            for (var inv : invites) {
                if (inv.getProjectId() == projectId) {
                    target = inv;
                    break;
                }
            }

            if (target == null) {
                redirectAttributes.addFlashAttribute("error", "Invitation not found");
                return "redirect:/";
            }

            try {
                if (accept) {
                    target.accept();
                    redirectAttributes.addFlashAttribute("message", "Invitation accepted");
                } else {
                    target.decline();
                    redirectAttributes.addFlashAttribute("message", "Invitation declined");
                }
                invitationManager.updateInvitation(target);
                uow.commit();
            } catch (RuntimeException e) {
                uow.rollback();
                redirectAttributes.addFlashAttribute("error", "Invitation already handled");
            }
        } catch (UnitOfWork.UnitOfWorkException e) {
            throw new RuntimeException("Could not update invitation status", e);
        }

        return "redirect:/";
    }

    private String handleStudentInvitationDecision(int projectId, HttpSession session,
            RedirectAttributes redirectAttributes, boolean accept) {
        Object authObject = session.getAttribute("auth");
        if (!(authObject instanceof Auth auth)) {
            return "redirect:/login";
        }

        Object userData = session.getAttribute("userData");
        RightsContext rights = rightsService.from(userData instanceof User.Data data ? data : null);
        if (rights == null || !rights.isStudent()) {
            return "redirect:/forbidden";
        }

        try (var uow = unitOfWork.begin()) {
            var invites = invitationManager.getAllStudentInvitesReceived(auth.user());
            StudentInvitation target = null;
            for (var inv : invites) {
                if (inv.getProjectId() == projectId) {
                    target = inv;
                    break;
                }
            }

            if (target == null) {
                redirectAttributes.addFlashAttribute("error", "Invitation not found");
                return "redirect:/";
            }

            try {
                if (accept) {
                    target.accept();
                    redirectAttributes.addFlashAttribute("message", "Invitation accepted");
                } else {
                    target.decline();
                    redirectAttributes.addFlashAttribute("message", "Invitation declined");
                }
                invitationManager.updateStudentInvitation(target);
                uow.commit();
            } catch (RuntimeException e) {
                uow.rollback();
                redirectAttributes.addFlashAttribute("error", "Invitation already handled");
            }
        } catch (UnitOfWork.UnitOfWorkException e) {
            throw new RuntimeException("Could not update student invitation status", e);
        }

        return "redirect:/";
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty())
            return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private User.Data sessionUserData(HttpSession session) {
        Object userData = session.getAttribute("userData");
        if (userData instanceof User.Data data) {
            return data;
        }

        return null;
    }

    private List<ProjectCard> placeholderProjects() {
        return List.of(
                new ProjectCard(
                        "1",
                        "Project 1",
                        "Placeholder project description.",
                        "Software Technology",
                        "p.getStatus().toString()"),
                new ProjectCard(
                        "2",
                        "Project 2",
                        "Placeholder project description.",
                        "Software Technology",
                        "p.getStatus().toString()))"));
    }

    private List<InvitationCard> placeholderInvitations() {
        return List.of(
                new InvitationCard("Peter Sterner", "Pending", "pending", "", ""),
                new InvitationCard("Hans Henrik", "Accepted", "accepted", "", ""),
                new InvitationCard("Mišo Kovač", "Declined", "declined", "", ""));
    }

    private String buildRoleBadge(RightsContext rights) {
        if (rights == null || rights.roles().isEmpty()) {
            return "No role";
        }

        return rights.roles().stream()
                .sorted(Comparator.comparingInt(Roles::getClearanceLevel))
                .map(HomepageController::displayName)
                .collect(Collectors.joining(", "));
    }

    private static String displayName(Roles role) {
        if (role == Roles.STUDENT) {
            return "Student";
        }
        if (role == Roles.ADVISOR) {
            return "Advisor";
        }
        if (role == Roles.TEACHER) {
            return "Teacher";
        }
        if (role == Roles.STUDY_LEADER) {
            return "Study Leader";
        }
        return "Administrator";
    }

    public record ProjectCard(String id, String title, String description, String studyLine, String status) {
    }

    public record InvitationCard(String name, String status, String statusClass, String projectId, String inviterName) {
    }

    public record SectionVisibility(boolean projectsVisible, boolean advisorsVisible) {
    }
}