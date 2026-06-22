package dk.utd.fordel.domain;

public class AdvisorReachedMaxWorkLoad extends RuntimeException {
    public AdvisorReachedMaxWorkLoad(String message) {
        super(message);
    }
}
