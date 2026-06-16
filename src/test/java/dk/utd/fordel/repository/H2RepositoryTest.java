package dk.utd.fordel.repository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dk.utd.fordel.domain.*;
import org.bouncycastle.asn1.x509.UserNotice;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;

import dk.utd.fordel.domain.Email.InvalidEmail;
import dk.utd.fordel.services.Authenticator.DefaultPasswordEncoder;
import dk.utd.fordel.services.Authenticator.PasswordEncoding;
import dk.utd.fordel.utils.UnitOfWork.UnitOfWorkException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    void creating_a_project_with_an_existing_name_should_fail()
            throws ProjectTitleAlreadyInUse, UnitOfWorkException, Project.InvalidProject, EmailAlreadyInUse {
        Email email = new Email("test","test");
        User user = new User(email);

        User.Data data = new User.Data("name",new ArrayList<>(),"line", LocalDate.of(2001,1,1));
        Project project = new Project("test","test",0);


        try (var ignored = h2Repository.begin()){
            h2Repository.create(user, data);

            int userId = h2Repository.getUserIdByEmail(user.getEmail());

            h2Repository.create(project,user,userId);
            assertThrows(ProjectTitleAlreadyInUse.class,
                    () -> h2Repository.create(project,user,userId));
        }


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
            h2Repository.create(user, new User.Data("Peter", List.of(), null, null));
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
            h2Repository.create(user, new User.Data("Peter", List.of(), null, null));
            assertThrows(EmailAlreadyInUse.class, () -> h2Repository.create(user, new User.Data("Peter", List.of(), null, null)));
        }

        // This is okay because the previous transaction was not committed.
        try (var uow = h2Repository.begin()) {
            assertDoesNotThrow(() -> h2Repository.create(user, new User.Data("Peter", List.of(), null, null)));
            uow.commit();
        }

        // Since the transaction was commited, trying to create the same user again
        // fails
        try (var uow = h2Repository.begin()) {
            assertThrows(EmailAlreadyInUse.class, () -> h2Repository.create(user, new User.Data("Peter", List.of(), null, null)));
        }

        // But now we need to cleanup the database
        try (var uow = h2Repository.begin()) {
            h2Repository.delete(user);
            uow.commit();
        }
    }

}
