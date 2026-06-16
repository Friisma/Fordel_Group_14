package dk.utd.fordel.services;

import dk.utd.fordel.domain.*;
import org.springframework.stereotype.Service;

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
        public List<Roles> getUserRoles(User user);

        public record UserInfo(String id, String email, String name, List<Roles> roles, String studyLine) {}
        List<UserInfo> getAllUsers();

        List<UserInfo> getAllUsersRelatedToStudyLine(int studyLineId);
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

    public List<Roles> getUserRoles(User user) {
        return userRepository.getUserRoles(user);
    }

    public List<UserRepository.UserInfo> getAllUsers() {
        return userRepository.getAllUsers();
    }

    public List<UserRepository.UserInfo> getAllUsersRelatedToStudyLine(int studyLineId) {
        return userRepository.getAllUsersRelatedToStudyLine(studyLineId);
    }
}
