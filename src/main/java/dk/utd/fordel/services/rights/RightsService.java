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

        if (!roles.isEmpty()) {
            permissions.add(Permission.VIEW_PROJECT);
        }

        if (roles.contains(Roles.ADMINISTRATOR)) {
            permissions.add(Permission.VIEW_ADMIN_PAGE);
            permissions.add(Permission.GRANT_ROLES);
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