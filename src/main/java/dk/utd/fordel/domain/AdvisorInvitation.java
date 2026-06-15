package dk.utd.fordel.domain;

public class AdvisorInvitation {

    public enum Status {
        PENDING,
        ACCEPTED,
        DECLINED
    }

    private final int advisorId;
    private final int projectId;
    private Status status;

    public AdvisorInvitation(int advisorId, int projectId){
        this.advisorId = advisorId;
        this.projectId = projectId;
        this.status = Status.PENDING;
    }

    public void accept(){
        if (status != Status.PENDING){
            throw new AdvisorInvitationAlreadyHandled("");
        }

        status = Status.ACCEPTED;
    }

    public void decline(){
        if (status != Status.PENDING){
            throw new AdvisorInvitationAlreadyHandled("");
        }

        status = Status.DECLINED;
    }

    public Status getStatus(){
        return this.status;
    }

    public String getStatusAsString(){
        if (status == Status.ACCEPTED){
            return "accepted";
        }
        if (status == Status.DECLINED){
            return "declined";
        }
        if (status == Status.PENDING){
            return "pending";
        }
        else {
            // This should not happen due to the enum.
            throw new RuntimeException("invalid status");
        }
    }

    public int getAdvisorId() {
        return advisorId;
    }

    public int getProjectId() {
        return projectId;
    }
}
