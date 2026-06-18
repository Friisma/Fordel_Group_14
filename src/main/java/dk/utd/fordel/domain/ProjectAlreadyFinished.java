package dk.utd.fordel.domain;

public class ProjectAlreadyFinished extends RuntimeException {
    public ProjectAlreadyFinished(String message) {
        super(message);
    }
}
