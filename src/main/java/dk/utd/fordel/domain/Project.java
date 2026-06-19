package dk.utd.fordel.domain;

public class Project {

    public enum Status {
        DRAFT,
        STARTED,
        FINISHED
    }

    private int projectID = 0;
    private String projectName;
    private String description;
    private String studyLine;
    private Status status;

    public Project(String projectName, String description) throws InvalidProject {

        if (projectName.isEmpty()){
            throw new InvalidProject("Cant have an empty project name");
        }

        this.projectID = 0;
        this.projectName = projectName;
        this.description = description;
        this.studyLine = "";
        this.status = Status.DRAFT;

    }
    public int getProjectID() {
        return this.projectID;
    }
    public String getProjectIDAsString(){return String.valueOf(this.projectID);}

    public void setProjectName(String projectName) throws InvalidProject {
        if (projectName.isEmpty()){
            throw new InvalidProject("Cant have an empty project name");
        }
        this.projectName = projectName;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getDescription() {
        return description;
    }

    public String getStudyLine() {
        return studyLine;
    }

    public void setStudyLine(String studyLine) {
        this.studyLine = studyLine;
    }

    public void setProjectID(int projectID) {
        this.projectID = projectID;
    }

    public static class InvalidProject extends Exception {
        public InvalidProject(String msg) {
            super(msg);
        }
    }

    public void setStatus(Status status){
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }
}

