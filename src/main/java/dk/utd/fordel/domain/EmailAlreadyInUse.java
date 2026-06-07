package dk.utd.fordel.domain;

public class EmailAlreadyInUse extends Exception {
    public final Email email;

    public EmailAlreadyInUse(Email email) {
        super("Email already in use: " + email);
        this.email = email;
    }

}
