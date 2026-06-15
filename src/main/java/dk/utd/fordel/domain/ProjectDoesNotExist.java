package dk.utd.fordel.domain;

public class ProjectDoesNotExist extends Exception {
    public final Project project;
    public ProjectDoesNotExist(String reason, Project project) {
        super("no such project " + project + ":" + reason);
        this.project = project;
    }
}
