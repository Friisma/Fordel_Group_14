package dk.utd.fordel.services;

import dk.utd.fordel.domain.Roles;
import org.springframework.stereotype.Service;

import dk.utd.fordel.domain.EmailAlreadyInUse;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.domain.UserDoesNotExist;

import java.util.List;

@Service
public class UserManager {

    public static interface UserRepository {
        void create(User user, User.Data data) throws EmailAlreadyInUse;
        User.Data read(User user) throws UserDoesNotExist;
        void update(User user, User.Data data) throws UserDoesNotExist;
        void delete(User user) throws UserDoesNotExist;

        public void grantRole(User user, Roles roleToGrant);
        public void removeRole(User user, Roles roleToRemove);
        public List<Roles> getUserRoles(User user);

        public record UserInfo(String id, String email, String name, List<Roles> roles, String studyLine) {}
        List<UserInfo> getAllUsers();
    }

    private final UserRepository userRepository;

    public UserManager(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void createUser(User user, User.Data data) throws EmailAlreadyInUse {
        userRepository.create(user, data);
    }

    public User.Data readUser(User user) throws UserDoesNotExist {
        return userRepository.read(user);
    }

    public void grantRole(User user, Roles roleToGrant) {
        userRepository.grantRole(user, roleToGrant);
    }

    public void removeRole(User user, Roles roleToRemove) {
        userRepository.removeRole(user, roleToRemove);
    }

    public List<Roles> getUserRoles(User user) {
        return userRepository.getUserRoles(user);
    }

    public List<UserRepository.UserInfo> getAllUsers() {
        return userRepository.getAllUsers();
    }
}
