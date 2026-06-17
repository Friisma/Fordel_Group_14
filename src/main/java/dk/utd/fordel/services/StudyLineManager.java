package dk.utd.fordel.services;

import dk.utd.fordel.domain.StudyLine;
import dk.utd.fordel.domain.StudyLineAlreadyExists;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudyLineManager {

    public interface StudyLineRepository {
        void create(StudyLine studyLine) throws StudyLineAlreadyExists;
        List<String> getAllStudyLines();
        List<StudyLine> getAllStudyLines();
    }

    private final StudyLineRepository studyLineRepository;

    public StudyLineManager(StudyLineRepository studyLineRepository) {
        this.studyLineRepository = studyLineRepository;
    }

    public void create(StudyLine studyLine) throws StudyLineAlreadyExists {
        studyLineRepository.create(studyLine);
    }

    public List<String> getAllStudyLines() {
        return studyLineRepository.getAllStudyLines();
    }
}