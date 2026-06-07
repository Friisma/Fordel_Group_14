package dk.utd.fordel.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

import dk.utd.fordel.domain.Email.InvalidEmail;

public class UserTest {

  @Test
  void a_user_is_created_with_id() throws InvalidEmail {
    String id = "my@user.dk";
    assertEquals(Email.of(id), User.of(id).id());
  }

  @Test
  void a_users_name_is_its_id() throws InvalidEmail {
    String id = "my@user.dk";
    assertEquals(Email.of(id), User.of(id).getEmail());
  }

  @Test
  void a_user_is_equal_to_another_user_with_the_same_name() throws InvalidEmail {
    String id = "my@user.dk";
    assertEquals(User.of(id), User.of(id));
  }

  @Test
  void a_user_is_not_equal_to_another_user_with_a_different_name() throws InvalidEmail {
    String id = "my@user.dk";
    String id2 = "my2@user.dk";
    assertNotEquals(User.of(id), User.of(id2));

  }

  @Test
  void a_user_is_not_equal_to_any_object_not_a_user() throws InvalidEmail {
    String id = "my@user.dk";
    assertNotEquals(User.of(id), new Object());
  }

  @Test
  void a_user_prints_nicely() throws InvalidEmail {
    String id = "my@user.dk";
    assertEquals("User[id=Email[my@user.dk]]", User.of(id).toString());
  }

}
