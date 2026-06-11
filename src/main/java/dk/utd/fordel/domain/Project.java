package dk.utd.fordel.domain;

public class Project {
    private int projectID = 0;
    private String projectName;
    private String description;

    public Project(String projectName, String description) throws InvalidProject {

        if (projectName.isEmpty()){
            throw new InvalidProject("Cant have an empty project name");
        }

        this.projectID = 0;
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

