package dk.utd.fordel.domain;

public class UserDoesNotExist extends Exception {
    public final User user;

    public UserDoesNotExist(User user, String reason) {
        super("no such user " + user + ":" + reason);
        this.user = user;
    }
}
