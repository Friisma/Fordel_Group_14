package dk.utd.fordel.domain;

/**
 * This class represents a study line which can be created.
 */
public class StudyLine {

    private final String lineName;

    public StudyLine(String lineName) {
        if (lineName == null || lineName.isBlank()) {
            throw new InvalidStudyLine("Study line name cannot be empty");
        }
        this.lineName = lineName;
    }
    public String getLineName() {
        return lineName;
    }
    public static class InvalidStudyLine extends RuntimeException {
        public InvalidStudyLine(String message) {
            super(message);
        }
    }
}