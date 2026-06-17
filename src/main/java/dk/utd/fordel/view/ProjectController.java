package dk.utd.fordel.view;

import dk.utd.fordel.domain.AdvisorAlreadyAdvises;
import dk.utd.fordel.domain.Project;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.domain.Project;
import dk.utd.fordel.services.Authenticator;
import dk.utd.fordel.services.Authenticator.Auth;
import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.services.UserManager;
import dk.utd.fordel.services.InvitationManager;
import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.services.rights.RightsContext;
import dk.utd.fordel.services.rights.RightsService;
import dk.utd.fordel.utils.Log;
import dk.utd.fordel.utils.UnitOfWork;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import dk.utd.fordel.domain.AdvisorAlreadyInvited;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ProjectController {
    private static final Logger logger = Log.getLogger(UserController.class);

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

    /**
     * GetMapping for the advisor project view side. The method first check if the session is valid.
     * The methods get all the current projects from the DB through the projectManager
     * and adds them to the model.
     * @return the path of an HTML side.
     */
    @GetMapping("/advisor-projects-view")
    public String getAllProjects(Model model, HttpSession session) {
        if (session.getAttribute("auth") == null) {
            return "redirect:/login";
        }
        Object userData = session.getAttribute("userData");
        RightsContext rights = rightsService.from(userData instanceof User.Data data ? data : null);
        if (!rightsService.canViewAllProjects(rights)) {
            return "redirect:/forbidden";
        }

        try (var uow = unitOfWork.begin()) {
            var projectList = projectManager.getAllProjects();
            model.addAttribute("title", "Projects");
            model.addAttribute("projects", projectList);
        } catch (Project.InvalidProject e) {
            throw new RuntimeException(e);
        } catch (UnitOfWork.UnitOfWorkException e) {
            throw Log.giveUp(logger, e);
        }

        return "advisor-projects-view";
    }

    /**
     * PostMapping for assigning advisors.
     * The method first check if the session is valid. I.e it checks whether the user is logged in,
     * and whether the user has the right role.
     * it then gets the current user, the clicked project, and then assigns the user as advisor on the project
     * through the projectManager which redirects the request to the database logic.
     * @return the path of an HTML side.
     */
    @PostMapping("/advisor-projects-view/assignAdvisor")
    public String assignAdvisor(HttpSession session, Model model,
                                @RequestParam("project_name") String projectName,
                                @RequestParam("project_description") String projectDescription,
                                @RequestParam("project_id") int projectId) {
        //Check if user is logged in
        if (session.getAttribute("auth") == null) {
            return "redirect:/login";
        }
        //Check if user has the right to perform action.
        Object userData = session.getAttribute("userData");
        RightsContext rights = rightsService.from(userData instanceof User.Data data ? data : null);
        if (!rightsService.canViewAllProjects(rights)) {
            return "redirect:/forbidden";
        }

        try (var uow = unitOfWork.begin()) {
            Authenticator.Auth auth = (Authenticator.Auth)session.getAttribute("auth");
            User user = auth.user();
            int userId = userManager.getUserIdByEmail(user.getEmail());
            Project project = new Project(projectName,projectDescription);
            project.setProjectID(projectId);
            projectManager.assignAdvisor(project,user);
            uow.commit();

        } catch (UnitOfWork.UnitOfWorkException e) {
            throw Log.giveUp(logger, e, "Failed to assign advisor");
        } catch (Project.InvalidProject e) {
            throw Log.giveUp(logger, e, "Could not assign advisor - invalid project" + projectName);
        } catch (AdvisorAlreadyAdvises e){
            try (var uow = unitOfWork.begin()) {
                model.addAttribute("errorMessage", "Advisor is already assigned to this project.");
                model.addAttribute("projects", projectManager.getAllProjects());
                uow.commit();
            } catch (Project.InvalidProject ex) {
                throw Log.giveUp(logger, ex, "Could not load projects");
            } catch (UnitOfWork.UnitOfWorkException ex) {
                throw Log.giveUp(logger, e, "Failed to load projects");
            }
            return "advisor-projects-view";
        }

        return "redirect:/";
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

            // load members that currently have access to this project
            try {
                String creatorEmail = projectManager.getProjectCreatorEmail(id);
                model.addAttribute("projectCreatorEmail", creatorEmail);
                var allUsers = userManager.getAllUsers();
                var members = allUsers.stream()
                        .filter(u -> {
                            try {
                                return projectManager.canViewProject(User.of(u.email()), id);
                            } catch (dk.utd.fordel.domain.Email.InvalidEmail e) {
                                return false;
                            }
                        })
                        .map(u -> new MemberCard(
                                u.name(),
                                u.roles(),
                                u.email(),
                                creatorEmail != null && creatorEmail.equalsIgnoreCase(u.email())))
                        .toList();
                model.addAttribute("members", members);
            } catch (Exception e) {
                model.addAttribute("members", java.util.List.of());
                model.addAttribute("projectCreatorEmail", null);
            }

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

    private static String formatRoles(List<Roles> roles) {
        if (roles == null || roles.isEmpty()) {
            return "No role";
        }
        return roles.stream()
                .map(r -> switch (r) {
                    case STUDENT -> "Student";
                    case ADVISOR -> "Advisor";
                    case TEACHER -> "Teacher";
                    case STUDY_LEADER -> "Study Leader";
                    case ADMINISTRATOR -> "Administrator";
                })
                .collect(Collectors.joining(", "));
    }

    public record MemberCard(String name, List<Roles> roles, String email, boolean creator) {
        public String rolesDisplay() {
            return formatRoles(roles);
        }

        public boolean kickable() {
            return !creator;
        }
    }

    @PostMapping("/project/{id}/members/kick")
    public String kickMember(@PathVariable int id,
                             @RequestParam("memberEmail") String memberEmail,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        Object authObject = session.getAttribute("auth");
        if (!(authObject instanceof Auth auth)) {
            return "redirect:/login";
        }

        Object userData = session.getAttribute("userData");
        RightsContext rights = rightsService.from(userData instanceof User.Data data ? data : null);
        if (!rightsService.canViewProject(rights)) {
            return "redirect:/forbidden";
        }

        try (var uow = unitOfWork.begin()) {
            if (!projectManager.canViewProject(auth.user(), id)) {
                return "redirect:/forbidden";
            }

            String creatorEmail = projectManager.getProjectCreatorEmail(id);
            String actorEmail = auth.user().getEmail().asString();

            // only project creator can kick, and creator cannot be kicked
            if (creatorEmail == null || !creatorEmail.equalsIgnoreCase(actorEmail)) {
                redirectAttributes.addFlashAttribute("error", "Only the project creator can kick members");
                uow.rollback();
                return "redirect:/project/" + id;
            }

            if (creatorEmail.equalsIgnoreCase(memberEmail)) {
                redirectAttributes.addFlashAttribute("error", "The project creator cannot be kicked");
                uow.rollback();
                return "redirect:/project/" + id;
            }

            try {
                var member = User.of(memberEmail);
                if (!projectManager.canViewProject(member, id)) {
                    redirectAttributes.addFlashAttribute("error", "Member does not have access to this project");
                    uow.rollback();
                    return "redirect:/project/" + id;
                }
                projectManager.kickMemberFromProject(id, member);
                uow.commit();
                redirectAttributes.addFlashAttribute("message", "Member was kicked from the project");
            } catch (dk.utd.fordel.domain.Email.InvalidEmail invalidEmail) {
                uow.rollback();
                redirectAttributes.addFlashAttribute("error", "Invalid member email");
            }
        } catch (UnitOfWork.UnitOfWorkException e) {
            throw new RuntimeException("Could not open project member-kick transaction", e);
        }

        return "redirect:/project/" + id;
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
