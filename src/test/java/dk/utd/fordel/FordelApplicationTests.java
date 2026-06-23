package dk.utd.fordel;

import dk.utd.fordel.domain.Project;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.utils.UnitOfWork;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-integrationtest.properties")
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD) // Reset the database before each test
class FordelApplicationTests {

	@Autowired
	MockMvc mvc;

	@Autowired
	ProjectManager projectManager;

	@Autowired
	UnitOfWork unitOfWork;

	@Test
	void context_loads() {
		// Note this only works wthen the main application is NOT running.
		// If the main application is running, the database is locked.
	}

	@Test
	void we_can_create_and_login_as_a_user() throws Exception {
		mvc.perform(post("/create-user")
				.param("name", "Testy McTestface")
				.param("email", "test@test.dk")
				.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"));

		mvc.perform(post("/login")
				.param("email", "test@test.dk")
				.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"));
	}

	@Test
	void when_we_create_a_user_we_are_logged_in() throws Exception {
		MvcResult res = mvc.perform(post("/create-user")
				.param("name", "Testy McTestface")
				.param("email", "test@test.dk")
				.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/")).andReturn();

		MockHttpSession sessionId = (MockHttpSession) res.getRequest().getSession();
		assertNotNull(sessionId);

		mvc.perform(get("/")
				.session(sessionId))
				.andExpect(status().isOk());

	}

	@Test
	void we_cannot_create_the_same_user_twice() throws Exception {
		mvc.perform(post("/create-user")
				.param("name", "Testy McTestface")
				.param("email", "test@test.dk")
				.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"));

		MvcResult res = mvc.perform(post("/create-user")
				.param("name", "Testy McTestface")
				.param("email", "test@test.dk")
				.param("password", "password"))
				.andExpect(status().isOk())
				.andReturn();

		assertTrue(res.getResponse().getContentAsString().contains("Email already in use"));
	}

	@Test
	void we_can_open_create_study_line_page() throws Exception {
		MvcResult res = mvc.perform(get("/create-study-line"))
				.andExpect(status().isOk())
				.andReturn();

		assertTrue(res.getResponse().getContentAsString().contains("Create Study Line"));
	}

	@Test
	void we_cannot_create_the_same_study_line_twice() throws Exception {
		mvc.perform(post("/create-study-line")
				.param("lineName", "Software Technology"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"));

		MvcResult res = mvc.perform(post("/create-study-line")
				.param("lineName", "Software Technology"))
				.andExpect(status().isOk())
				.andReturn();

		assertTrue(res.getResponse().getContentAsString().contains("Study line already exists"));
	}

	@Test
	void when_login_credentials_are_invalid_then_we_stay_on_login_with_error() throws Exception {
		MvcResult res = mvc.perform(post("/login")
				.param("email", "admin@test.dk")
				.param("password", "wrong-password"))
				.andExpect(status().isOk())
				.andExpect(view().name("login"))
				.andExpect(content().string(containsString("Du har indtastet forkert password eller email")))
				.andReturn();

		assertTrue(res.getResponse().getContentAsString().contains("Du har indtastet forkert password eller email"));
	}

	@Test
	void when_login_email_is_invalid_then_we_stay_on_login_with_email_error() throws Exception {
		mvc.perform(post("/login")
				.param("email", "invalid-email")
				.param("password", "password"))
				.andExpect(status().isOk())
				.andExpect(view().name("login"))
				.andExpect(content().string(containsString("should have @ sign")));
	}

	@Test
	void when_create_user_email_is_invalid_then_we_stay_on_create_user_with_email_error() throws Exception {
		mvc.perform(post("/create-user")
				.param("name", "Invalid Email User")
				.param("email", "invalid-email")
				.param("password", "password"))
				.andExpect(status().isOk())
				.andExpect(view().name("create-user"))
				.andExpect(content().string(containsString("should have @ sign")));
	}

	@Test
	void when_create_user_name_is_empty_then_we_stay_on_create_user_with_name_error() throws Exception {
		mvc.perform(post("/create-user")
				.param("name", "")
				.param("email", "empty-name@test.dk")
				.param("password", "password"))
				.andExpect(status().isOk())
				.andExpect(view().name("create-user"))
				.andExpect(content().string(containsString("Username cannot be empty")));
	}

	@Test
	void when_create_user_password_is_empty_then_we_stay_on_create_user() throws Exception {
		mvc.perform(post("/create-user")
				.param("name", "Empty Password User")
				.param("email", "empty-password@test.dk")
				.param("password", ""))
				.andExpect(status().isOk())
				.andExpect(view().name("create-user"));
	}

	@Test
	void when_password_is_expired_then_protected_pages_redirect_to_password_expired() throws Exception {
		MvcResult loginRes = mvc.perform(post("/login")
				.param("email", "expired@test.dk")
				.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		MockHttpSession expiredSession = (MockHttpSession) loginRes.getRequest().getSession();
		assertNotNull(expiredSession);

		mvc.perform(get("/")
				.session(expiredSession))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/password-expired"));

		mvc.perform(get("/update-password")
				.session(expiredSession))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Update Password")));
	}

	@Test
	void when_password_is_expired_then_allowlisted_routes_still_work() throws Exception {
		MvcResult loginRes = mvc.perform(post("/login")
				.param("email", "expired@test.dk")
				.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		MockHttpSession expiredSession = (MockHttpSession) loginRes.getRequest().getSession();
		assertNotNull(expiredSession);

		mvc.perform(get("/login")
				.session(expiredSession))
				.andExpect(status().isOk())
				.andExpect(view().name("login"));

		mvc.perform(get("/password-expired")
				.session(expiredSession))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Password Expired!")));

		mvc.perform(get("/logout")
				.session(expiredSession))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	void when_expired_user_changes_password_then_user_can_access_homepage() throws Exception {
		MvcResult loginRes = mvc.perform(post("/login")
				.param("email", "expired@test.dk")
				.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		MockHttpSession expiredSession = (MockHttpSession) loginRes.getRequest().getSession();
		assertNotNull(expiredSession);

		mvc.perform(get("/")
				.session(expiredSession))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/password-expired"));

		mvc.perform(post("/change-password")
				.session(expiredSession)
				.param("currentPassword", "password")
				.param("newPassword", "renewed-password")
				.param("confirmNewPassword", "renewed-password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"));

		mvc.perform(get("/")
				.session(expiredSession))
				.andExpect(status().isOk());
	}

	@Test
	void when_password_is_changed_then_old_password_fails_and_new_password_works() throws Exception {
		MvcResult createUserRes = mvc.perform(post("/create-user")
				.param("name", "Password Flow User")
				.param("email", "password-flow@test.dk")
				.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		MockHttpSession userSession = (MockHttpSession) createUserRes.getRequest().getSession();
		assertNotNull(userSession);

		mvc.perform(post("/change-password")
				.session(userSession)
				.param("currentPassword", "password")
				.param("newPassword", "new-password")
				.param("confirmNewPassword", "new-password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"));

		mvc.perform(post("/login")
				.param("email", "password-flow@test.dk")
				.param("password", "password"))
				.andExpect(status().isOk())
				.andExpect(view().name("login"))
				.andExpect(content().string(containsString("Du har indtastet forkert password eller email")));

		mvc.perform(post("/login")
				.param("email", "password-flow@test.dk")
				.param("password", "new-password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"));
	}

	@Test
	void when_update_password_is_requested_without_login_then_redirect_to_login() throws Exception {
		mvc.perform(get("/update-password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/login"));
	}

	@Test
	void when_change_password_confirmation_does_not_match_then_show_validation_error() throws Exception {
		MvcResult createUserRes = mvc.perform(post("/create-user")
				.param("name", "Mismatch Password User")
				.param("email", "mismatch-password@test.dk")
				.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		MockHttpSession userSession = (MockHttpSession) createUserRes.getRequest().getSession();
		assertNotNull(userSession);

		mvc.perform(post("/change-password")
				.session(userSession)
				.param("currentPassword", "password")
				.param("newPassword", "new-password")
				.param("confirmNewPassword", "different-password"))
				.andExpect(status().isOk())
				.andExpect(view().name("update-password"))
				.andExpect(content().string(containsString("Passwords do not match")));
	}

	@Test
	void when_current_password_is_wrong_then_change_password_fails() throws Exception {
		MvcResult createUserRes = mvc.perform(post("/create-user")
				.param("name", "Wrong Current Password User")
				.param("email", "wrong-current-password@test.dk")
				.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		MockHttpSession userSession = (MockHttpSession) createUserRes.getRequest().getSession();
		assertNotNull(userSession);

		mvc.perform(post("/change-password")
				.session(userSession)
				.param("currentPassword", "wrong-password")
				.param("newPassword", "new-password")
				.param("confirmNewPassword", "new-password"))
				.andExpect(status().isOk())
				.andExpect(view().name("update-password"))
				.andExpect(content().string(containsString("Current password is incorrect")));
	}

	@Test
	void we_can_create_a_project_and_view_it_on_the_project_page() throws Exception {
		MvcResult loginUserRes = mvc.perform(post("/login")
						.param("email", "student@test.dk")
						.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		MockHttpSession userSession = (MockHttpSession) loginUserRes.getRequest().getSession();
		assertNotNull(userSession);

		mvc.perform(post("/create-project")
						.session(userSession)
						.param("project_name", "Test project")
						.param("project_description", "This is a test project")
						.param("study_line", "Test Study Line"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"));

		int projectId;
		try (var uow = unitOfWork.begin()) {
			User student = User.of("student@test.dk");
			var studentProjects = projectManager.getAllProjectsRelatedToUser(student);

			Project createdProject = studentProjects.stream()
					.filter(p -> p.getProjectName().equals("Test project"))
					.findFirst()
					.orElseThrow(() -> new AssertionError("Project was not found"));

			projectId = createdProject.getProjectID();
			uow.commit();
		}

		mvc.perform(get("/project/" + projectId)
						.session(userSession))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Test project")));
	}

	@Test
	void when_create_project_with_empty_title_then_stay_on_page_with_error() throws Exception {
		MvcResult loginUserRes = mvc.perform(post("/login")
						.param("email", "student@test.dk")
						.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		MockHttpSession userSession = (MockHttpSession) loginUserRes.getRequest().getSession();
		assertNotNull(userSession);

		mvc.perform(post("/create-project")
						.session(userSession)
						.param("project_name", "")
						.param("project_description", "This is a test project")
						.param("study_line", "Test Study Line"))
				.andExpect(view().name("create-project"))
				.andExpect(content().string(containsString("Cant create project")));

	}

	@Test
	void when_project_is_draft_creator_can_edit_it() throws Exception {
		MvcResult loginUserRes = mvc.perform(post("/login")
						.param("email", "student@test.dk")
						.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		MockHttpSession userSession = (MockHttpSession) loginUserRes.getRequest().getSession();
		assertNotNull(userSession);

		mvc.perform(post("/create-project")
						.session(userSession)
						.param("project_name", "Test project")
						.param("project_description", "This is a test project")
						.param("study_line", "Test Study Line"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"));


		int projectId;
		try (var uow = unitOfWork.begin()) {
			User student = User.of("student@test.dk");
			var studentProjects = projectManager.getAllProjectsRelatedToUser(student);

			Project createdProject = studentProjects.stream()
					.filter(p -> p.getProjectName().equals("Test project"))
					.findFirst()
					.orElseThrow(() -> new AssertionError("Project was not found"));

			projectId = createdProject.getProjectID();
			uow.commit();
		}

		mvc.perform(post("/project/" + projectId + "/updateProject")
						.session(userSession)
						.param("id", String.valueOf(projectId))
						.param("newTitle", "Test project still")
						.param("newDescription", "This is still a test project"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/project/" + projectId));

		mvc.perform(get("/project/" + projectId)
						.session(userSession))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Test project still")));
	}

	@Test
	void when_non_member_accesses_project_then_forbidden() throws Exception {
		MvcResult loginUserRes = mvc.perform(post("/login")
						.param("email", "student@test.dk")
						.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		MockHttpSession userSession = (MockHttpSession) loginUserRes.getRequest().getSession();
		assertNotNull(userSession);

		mvc.perform(post("/create-project")
						.session(userSession)
						.param("project_name", "Test project")
						.param("project_description", "This is a test project")
						.param("study_line", "Test Study Line"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"));


		int projectId;
		try (var uow = unitOfWork.begin()) {
			User student = User.of("student@test.dk");
			var studentProjects = projectManager.getAllProjectsRelatedToUser(student);

			Project createdProject = studentProjects.stream()
					.filter(p -> p.getProjectName().equals("Test project"))
					.findFirst()
					.orElseThrow(() -> new AssertionError("Project was not found"));

			projectId = createdProject.getProjectID();
			uow.commit();
		}

		loginUserRes = mvc.perform(post("/login")
						.param("email", "advisor@test.dk")
						.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		userSession = (MockHttpSession) loginUserRes.getRequest().getSession();
		assertNotNull(userSession);

		mvc.perform(get("/project/" + projectId)
						.session(userSession))
				.andExpect(redirectedUrl("/forbidden"));
	}

	@Test
	void admin_can_delete_user() throws Exception {
		mvc.perform(post("/create-user")
						.param("name", "User to delete")
						.param("email", "deleteuser@test.dk")
						.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"));

		MvcResult loginUserRes = mvc.perform(post("/login")
						.param("email", "admin@test.dk")
						.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		MockHttpSession userSession = (MockHttpSession) loginUserRes.getRequest().getSession();
		assertNotNull(userSession);

		mvc.perform(post("/adminpage/delete-user")
						.session(userSession)
						.param("email", "deleteuser@test.dk"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/adminpage"));

		mvc.perform(post("/login")
						.param("email", "deleteuser@test.dk")
						.param("password", "password"))
				.andExpect(status().isOk())
				.andExpect(view().name("login"))
				.andExpect(model().attributeExists("error_password"));
	}

	@Test
	void admin_can_grant_and_remove_roles_from_users() throws Exception {

	}

	@Test
	void advisor_can_create_project_and_view_projects() throws Exception {
		MvcResult loginUserRes = mvc.perform(post("/login")
						.param("email", "advisor@test.dk")
						.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		MockHttpSession userSession = (MockHttpSession) loginUserRes.getRequest().getSession();
		assertNotNull(userSession);

		mvc.perform(post("/create-advisor-project")
						.session(userSession)
						.param("project_name", "")
						.param("project_description", "This is a test project")
						.param("study_line", "Test Study Line"))
				.andExpect(status().isOk())
				.andExpect(view().name("create-project"))
				.andExpect(model().attributeExists("project_name"));

		mvc.perform(post("/create-advisor-project")
						.session(userSession)
						.param("project_name", "Test project")
						.param("project_description", "")
						.param("study_line", "Test Study Line"))
				.andExpect(status().isOk())
				.andExpect(view().name("create-project"))
				.andExpect(model().attributeExists("project_description"));

		mvc.perform(post("/create-advisor-project")
						.session(userSession)
						.param("project_name", "Test project")
						.param("project_description", "This is a test project")
						.param("study_line", "Test Study Line"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"));

		mvc.perform(get("/projects")
						.session(userSession))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Test project")));
	}

	@Test
	void create_project_without_any_title_or_description() throws Exception {
		MvcResult loginUserRes = mvc.perform(post("/login")
						.param("email", "advisor@test.dk")
						.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		MockHttpSession userSession = (MockHttpSession) loginUserRes.getRequest().getSession();
		assertNotNull(userSession);

		mvc.perform(post("/create-project")
						.session(userSession)
						.param("project_name", "")
						.param("project_description", "This is a test project")
						.param("study_line", "Test Study Line"))
				.andExpect(status().isOk())
				.andExpect(view().name("create-project"))
				.andExpect(model().attributeExists("project_name"));

		mvc.perform(post("/create-project")
						.session(userSession)
						.param("project_name", "Test project")
						.param("project_description", "")
						.param("study_line", "Test Study Line"))
				.andExpect(status().isOk())
				.andExpect(view().name("create-project"))
				.andExpect(model().attributeExists("project_description"));
	}

	@Test
	void view_study_lines_and_view_study_line_page_with_projects() throws Exception {
		MvcResult loginUserRes = mvc.perform(post("/login")
						.param("email", "student@test.dk")
						.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		MockHttpSession userSession = (MockHttpSession) loginUserRes.getRequest().getSession();
		assertNotNull(userSession);

		mvc.perform(get("/studylines")
						.session(userSession))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Test Study Line")));

		mvc.perform(post("/create-project")
						.session(userSession)
						.param("project_name", "Test project")
						.param("project_description", "This is a test project")
						.param("study_line", "Test Study Line"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"));

		mvc.perform(get("/studyline/1")
						.session(userSession))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Test Study Line")))
				.andExpect(content().string(containsString("Test project")));

		mvc.perform(get("/studyline/1/members")
						.session(userSession))
				.andExpect(status().isOk())
				.andExpect(view().name("studyline-members"))
				.andExpect(model().attributeExists("studyLineMembers"));
	}


	@Test
	void only_study_leader_can_access_manage_study_line() throws Exception {
		MvcResult loginUserRes = mvc.perform(post("/login")
						.param("email", "studyleader@test.dk")
						.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		MockHttpSession userSession = (MockHttpSession) loginUserRes.getRequest().getSession();
		assertNotNull(userSession);

		mvc.perform(get("/studyline/1/manage")
						.session(userSession))
				.andExpect(status().isOk())
				.andExpect(view().name("manage-study-line"));

		loginUserRes = mvc.perform(post("/login")
						.param("email", "student@test.dk")
						.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		userSession = (MockHttpSession) loginUserRes.getRequest().getSession();
		assertNotNull(userSession);

		mvc.perform(get("/studyline/1/manage")
						.session(userSession))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/forbidden"));
	}

	@Test
	void study_leader_can_add_and_remove_users_from_study_line() throws Exception {
		MvcResult loginUserRes = mvc.perform(post("/login")
						.param("email", "studyleader@test.dk")
						.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		MockHttpSession userSession = (MockHttpSession) loginUserRes.getRequest().getSession();
		assertNotNull(userSession);

		mvc.perform(post("/studyline/1/manage/remove-user")
						.session(userSession)
						.param("emailToRemove", "student@test.dk"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/studyline/1/manage"));

		loginUserRes = mvc.perform(post("/login")
						.param("email", "student@test.dk")
						.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		userSession = (MockHttpSession) loginUserRes.getRequest().getSession();
		assertNotNull(userSession);

		mvc.perform(get("/studyline/1")
						.session(userSession))
				.andExpect(redirectedUrl("/forbidden"));

		loginUserRes = mvc.perform(post("/login")
						.param("email", "studyleader@test.dk")
						.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		userSession = (MockHttpSession) loginUserRes.getRequest().getSession();
		assertNotNull(userSession);

		mvc.perform(post("/studyline/1/manage/add-student")
						.session(userSession)
						.param("emailToAdd", "student@test.dk"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/studyline/1/manage"));

		loginUserRes = mvc.perform(post("/login")
						.param("email", "student@test.dk")
						.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		userSession = (MockHttpSession) loginUserRes.getRequest().getSession();
		assertNotNull(userSession);

		mvc.perform(get("/studyline/1")
						.session(userSession))
				.andExpect(status().isOk())
				.andExpect(view().name("studyline"));
	}

	@Test
	void only_admin_can_access_admin_page() throws Exception {
		MvcResult loginUserRes = mvc.perform(post("/login")
						.param("email", "admin@test.dk")
						.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		MockHttpSession userSession = (MockHttpSession) loginUserRes.getRequest().getSession();
		assertNotNull(userSession);

		mvc.perform(get("/adminpage")
						.session(userSession))
				.andExpect(status().isOk())
				.andExpect(view().name("adminpage"));

		loginUserRes = mvc.perform(post("/login")
						.param("email", "student@test.dk")
						.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		userSession = (MockHttpSession) loginUserRes.getRequest().getSession();
		assertNotNull(userSession);

		mvc.perform(get("/adminpage")
						.session(userSession))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/forbidden"));
	}

	@Test
	void admin_can_grant_admin_and_remove_it() throws Exception {
		MvcResult loginUserRes = mvc.perform(post("/login")
						.param("email", "admin@test.dk")
						.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		MockHttpSession userSession = (MockHttpSession) loginUserRes.getRequest().getSession();
		assertNotNull(userSession);

		mvc.perform(post("/adminpage/grant-admin")
						.session(userSession)
						.param("email", "student@test.dk"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/adminpage"));

		loginUserRes = mvc.perform(post("/login")
						.param("email", "student@test.dk")
						.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		userSession = (MockHttpSession) loginUserRes.getRequest().getSession();
		assertNotNull(userSession);

		mvc.perform(get("/adminpage")
						.session(userSession))
				.andExpect(status().isOk())
				.andExpect(view().name("adminpage"));

		loginUserRes = mvc.perform(post("/login")
						.param("email", "admin@test.dk")
						.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		userSession = (MockHttpSession) loginUserRes.getRequest().getSession();
		assertNotNull(userSession);

		mvc.perform(post("/adminpage/remove-role")
						.session(userSession)
						.param("email", "student@test.dk")
						.param("role", "ADMINISTRATOR"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/adminpage"));

		loginUserRes = mvc.perform(post("/login")
						.param("email", "student@test.dk")
						.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		userSession = (MockHttpSession) loginUserRes.getRequest().getSession();
		assertNotNull(userSession);

		mvc.perform(get("/adminpage")
						.session(userSession))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/forbidden"));
	}

	@Test
	void view_users_and_individual_user_pages() throws Exception {
		MvcResult loginUserRes = mvc.perform(post("/login")
						.param("email", "student@test.dk")
						.param("password", "password"))
				.andExpect(status().isFound())
				.andExpect(redirectedUrl("/"))
				.andReturn();

		MockHttpSession userSession = (MockHttpSession) loginUserRes.getRequest().getSession();
		assertNotNull(userSession);

		mvc.perform(get("/users")
						.session(userSession))
				.andExpect(status().isOk())
				.andExpect(view().name("users"));


		mvc.perform(get("/user/1")
						.session(userSession))
				.andExpect(status().isOk())
				.andExpect(view().name("user"));
	}
}


