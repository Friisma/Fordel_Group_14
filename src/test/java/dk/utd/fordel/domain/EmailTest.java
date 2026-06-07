package dk.utd.fordel.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import dk.utd.fordel.domain.Email.InvalidEmail;

public class EmailTest {

    @Test
    void throws_assertion_errors_if_null() throws InvalidEmail {
        assertThrows(IllegalArgumentException.class, () -> Email.of(null));
        assertThrows(IllegalArgumentException.class, () -> Email.of(null, "any"));
        assertThrows(IllegalArgumentException.class, () -> Email.of("any", null));
    }

    @Test
    @DisplayName("with no @ is invalid")
    void with_no_atsign_is_invalid() {
        var t = assertThrows(InvalidEmail.class, () -> Email.of("myexample.com"));
        assertEquals("myexample.com should have @ sign", t.getMessage());
    }

    @Test
    @DisplayName("with no local is invalid")
    void with_no_domain_is_invalid() {
        var t = assertThrows(InvalidEmail.class, () -> Email.of("my@"));
        assertEquals("domain is empty", t.getMessage());
    }

    @Test
    @DisplayName("with no local is invalid")
    void with_no_local_is_invalid() {
        var t = assertThrows(InvalidEmail.class, () -> Email.of("@example.com"));
        assertEquals("local is empty", t.getMessage());
    }

    @ParameterizedTest
    @MethodSource("goodExamples")
    void with_simple_local_and_domain_is_valid(String local, String domain) {
        Email email = assertDoesNotThrow(() -> Email.of(local, domain));
        assertEquals(local, email.local());
        assertEquals(domain, email.domain());
        assertEquals(email, assertDoesNotThrow(() -> Email.of(email.asString())));
    }

    @ParameterizedTest
    @MethodSource("goodExamples")
    void with_same_local_and_domain_is_equal(String local, String domain) throws InvalidEmail {
        assertEquals(Email.of(local, domain), Email.of(local, domain));
        assertEquals(Email.of(local, domain).hashCode(), Email.of(local, domain).hashCode());
    }

    static Stream<Arguments> goodExamples() {
        return Stream.of(
                Arguments.of("my", "example.com"),
                Arguments.of("user1", "example.com"),
                Arguments.of("user2", "utd.dk"));
    }
}
