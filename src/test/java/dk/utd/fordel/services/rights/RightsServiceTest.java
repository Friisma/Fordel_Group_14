package dk.utd.fordel.services.rights;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.domain.User;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class RightsServiceTest {

    private final RightsService rightsService = new RightsService();

    @Test
    void student_gets_the_expected_baseline_permissions() {
        RightsContext rights = rightsService.from(new User.Data("Student", List.of(Roles.STUDENT), null, null));

        assertTrue(rights.isStudent());
        assertTrue(rights.hasPermission(Permission.VIEW_HOME));
        assertTrue(rights.hasPermission(Permission.EDIT_OWN_PROFILE));
        assertTrue(rights.hasPermission(Permission.CAN_EDIT_PROJECT));
        assertTrue(rights.hasPermission(Permission.VIEW_STUDY_LINE));
        assertTrue(rights.hasPermission(Permission.VIEW_PROJECT));
        assertTrue(rights.hasPermission(Permission.VIEW_USER));
        assertFalse(rights.hasPermission(Permission.VIEW_ADMIN_PAGE));
        assertFalse(rights.hasPermission(Permission.GRANT_ROLES));
        assertFalse(rights.hasPermission(Permission.ADVISOR_DASHBOARD));
        assertFalse(rights.hasPermission(Permission.MANAGE_STUDY_LINE));
    }

    @Test
    void advisor_gets_advisor_permissions_and_baseline_view_rights() {
        RightsContext rights = rightsService.from(new User.Data("Advisor", List.of(Roles.ADVISOR), null, null));

        assertTrue(rights.isAdvisor());
        assertTrue(rights.hasPermission(Permission.VIEW_HOME));
        assertTrue(rights.hasPermission(Permission.EDIT_OWN_PROFILE));
        assertTrue(rights.hasPermission(Permission.CAN_EDIT_PROJECT));
        assertTrue(rights.hasPermission(Permission.VIEW_PROJECT));
        assertTrue(rights.hasPermission(Permission.VIEW_USER));
        assertTrue(rights.hasPermission(Permission.ADVISOR_DASHBOARD));
        assertTrue(rights.hasPermission(Permission.VIEW_ALL_PROJECTS));
        assertTrue(rights.hasPermission(Permission.CAN_START_PROJECT));
        assertTrue(rights.hasPermission(Permission.CAN_EDIT_STARTED_PROJECT));
        assertFalse(rights.hasPermission(Permission.VIEW_ADMIN_PAGE));
        assertFalse(rights.hasPermission(Permission.GRANT_ROLES));
        assertFalse(rights.hasPermission(Permission.CAN_EDIT_STUDENT));
    }

    @Test
    void study_leader_gets_study_line_management_permissions() {
        RightsContext rights = rightsService.from(new User.Data("Leader", List.of(Roles.STUDY_LEADER), null, null));

        assertTrue(rights.isStudyLeader());
        assertTrue(rights.hasPermission(Permission.VIEW_HOME));
        assertTrue(rights.hasPermission(Permission.VIEW_PROJECT));
        assertTrue(rights.hasPermission(Permission.VIEW_USER));
        assertTrue(rights.hasPermission(Permission.MANAGE_STUDY_LINE));
        assertTrue(rights.hasPermission(Permission.VIEW_STUDENT_PROJECTS));
        assertFalse(rights.hasPermission(Permission.VIEW_ADMIN_PAGE));
        assertFalse(rights.hasPermission(Permission.GRANT_ROLES));
        assertFalse(rights.hasPermission(Permission.ADVISOR_DASHBOARD));
    }

    @Test
    void administrator_gets_admin_and_study_line_permissions() {
        RightsContext rights = rightsService.from(new User.Data("Admin", List.of(Roles.ADMINISTRATOR), null, null));

        assertTrue(rights.isAdministrator());
        assertTrue(rights.hasPermission(Permission.VIEW_ADMIN_PAGE));
        assertTrue(rights.hasPermission(Permission.GRANT_ROLES));
        assertTrue(rights.hasPermission(Permission.CAN_EDIT_STUDENT));
        assertTrue(rights.hasPermission(Permission.MANAGE_STUDY_LINE));
        assertTrue(rights.hasPermission(Permission.VIEW_STUDENT_PROJECTS));
        assertTrue(rights.hasPermission(Permission.VIEW_PROJECT));
        assertTrue(rights.hasPermission(Permission.VIEW_USER));
    }

    @Test
    void multiple_roles_combine_permissions_instead_of_overriding_each_other() {
        RightsContext rights = rightsService.from(new User.Data(
                "Mixed",
                List.of(Roles.STUDENT, Roles.ADVISOR, Roles.STUDY_LEADER),
                null,
                null));

        assertTrue(rights.isStudent());
        assertTrue(rights.isAdvisor());
        assertTrue(rights.isStudyLeader());
        assertTrue(rights.hasPermission(Permission.VIEW_USER));
        assertTrue(rights.hasPermission(Permission.ADVISOR_DASHBOARD));
        assertTrue(rights.hasPermission(Permission.VIEW_ALL_PROJECTS));
        assertTrue(rights.hasPermission(Permission.MANAGE_STUDY_LINE));
        assertTrue(rights.hasPermission(Permission.VIEW_STUDENT_PROJECTS));
        assertTrue(rights.hasPermission(Permission.CAN_START_PROJECT));
    }

    @Test
    void null_roles_in_user_data_are_ignored() {
        List<Roles> roles = new ArrayList<>();
        roles.add(Roles.ADVISOR);
        roles.add(null);

        RightsContext rights = rightsService.from(new User.Data("Mixed", roles, null, null));

        assertTrue(rights.isAdvisor());
        assertTrue(rights.hasPermission(Permission.ADVISOR_DASHBOARD));
        assertTrue(rights.hasPermission(Permission.VIEW_ALL_PROJECTS));
        assertFalse(rights.hasPermission(Permission.VIEW_ADMIN_PAGE));
    }

    @Test
    void null_user_data_resolves_to_anonymous_rights() {
        RightsContext rights = rightsService.from(null);

        assertFalse(rights.isStudent());
        assertFalse(rights.isAdvisor());
        assertFalse(rights.isStudyLeader());
        assertFalse(rights.hasPermission(Permission.VIEW_USER));
        assertFalse(rights.hasPermission(Permission.VIEW_PROJECT));
        assertFalse(rights.hasPermission(Permission.MANAGE_STUDY_LINE));
    }

    @Test
    void it_does_not_grant_student_visibility_to_users_without_roles() {
        RightsContext rights = rightsService.from(new User.Data("No Roles", List.of(), null, null));

        assertFalse(rights.hasPermission(Permission.VIEW_USER));
        assertFalse(rights.hasPermission(Permission.VIEW_PROJECT));
        assertFalse(rights.hasPermission(Permission.VIEW_ADMIN_PAGE));
        assertFalse(rights.hasPermission(Permission.ADVISOR_DASHBOARD));
        assertFalse(rights.hasPermission(Permission.MANAGE_STUDY_LINE));
    }

    @Test
    void it_does_not_grant_student_specific_rights_to_anonymous_users() {
        RightsContext rights = RightsContext.anonymous();

        assertFalse(rights.hasPermission(Permission.VIEW_USER));
        assertFalse(rights.hasPermission(Permission.CAN_EDIT_STUDENT));
        assertFalse(rights.hasPermission(Permission.VIEW_STUDENT_PROJECTS));
        assertFalse(rights.hasRole(Roles.STUDENT));
        assertFalse(rights.hasRole(Roles.ADVISOR));
    }
}