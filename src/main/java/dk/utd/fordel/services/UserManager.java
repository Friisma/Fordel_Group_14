package dk.utd.fordel.services;

import dk.utd.fordel.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserManager {

    public static interface UserRepository {
        void create(User user, User.Data data) throws EmailAlreadyInUse;

        User.Data read(User user) throws UserDoesNotExist;

        void update(User user, User.Data data) throws UserDoesNotExist;

        void updateStudyStart(User user, LocalDate studyStart) throws UserDoesNotExist;

        void delete(User user) throws UserDoesNotExist;

        int getUserIdByEmail(Email email);

        int getMaxWorkLoad(User user);

        void updateMaxWorkLoad(User user, int newMaxLoad);

        Roles determineStudyLineSpecificRole(User user, String studyLineName);

        LocalDateTime getPasswordChangedAt(User user) throws UserDoesNotExist;

        void setPasswordChangedAt(User user, LocalDateTime passwordChangedAt) throws UserDoesNotExist;

        public void grantRole(User user, Roles roleToGrant);

        public void removeRole(User user, Roles roleToRemove);

        void assignStudyLine(User user, String studyLineName);

        void assignStudyLineRole(User user, String studLineName, Roles roleToGrant);

        public List<Roles> getUserRoles(User user);

        boolean checkMaxWorkLoad(User advisor);

        public record UserInfo(String id, String email, String name, List<Roles> roles, String studyLine) {
        }

        List<UserInfo> getAllUsers();

        List<UserInfo> getAllUsersRelatedToStudyLine(int studyLineId);

        List<UserInfo> getAllStudentsRelatedToStudyLine(int studyLineId);

        List<UserInfo> getAllLeadersRelatedToStudyLine(int studyLineId);

        List<UserInfo> getAllAdvisorsRelatedToStudyLine(int studyLineId);

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

    public void updateStudyStart(User user, LocalDate studyStart) throws UserDoesNotExist {
        userRepository.updateStudyStart(user, studyStart);
    }

    public void deleteUser(User user) throws UserDoesNotExist {
        userRepository.delete(user);
    }

    public boolean checkMaxWorkLoad(User advisor){
        return userRepository.checkMaxWorkLoad(advisor);
    }

    public int getUserIdByEmail(Email email) {
        return userRepository.getUserIdByEmail(email);
    }

    public LocalDateTime getPasswordChangedAt(User user) throws UserDoesNotExist {
        return userRepository.getPasswordChangedAt(user);
    }

    public void setPasswordChangedAt(User user, LocalDateTime passwordChangedAt) throws UserDoesNotExist {
        userRepository.setPasswordChangedAt(user, passwordChangedAt);
    }

    public void grantRole(User user, Roles roleToGrant) {
        userRepository.grantRole(user, roleToGrant);
    }

    public void removeRole(User user, Roles roleToRemove) {
        userRepository.removeRole(user, roleToRemove);
    }

    public void assignStudyLine(User user, String studyLineName) { userRepository.assignStudyLine(user, studyLineName); }

    public void assignStudyLineRole(User user, String studLineName, Roles roleToGrant) {
        userRepository.assignStudyLineRole(user, studLineName, roleToGrant);
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

    public int getMaxWorkLoad(User user){return userRepository.getMaxWorkLoad(user);}

    public void updateMaxWorkLoad(User user, int newMaxLoad){
        userRepository.updateMaxWorkLoad(user,newMaxLoad);
    }

    public Roles determineStudyLineSpecificRole(User user, String studyLineName){
        return userRepository.determineStudyLineSpecificRole(user,studyLineName);
    }

    public List<UserRepository.UserInfo> getAllStudentsRelatedToStudyLine(int studyLineId) {
        return userRepository.getAllStudentsRelatedToStudyLine(studyLineId);
    }

    public List<UserRepository.UserInfo> getAllLeadersRelatedToStudyLine(int studyLineId) {
        return userRepository.getAllLeadersRelatedToStudyLine(studyLineId);
    }

    public List<UserRepository.UserInfo> getAllAdvisorsRelatedToStudyLine(int studyLineId) {
        return userRepository.getAllAdvisorsRelatedToStudyLine(studyLineId);
    }
}
