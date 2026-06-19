package dk.utd.fordel.services;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import dk.utd.fordel.domain.User;
import dk.utd.fordel.domain.UserDoesNotExist;

@Service
public class PasswordExpiryService {

    private final UserManager userManager;

    public PasswordExpiryService(UserManager userManager) {
        this.userManager = userManager;
    }

    public LocalDateTime getPasswordExpiresAt(User user) throws UserDoesNotExist {
        LocalDateTime passwordChangedAt = userManager.getPasswordChangedAt(user);
        return passwordChangedAt != null ? passwordChangedAt.plusMonths(3) : null;
    }

    public boolean isPasswordExpired(User user) throws UserDoesNotExist {
        LocalDateTime passwordExpiresAt = getPasswordExpiresAt(user);
        return passwordExpiresAt != null && !passwordExpiresAt.isAfter(LocalDateTime.now());
    }
}