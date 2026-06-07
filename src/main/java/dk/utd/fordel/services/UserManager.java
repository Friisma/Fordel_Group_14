package dk.utd.fordel.services;

import org.springframework.stereotype.Service;

import dk.utd.fordel.domain.EmailAlreadyInUse;
import dk.utd.fordel.domain.User;
import dk.utd.fordel.domain.UserDoesNotExist;

@Service
public class UserManager {

    public static interface UserRepository {
        void create(User user, User.Data data) throws EmailAlreadyInUse;
        User.Data read(User user) throws UserDoesNotExist;
        void update(User user, User.Data data) throws UserDoesNotExist;
        void delete(User user) throws UserDoesNotExist;
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

}
