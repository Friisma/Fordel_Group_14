package dk.utd.fordel.services.rights;

import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.domain.User;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

@Service
public class RightsService {

    public RightsContext from(User.Data userData) {
        if (userData == null) {
            return RightsContext.anonymous();
        }

        Set<Roles> roles = toEnumSet(userData.roles(), Roles.class);

        Set<Permission> permissions = EnumSet.noneOf(Permission.class);

        permissions.add(Permission.VIEW_HOME);
        permissions.add(Permission.EDIT_OWN_PROFILE);
        permissions.add(Permission.MANAGE_STUDY_LINE);
        permissions.add(Permission.CAN_EDIT_PROJECT);

        if (!roles.isEmpty()) {
            permissions.add(Permission.VIEW_PROJECT);
        }

        if (roles.contains(Roles.ADMINISTRATOR)) {
            permissions.add(Permission.VIEW_ADMIN_PAGE);
            permissions.add(Permission.GRANT_ROLES);
        }

        if (roles.contains(Roles.ADVISOR)) {
            permissions.add(Permission.ADVISOR_DASHBOARD);
            permissions.add(Permission.VIEW_ALL_PROJECTS);
            permissions.add(Permission.CAN_EDIT_STARTED_PROJECT);
            permissions.add(Permission.CAN_EDIT_PROJECT);
        }

        return new RightsContext(true, roles, permissions);
    }

    public boolean canViewAdminPage(RightsContext rights) {
        return rights != null && rights.hasPermission(Permission.VIEW_ADMIN_PAGE);
    }

    public boolean canGrantRoles(RightsContext rights) {
        return rights != null && rights.hasPermission(Permission.GRANT_ROLES);
    }

    public boolean canEditOwnProfile(RightsContext rights) {
        return rights != null && rights.hasPermission(Permission.EDIT_OWN_PROFILE);
    }

    public boolean canViewProject(RightsContext rights) {
        return rights != null && rights.hasPermission(Permission.VIEW_PROJECT);
    }

    public boolean canViewAdvisorDashboard(RightsContext rights) {
        return rights != null && rights.hasPermission(Permission.ADVISOR_DASHBOARD);
    }

    public boolean canViewAllProjects(RightsContext rights) {
        return rights != null && rights.hasPermission(Permission.VIEW_ALL_PROJECTS);
    }

    public boolean canEditStartedProject(RightsContext rights) {
        return rights != null && rights.hasPermission(Permission.CAN_EDIT_STARTED_PROJECT);
    }

    public boolean canManageStudyLine(RightsContext rights) {
        return rights != null && rights.hasPermission(Permission.MANAGE_STUDY_LINE);
    }


    private static <E extends Enum<E>> Set<E> toEnumSet(Iterable<E> values, Class<E> enumType) {
        if (values == null) {
            return Set.of();
        }

        EnumSet<E> copy = EnumSet.noneOf(enumType);
        for (E value : values) {
            if (value != null) {
                copy.add(value);
            }
        }

        if (copy.isEmpty()) {
            return Set.of();
        }

        return Set.copyOf(copy);
    }
}