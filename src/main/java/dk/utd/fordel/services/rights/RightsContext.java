package dk.utd.fordel.services.rights;

import dk.utd.fordel.domain.Roles;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public record RightsContext(
        boolean authenticated,
        Set<Roles> roles,
        Set<Permission> permissions) {

    public RightsContext {
        roles = copyEnumSet(roles, Roles.class);
        permissions = copyEnumSet(permissions, Permission.class);
    }

    private static <E extends Enum<E>> Set<E> copyEnumSet(Set<E> values, Class<E> enumType) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }

        EnumSet<E> copy = EnumSet.noneOf(enumType);
        copy.addAll(values);
        return Collections.unmodifiableSet(copy);
    }

    public boolean hasRole(Roles role) {
        return roles.contains(role);
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    public boolean isAdministrator() {
        return hasRole(Roles.ADMINISTRATOR);
    }

    public boolean isStudyLeader() {
        return hasRole(Roles.STUDY_LEADER);
    }

    public boolean isTeacher() {
        return hasRole(Roles.TEACHER);
    }

    public boolean isAdvisor() {
        return hasRole(Roles.ADVISOR);
    }

    public boolean isStudent() {
        return hasRole(Roles.STUDENT);
    }

    public static RightsContext anonymous() {
        return new RightsContext(false, Set.of(), Set.of());
    }
}