package dk.utd.fordel.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import dk.utd.fordel.domain.User;

class PasswordExpiryServiceTest {

    @Test
    void get_password_expires_at_returns_null_when_password_change_date_is_missing() throws Exception {
        UserManager userManager = mock(UserManager.class);
        User user = User.of("student@test.dk");
        when(userManager.getPasswordChangedAt(user)).thenReturn(null);

        PasswordExpiryService service = new PasswordExpiryService(userManager);

        assertEquals(null, service.getPasswordExpiresAt(user));
    }

    @Test
    void get_password_expires_at_adds_three_months_to_password_change_date() throws Exception {
        UserManager userManager = mock(UserManager.class);
        User user = User.of("student@test.dk");
        LocalDateTime changedAt = LocalDateTime.of(2026, 1, 15, 10, 30);
        when(userManager.getPasswordChangedAt(user)).thenReturn(changedAt);

        PasswordExpiryService service = new PasswordExpiryService(userManager);

        assertEquals(changedAt.plusMonths(3), service.getPasswordExpiresAt(user));
    }

    @Test
    void is_password_expired_returns_true_when_expiry_is_in_the_past() throws Exception {
        UserManager userManager = mock(UserManager.class);
        User user = User.of("student@test.dk");
        LocalDateTime changedAt = LocalDateTime.now().minusMonths(3).minusMinutes(1);
        when(userManager.getPasswordChangedAt(user)).thenReturn(changedAt);

        PasswordExpiryService service = new PasswordExpiryService(userManager);

        assertTrue(service.isPasswordExpired(user));
    }

    @Test
    void is_password_expired_returns_false_when_expiry_is_in_the_future() throws Exception {
        UserManager userManager = mock(UserManager.class);
        User user = User.of("student@test.dk");
        LocalDateTime changedAt = LocalDateTime.now().minusMonths(3).plusDays(1);
        when(userManager.getPasswordChangedAt(user)).thenReturn(changedAt);

        PasswordExpiryService service = new PasswordExpiryService(userManager);

        assertFalse(service.isPasswordExpired(user));
    }
}
