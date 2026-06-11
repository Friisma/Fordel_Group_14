package dk.utd.fordel.domain;

import java.time.LocalDate;
import java.util.List;

/*
 * A User is an entity with a string Id.
 */
public record User(Email id) {

  public static User of(String email) throws Email.InvalidEmail {
    return new User(Email.of(email));
  }

  /**
   * A users id is the email address.
   * 
   * @return the email address of the user
   */
  public Email getEmail() {
    return this.id;
  }

  public record Data(String name, List<Roles> roles, String studyLine, LocalDate studyStart) {
  }

}
