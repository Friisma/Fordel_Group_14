package dk.utd.fordel.services;

import dk.utd.fordel.domain.Email;
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
        int getUserIdByEmail(Email email);

        public void grantRole(User user, Roles roleToGrant);
        public void removeRole(User user, Roles roleToRemove);
        void assignStudyLine(User user, String studyLineName);
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

    public void updateUser(User user, User.Data data) throws UserDoesNotExist {
        userRepository.update(user, data);
    }

    //TODO Maybe find a better solution, once the domain is complete
    public int getUserIdByEmail(Email email){
        return userRepository.getUserIdByEmail(email);
    }

    public void grantRole(User user, Roles roleToGrant) {
        userRepository.grantRole(user, roleToGrant);
    }

    public void removeRole(User user, Roles roleToRemove) {
        userRepository.removeRole(user, roleToRemove);
    }

    public void assignStudyLine(User user, String studyLineName) { userRepository.assignStudyLine(user, studyLineName); }

    public List<Roles> getUserRoles(User user) {
        return userRepository.getUserRoles(user);
    }

    public List<UserRepository.UserInfo> getAllUsers() {
        return userRepository.getAllUsers();
    }
}
