package dk.utd.fordel.domain;

public class StudentInvitationAlreadyHandled extends RuntimeException {
    public StudentInvitationAlreadyHandled(String message) {
        super(message);
    }
}