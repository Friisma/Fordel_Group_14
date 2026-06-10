package dk.utd.fordel.domain;

/**
 * An enum (fixed set of values) which states the different types of users and their respective clearance level.
 * This way we make sure that the users use the same role logic.
 */
public enum Roles {
    USER(1),
    STUDENT(2),
    ADVISOR(3),
    TEACHER(4),
    STUDY_LEADER(5),
    ADMIN(6);

    private final int clearanceLevel;

    Roles(int clearanceLevel) {
        this.clearanceLevel = clearanceLevel;
    }

    public int getClearanceLevel() {return clearanceLevel;}
}

