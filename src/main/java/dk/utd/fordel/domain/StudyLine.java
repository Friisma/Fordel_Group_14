package dk.utd.fordel.domain;

/**
 * this class represents a studyLine which can be created.
 */
public class StudyLine {

    private String lineName;

    StudyLine(String lineName) {
        this.lineName = lineName;
    }
    public String getLineName() {return lineName;}
    public void setLineName(String lineName) {this.lineName = lineName;}


}
