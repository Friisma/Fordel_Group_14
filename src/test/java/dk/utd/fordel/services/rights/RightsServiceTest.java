package dk.utd.fordel.services.rights;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.domain.User;
import org.junit.jupiter.api.Test;

import java.util.List;

class RightsServiceTest {

    private final RightsService rightsService = new RightsService();

    @Test
    void it_grants_baseline_student_visibility_to_any_authenticated_role() {
        RightsContext rights = rightsService.from(new User.Data("Student", List.of(Roles.STUDENT), null, null));

        assertTrue(rights.hasPermission(Permission.VIEW_STUDENT));
    }

    @Test
    void it_grants_admin_student_edit_rights() {
        RightsContext rights = rightsService.from(new User.Data("Admin", List.of(Roles.ADMINISTRATOR), null, null));

        assertTrue(rights.hasPermission(Permission.CAN_EDIT_STUDENT));
    }

    @Test
    void it_grants_study_leader_student_project_visibility() {
        RightsContext rights = rightsService.from(new User.Data("Leader", List.of(Roles.STUDY_LEADER), null, null));

        assertTrue(rights.hasPermission(Permission.VIEW_STUDENT_PROJECTS));
    }

    @Test
    void it_does_not_grant_student_specific_rights_to_anonymous_users() {
        RightsContext rights = RightsContext.anonymous();

        assertFalse(rights.hasPermission(Permission.VIEW_STUDENT));
        assertFalse(rights.hasPermission(Permission.CAN_EDIT_STUDENT));
        assertFalse(rights.hasPermission(Permission.VIEW_STUDENT_PROJECTS));
    }
}