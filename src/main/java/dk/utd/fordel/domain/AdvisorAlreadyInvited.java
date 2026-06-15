package dk.utd.fordel.domain;

public class AdvisorAlreadyInvited extends RuntimeException {
    public AdvisorAlreadyInvited(String message) {
        super(message);
    }
}
