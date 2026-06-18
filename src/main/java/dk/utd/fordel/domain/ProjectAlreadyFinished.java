package dk.utd.fordel.domain;

public class ProjectAlreadyPublished extends RuntimeException {
    public ProjectAlreadyPublished(String message) {
        super(message);
    }
}
