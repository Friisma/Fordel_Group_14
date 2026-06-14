package dk.utd.fordel.services;

import dk.utd.fordel.domain.AdvisorAlreadyInvited;
import dk.utd.fordel.domain.AdvisorInvitation;
import dk.utd.fordel.domain.Project;
import dk.utd.fordel.domain.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InvitationManager {
    public static interface InvitationRepository {
        void create(User invited, Project project);
        void delete();
        void update(AdvisorInvitation advisorInvitation) throws AdvisorAlreadyInvited;
        List<AdvisorInvitation> getAllInvitesReceived(User invited);
    }

    private final InvitationRepository invitationRepository;

    public InvitationManager(InvitationRepository invitationRepository){this.invitationRepository = invitationRepository;}


    public void createInvitaion(User invited, Project project){
        invitationRepository.create(invited, project);
    }

    public void deleteInvitation(){
        invitationRepository.delete();
    }

    public void updateInvitation(AdvisorInvitation advisorInvitation){
        invitationRepository.update(advisorInvitation);
    }

    public List<AdvisorInvitation> getAllInvitesReceived(User invited){
       return invitationRepository.getAllInvitesReceived(invited);
    }

}
