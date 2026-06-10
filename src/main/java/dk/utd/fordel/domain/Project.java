package dk.utd.fordel.domain;

public class Project {
    private final int projectID;
    private String projectName;
    private String description;


    public Project(int projectID, String projectName, String description) throws InvalidProject {

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

    public String getProjectName() {
        return projectName;
    }

    public String getDescription() {
        return description;
    }


    public static class InvalidProject extends Exception {
        public InvalidProject(String msg) {
            super(msg);
        }
    }
}

