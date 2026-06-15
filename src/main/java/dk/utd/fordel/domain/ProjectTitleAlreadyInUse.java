package dk.utd.fordel.domain;

public class ProjectTitleAlreadyInUse extends RuntimeException {
    public ProjectTitleAlreadyInUse(String message) {
        super(message);
    }
}
