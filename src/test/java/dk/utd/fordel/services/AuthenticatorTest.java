package dk.utd.fordel.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dk.utd.fordel.domain.Email.InvalidEmail;
import dk.utd.fordel.domain.UserDoesNotExist;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.services.Authenticator.Auth;
import dk.utd.fordel.services.Authenticator.BadCredentials;
import dk.utd.fordel.services.Authenticator.DefaultPasswordEncoder;
import dk.utd.fordel.services.Authenticator.PasswordEncoding;
import dk.utd.fordel.services.Authenticator.PasswordRepository;

public class AuthenticatorTest {

    private final PasswordEncoding encodingOfPassword = new PasswordEncoding(
            "$argon2id$v=19$m=60000,t=10,p=1$PFST899DkMZdpbplW4zExw$XOdNo9v0mEtyx2uyCQxK0qqqc2gCEgVVkb1kj+B9GRg");

    private User knownUser = User.of("known@email.dk");

    public AuthenticatorTest() throws InvalidEmail {
    }

    private final PasswordRepository mockPasswordRepository = new PasswordRepository() {
        PasswordEncoding encodingForUser = encodingOfPassword;

        @Override
        public PasswordEncoding getPassword(User user) throws UserDoesNotExist {
            if (!knownUser.equals(user))
                throw new UserDoesNotExist(user, "no such user");
            return encodingForUser;
        }

        @Override
        public void setPassword(User user, PasswordEncoding encoding) throws UserDoesNotExist {
            if (!knownUser.equals(user))
                throw new UserDoesNotExist(user, "no such user");
            this.encodingForUser = encoding;
        }

    };

    @Test
    void default_encoder_remains_unchanged() {
        DefaultPasswordEncoder defaultPasswordEncoder = new DefaultPasswordEncoder();
        assertTrue(defaultPasswordEncoder.matches("password", encodingOfPassword));
    }

    @Test
    void when_the_user_is_unknown_then_a_bad_credential_error_is_thrown() {
        Authenticator authenticator = new Authenticator(new DefaultPasswordEncoder(), mockPasswordRepository);
        assertThrows(Authenticator.BadCredentials.class, () -> {
            authenticator.authenticate(User.of("unknown@user.dk"), "password");
        });
    }

    @Test
    void when_the_user_has_a_bad_password_throw_a_bad_credential_error() {
        Authenticator authenticator = new Authenticator(new DefaultPasswordEncoder(), mockPasswordRepository);
        assertThrows(Authenticator.BadCredentials.class, () -> {
            authenticator.authenticate(knownUser, "nopass");
        });
    }

    @Test
    void when_the_password_match_the_users_then_authenticate() {
        Authenticator authenticator = new Authenticator(new DefaultPasswordEncoder(), mockPasswordRepository);
        Auth auth = assertDoesNotThrow(() -> authenticator.authenticate(knownUser, "password"));
        assertEquals(knownUser, auth.user());
    }

    @Test
    void when_a_new_password_has_been_registered_it_works() throws UserDoesNotExist {
        Authenticator authenticator = new Authenticator(new DefaultPasswordEncoder(), mockPasswordRepository);
        authenticator.register(knownUser, "password2");
        Auth auth = assertDoesNotThrow(() -> authenticator.authenticate(knownUser, "password2"));
        assertEquals(knownUser, auth.user());
    }

    @Test
    void when_a_new_password_has_been_registered_the_old_no_longer_works() throws UserDoesNotExist {
        Authenticator authenticator = new Authenticator(new DefaultPasswordEncoder(), mockPasswordRepository);
        authenticator.register(knownUser, "password2");
        assertThrows(BadCredentials.class, () -> authenticator.authenticate(knownUser, "password"));
    }

}
