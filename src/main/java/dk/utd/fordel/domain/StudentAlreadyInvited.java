package dk.utd.fordel.domain;

public class StudentAlreadyInvited extends RuntimeException {
    public StudentAlreadyInvited(String message) {
        super(message);
    }
}