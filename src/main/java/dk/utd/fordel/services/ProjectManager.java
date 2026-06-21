package dk.utd.fordel.services;

import dk.utd.fordel.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectManager {

    public static interface ProjectRepository {
        void create(Project project, User user, int userId) throws ProjectTitleAlreadyInUse;

        void create(Project project, User user, String studyLineName, int userId) throws ProjectTitleAlreadyInUse;

        Project read(Project project) throws ProjectDoesNotExist;

        List<Project> getAllProjectsRelatedToUser(User user) throws Project.InvalidProject;

        List<Project> getAllProjectsRelatedToStudyLine(int studyLineId);

        List<Project> getAllProjectsRelatedToStudyLineAndUser(int studyLineId, User user);

        boolean canViewProject(User user, int projectId);

        String getProjectCreatorEmail(int projectId);

        void kickMemberFromProject(int projectId, User member);

        List<Project> getAllAdvisorProjects(User advisor);

        int getCountOfAdvisorProjects(User advisor);

        void StartProject(User advisor,Project project);

        void update(Project project) throws ProjectDoesNotExist;

        void delete(Project project) throws ProjectDoesNotExist;
        List<Project> getAllProjects() throws Project.InvalidProject;
        void assignAdvisor(User advisor,Project project) throws AdvisorAlreadyAdvises;
    }

    private final ProjectRepository projectRepository;

    public ProjectManager(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public void createProject(Project project, User user, String studyLineName, int userId)
            throws ProjectTitleAlreadyInUse {
        projectRepository.create(project, user, studyLineName, userId);
    }

    public List<Project> getAllProjectsRelatedToUser(User user) throws Project.InvalidProject {
        return projectRepository.getAllProjectsRelatedToUser(user);
    }

    public List<Project> getAllProjectsRelatedToStudyLine(int studyLineId) {
        return projectRepository.getAllProjectsRelatedToStudyLine(studyLineId);
    }

    public List<Project> getAllProjectsRelatedToStudyLineAndUser(int studyLineId, User user) {
        return projectRepository.getAllProjectsRelatedToStudyLineAndUser(studyLineId, user);
    }

    public boolean canViewProject(User user, int projectId) {
        return projectRepository.canViewProject(user, projectId);
    }

    public String getProjectCreatorEmail(int projectId) {
        return projectRepository.getProjectCreatorEmail(projectId);
    }

    public void kickMemberFromProject(int projectId, User member) {
        projectRepository.kickMemberFromProject(projectId, member);
    }

    public void assignAdvisor(Project project, User user) throws AdvisorAlreadyAdvises{
        projectRepository.assignAdvisor(user,project);
    }

    public List<Project> getAllProjects() throws Project.InvalidProject {
        return projectRepository.getAllProjects();
    }

    public List<Project> getAllAdvisorProjects(User advisor) throws Project.InvalidProject {
        return projectRepository.getAllAdvisorProjects(advisor);
    }

    public int getCountOfAdvisedProjects(User user){
        return projectRepository.getCountOfAdvisorProjects(user);
    }

    public void StartProject(User advisor, Project project){
        projectRepository.StartProject(advisor,project);

    }

}
