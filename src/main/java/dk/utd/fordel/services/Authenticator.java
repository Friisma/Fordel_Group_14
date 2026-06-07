package dk.utd.fordel.services;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.stereotype.Service;

import dk.utd.fordel.domain.UserDoesNotExist;
import dk.utd.fordel.utils.Assert;
import dk.utd.fordel.domain.User;

@Service
public class Authenticator {

    public static record PasswordEncoding(String hash) {
    }

    public static interface PasswordRepository {
        /**
         * Get the password for a user.
         * 
         * @param user the user to get the password for
         * @return the password for the user
         * @throws UserDoesNotExist if the user does not exist
         */
        PasswordEncoding getPassword(User user) throws UserDoesNotExist;

        /**
         * Set the password for a user.
         * 
         * @param user     the user to set the password for
         * @param encoding the password to set
         * @throws UserDoesNotExist if the user does not exist
         */
        void setPassword(User user, PasswordEncoding encoding) throws UserDoesNotExist;
    }

    public static interface PasswordEncoder {
        /**
         * Encode a password.
         * 
         * @param password the password to encode
         * @return the encoded password
         */
        PasswordEncoding encode(String password);

        /**
         * Check if a password matches the encoded password.
         * 
         * @param raw      the raw password
         * @param encoding the encoded password
         * @return true if the password matches, false otherwise
         */
        boolean matches(String raw, PasswordEncoding encoding);
    }

    private final PasswordEncoder passwordEncoder;
    private final PasswordRepository passwordRepository;

    public Authenticator(PasswordEncoder passwordEncoder, PasswordRepository passwordRepository) {
        this.passwordEncoder = passwordEncoder;
        this.passwordRepository = passwordRepository;
    }

    public void register(User user, String password) throws UserDoesNotExist {
        PasswordEncoding encoding = passwordEncoder.encode(password);
        passwordRepository.setPassword(user, encoding);
    }

    public Auth authenticate(User user, String password) throws BadCredentials {
        Assert.isNotNull(user, "user should be non-null");
        Assert.isNotNull(password, "password should be non-null");
        try {
            PasswordEncoding encoding = passwordRepository.getPassword(user);
            if (!passwordEncoder.matches(password, encoding)) {
                throw new BadCredentials("Invalid password");
            }
        } catch (UserDoesNotExist e) {
            throw new BadCredentials("No such user: " + e.user);
        }

        return new Auth(user);
    }

    public static record Auth(User user) {
    }

    public static class BadCredentials extends Exception {
        public BadCredentials(String msg) {
            super(msg);
        }
    }

    @Service
    public static class DefaultPasswordEncoder implements PasswordEncoder {
        private final Argon2PasswordEncoder encoder = new Argon2PasswordEncoder(16, 32, 1, 60000, 10);

        @Override
        public PasswordEncoding encode(String password) {
            return new PasswordEncoding(encoder.encode(password));
        }

        @Override
        public boolean matches(String raw, PasswordEncoding encoding) {
            return encoder.matches(raw, encoding.hash);
        }

    }
}
