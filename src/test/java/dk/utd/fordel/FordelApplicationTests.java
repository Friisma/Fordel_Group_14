package dk.utd.fordel;

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

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-integrationtest.properties")
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD) // Reset the database before each test
class FordelApplicationTests {

	@Autowired
	MockMvc mvc;

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

		assertTrue(res.getResponse().getContentAsString().contains("Email er allerede i brug"));
	}

}
