package dk.utd.fordel.services;

import dk.utd.fordel.domain.StudyLine;
import dk.utd.fordel.domain.StudyLineAlreadyExists;
import dk.utd.fordel.domain.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudyLineManager {

    public static interface StudyLineRepository {
        void create(StudyLine studyLine) throws StudyLineAlreadyExists;

        boolean canManageStudyLine(User user, int studyLineId);
    }

    private final StudyLineRepository studyLineRepository;

    public StudyLineManager(StudyLineRepository studyLineRepository) {
        this.studyLineRepository = studyLineRepository;
    }

    public void create(StudyLine studyLine) throws StudyLineAlreadyExists {
        studyLineRepository.create(studyLine);
    }

    public boolean canManageStudyLine(User user, int studyLineId) {
        return studyLineRepository.canManageStudyLine(user, studyLineId);
    }
}