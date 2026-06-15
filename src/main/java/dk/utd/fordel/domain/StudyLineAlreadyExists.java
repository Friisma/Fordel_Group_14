package dk.utd.fordel.domain;

public class StudyLineAlreadyExists extends Exception {
    public StudyLineAlreadyExists(StudyLine studyLine) {
        super("Study line already exists: " + studyLine.getLineName());
    }
}