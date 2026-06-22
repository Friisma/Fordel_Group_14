package dk.utd.fordel.services;

import dk.utd.fordel.domain.AdvisorAlreadyInvited;
import dk.utd.fordel.domain.AdvisorInvitation;
import dk.utd.fordel.domain.StudentInvitation;
import dk.utd.fordel.domain.Project;
import dk.utd.fordel.domain.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InvitationManager {
    public static interface InvitationRepository {
        void create(User invited, Project project);

        void createStudentInvitation(User invited, Project project);

        void delete();

        void update(AdvisorInvitation advisorInvitation) throws AdvisorAlreadyInvited;

        void updateStudentInvitation(StudentInvitation studentInvitation);

        List<AdvisorInvitation> getAllInvitesReceived(User invited);

        List<StudentInvitation> getAllStudentInvitesReceived(User invited);

        List<InviteView> getInvitesForProject(int projectId);

        List<StudentInviteView> getStudentInvitesForProject(int projectId);

        List<AdvisorInviteView> getInvitesReceivedForAdvisor(int advisorId);
    }

    private final InvitationRepository invitationRepository;

    public InvitationManager(InvitationRepository invitationRepository) {
        this.invitationRepository = invitationRepository;
    }

    public void createInvitaion(User invited, Project project) {

        invitationRepository.create(invited, project);
    }

    public void createStudentInvitation(User invited, Project project) {
        invitationRepository.createStudentInvitation(invited, project);
    }

    public void deleteInvitation() {
        invitationRepository.delete();
    }

    public void updateInvitation(AdvisorInvitation advisorInvitation) {
        invitationRepository.update(advisorInvitation);
    }

    public void updateStudentInvitation(StudentInvitation studentInvitation) {
        invitationRepository.updateStudentInvitation(studentInvitation);
    }

    public List<AdvisorInvitation> getAllInvitesReceived(User invited) {
        return invitationRepository.getAllInvitesReceived(invited);
    }

    public List<StudentInvitation> getAllStudentInvitesReceived(User invited) {
        return invitationRepository.getAllStudentInvitesReceived(invited);
    }

    public static record InviteView(String name, String email, String status, String statusClass) {
    }

    public static record StudentInviteView(String name, String email, String status, String statusClass) {
    }

    public List<InviteView> getInvitesForProject(int projectId) {
        return invitationRepository.getInvitesForProject(projectId);
    }

    public List<StudentInviteView> getStudentInvitesForProject(int projectId) {
        return invitationRepository.getStudentInvitesForProject(projectId);
    }

    public static record AdvisorInviteView(String projectTitle, String inviterName, String status, String statusClass,
            int projectId) {
    }

    public List<AdvisorInviteView> getInvitesReceivedForAdvisor(int advisorId) {
        return invitationRepository.getInvitesReceivedForAdvisor(advisorId);
    }

}
