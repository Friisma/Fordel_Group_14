package dk.utd.fordel.domain;

public class Project {
    private int projectID = 0;
    private String projectName;
    private String description;
    private StudyLine studyLine;

    public Project(String projectName, String description, int projectID) throws InvalidProject {

        if (projectName.isEmpty()){
            throw new InvalidProject("Cant have an empty project name");
        }

        this.projectID = projectID;
        this.projectName = projectName;
        this.description = description;
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

    public void setProjectID(int projectID) {
        this.projectID = projectID;
    }

    public static class InvalidProject extends Exception {
        public InvalidProject(String msg) {
            super(msg);
        }
    }
}

