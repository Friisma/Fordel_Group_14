package dk.utd.fordel.repository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bouncycastle.asn1.x509.UserNotice;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;

import dk.utd.fordel.domain.Email.InvalidEmail;
import dk.utd.fordel.domain.EmailAlreadyInUse;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.domain.UserDoesNotExist;
import dk.utd.fordel.services.Authenticator.DefaultPasswordEncoder;
import dk.utd.fordel.services.Authenticator.PasswordEncoding;
import dk.utd.fordel.utils.UnitOfWork.UnitOfWorkException;

public class H2RepositoryTest {

    static H2Repository h2Repository;

    @BeforeAll
    static void setUp() {
        // Set up the test environment

        javax.sql.DataSource dataSource = DataSourceBuilder.create()
                .url("jdbc:h2:mem:testdb")
                .driverClassName("org.h2.Driver")
                .username("sa")
                .password("")
                .build();

        h2Repository = new H2Repository(dataSource);
    }

    @Test
    void setting_the_password_for_a_non_existent_user_should_fail() throws InvalidEmail, UnitOfWorkException {
        var user = User.of("unknown@user.dk");
        try (var ignored = h2Repository.begin()) {
            assertThrows(UserDoesNotExist.class,
                    () -> h2Repository.setPassword(user, new PasswordEncoding("password")));
        }
    }

    @Test
    void setting_the_password_for_an_existing_user_should_succeed()
            throws InvalidEmail, UserDoesNotExist, EmailAlreadyInUse, UnitOfWorkException {
        var user = User.of("known@user.dk");
        try (var ignored = h2Repository.begin()) {
            h2Repository.create(user, new User.Data("Peter"));
            var encoding = new DefaultPasswordEncoder().encode("password");
            h2Repository.setPassword(user, encoding);
            assertEquals(encoding, h2Repository.getPassword(user));
        }

    }

    @Test
    void creating_the_same_user_twice_should_fail()
            throws InvalidEmail, UserDoesNotExist, EmailAlreadyInUse, UnitOfWorkException {
        var user = User.of("known@user.dk");
        try (var uow = h2Repository.begin()) {
            h2Repository.create(user, new User.Data("Peter"));
            assertThrows(EmailAlreadyInUse.class, () -> h2Repository.create(user, new User.Data("Peter")));
        }

        // This is okay because the previous transaction was not committed.
        try (var uow = h2Repository.begin()) {
            assertDoesNotThrow(() -> h2Repository.create(user, new User.Data("Peter")));
            uow.commit();
        }

        // Since the transaction was commited, trying to create the same user again
        // fails
        try (var uow = h2Repository.begin()) {
            assertThrows(EmailAlreadyInUse.class, () -> h2Repository.create(user, new User.Data("Peter")));
        }

        // But now we need to cleanup the database
        try (var uow = h2Repository.begin()) {
            h2Repository.delete(user);
            uow.commit();
        }
    }

}
