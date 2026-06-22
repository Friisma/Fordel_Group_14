package dk.utd.fordel.domain;

public class StudentInvitation {

    public enum Status {
        PENDING,
        ACCEPTED,
        DECLINED
    }

    private final int studentId;
    private final int projectId;
    private Status status;

    public StudentInvitation(int studentId, int projectId) {
        this.studentId = studentId;
        this.projectId = projectId;
        this.status = Status.PENDING;
    }

    public void accept() {
        if (status != Status.PENDING) {
            throw new StudentInvitationAlreadyHandled("");
        }

        status = Status.ACCEPTED;
    }

    public void decline() {
        if (status != Status.PENDING) {
            throw new StudentInvitationAlreadyHandled("");
        }

        status = Status.DECLINED;
    }

    public Status getStatus() {
        return this.status;
    }

    public String getStatusAsString() {
        if (status == Status.ACCEPTED) {
            return "accepted";
        }
        if (status == Status.DECLINED) {
            return "declined";
        }
        if (status == Status.PENDING) {
            return "pending";
        } else {
            throw new RuntimeException("invalid status");
        }
    }

    public int getStudentId() {
        return studentId;
    }

    public int getProjectId() {
        return projectId;
    }
}