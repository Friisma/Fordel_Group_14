package dk.utd.fordel.services;

import dk.utd.fordel.domain.*;
import org.springframework.stereotype.Service;

@Service
public class ProjectManager {

    public static interface ProjectRepository {
        void create(Project project, User user, int userId) throws ProjectTitleAlreadyInUse;
        Project read(Project project) throws ProjectDoesNotExist;
        void update(Project project) throws ProjectDoesNotExist;
        void delete(Project project) throws ProjectDoesNotExist;
    }

    private final ProjectRepository projectRepository;

    public ProjectManager(ProjectRepository projectRepository){this.projectRepository = projectRepository;}

    public void createProject(Project project, User user, int userId) throws ProjectTitleAlreadyInUse{
        projectRepository.create(project, user, userId);
    }

}
