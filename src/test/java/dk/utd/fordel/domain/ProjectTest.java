package dk.utd.fordel.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProjectTest {

    @Test
    void a_project_is_created_with_no_name(){
           assertThrows(Project.InvalidProject.class, () -> {
               new Project("","desc");
           });
    }

    @Test
    void a_project_name_is_changed_to_an_empty_name_should_fail() throws Project.InvalidProject {
        Project project = new Project("name","desc");
        assertThrows(Project.InvalidProject.class, () -> {
            project.setProjectName("");
        });
    }

}
