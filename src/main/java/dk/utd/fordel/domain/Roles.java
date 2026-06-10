package dk.utd.fordel.domain;

/**
 * An enum (fixed set of values) which states the different types of users and their respective clearance level.
 * This way we make sure that the users use the same role logic.
 */
public enum Roles {
    STUDENT(1),
    ADVISOR(2),
    TEACHER(3),
    STUDY_LEADER(4),
    ADMIN(5);

    private final int clearanceLevel;

    Roles(int clearanceLevel) {
        this.clearanceLevel = clearanceLevel;
    }

    public int getClearanceLevel() {return clearanceLevel;}
}

