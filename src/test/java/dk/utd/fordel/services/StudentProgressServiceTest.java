package dk.utd.fordel.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dk.utd.fordel.domain.Roles;
import dk.utd.fordel.domain.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

class StudentProgressServiceTest {

    private final StudentProgressService service = new StudentProgressService();

    @Test
    void it_builds_progress_from_a_study_start_date() {
        LocalDate studyStart = LocalDate.now().minusMonths(2).withDayOfMonth(1);
        User.Data userData = new User.Data("Student", List.of(Roles.STUDENT), null, studyStart);

        var progress = service.build(userData);

        assertTrue(progress.hasStudyStart());
        assertEquals(studyStart, progress.studyStart());
        assertEquals(studyStart.plusMonths(30), progress.internshipStart());
        assertEquals(studyStart.plusMonths(36), progress.projectStart());
        assertFalse(progress.currentSemesterLabel().isBlank());
        assertFalse(progress.internshipCountdown().isBlank());
        assertFalse(progress.projectCountdown().isBlank());
    }

    @Test
    void it_reports_missing_study_start_dates() {
        User.Data userData = new User.Data("Student", List.of(Roles.STUDENT), null, null);

        var progress = service.build(userData);

        assertFalse(progress.hasStudyStart());
        assertEquals("Not set", progress.currentSemesterLabel());
    }
}
