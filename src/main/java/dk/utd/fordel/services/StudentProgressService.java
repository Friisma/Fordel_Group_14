package dk.utd.fordel.services;

import dk.utd.fordel.domain.User;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

@Service
public class StudentProgressService {
    private static final int INTERNSHIP_SEMESTER = 6;
    private static final int PROJECT_SEMESTER = 7;
    private static final int MONTHS_PER_SEMESTER = 6;

    public StudentProgressView build(User.Data userData) {
        if (userData == null) {
            return StudentProgressView.empty();
        }

        LocalDate studyStart = userData.studyStart();
        if (studyStart == null) {
            return StudentProgressView.missing(userData.name());
        }

        LocalDate internshipStart = studyStart.plusMonths((long) (INTERNSHIP_SEMESTER - 1) * MONTHS_PER_SEMESTER);
        LocalDate projectStart = studyStart.plusMonths((long) (PROJECT_SEMESTER - 1) * MONTHS_PER_SEMESTER);

        int currentSemester = calculateCurrentSemester(studyStart, LocalDate.now());
        return new StudentProgressView(
                true,
                userData.name(),
                studyStart,
                currentSemester,
                internshipStart,
                timeUntil(internshipStart),
                projectStart,
                timeUntil(projectStart));
    }

    private static int calculateCurrentSemester(LocalDate studyStart, LocalDate today) {
        if (today.isBefore(studyStart)) {
            return 0;
        }

        long monthsSinceStart = ChronoUnit.MONTHS.between(studyStart, today);
        return (int) (monthsSinceStart / MONTHS_PER_SEMESTER) + 1;
    }

    private static String timeUntil(LocalDate targetDate) {
        LocalDate today = LocalDate.now();
        if (!today.isBefore(targetDate)) {
            return "already started";
        }

        Period period = Period.between(today, targetDate);
        StringBuilder builder = new StringBuilder();

        appendPart(builder, period.getYears(), "year");
        appendPart(builder, period.getMonths(), "month");
        appendPart(builder, period.getDays(), "day");

        return builder.length() == 0 ? "less than a day" : builder.toString();
    }

    private static void appendPart(StringBuilder builder, int value, String label) {
        if (value == 0) {
            return;
        }

        if (!builder.isEmpty()) {
            builder.append(", ");
        }

        builder.append(value).append(' ').append(label);
        if (value != 1) {
            builder.append('s');
        }
    }

    public record StudentProgressView(
            boolean hasStudyStart,
            String studentName,
            LocalDate studyStart,
            int currentSemester,
            LocalDate internshipStart,
            String internshipCountdown,
            LocalDate projectStart,
            String projectCountdown) {

        public static StudentProgressView empty() {
            return new StudentProgressView(false, null, null, 0, null, "", null, "");
        }

        public static StudentProgressView missing(String studentName) {
            return new StudentProgressView(false, studentName, null, 0, null, "", null, "");
        }

        public String currentSemesterLabel() {
            if (!hasStudyStart) {
                return "Not set";
            }

            return currentSemester <= 0 ? "Starting soon" : semesterLabel(currentSemester);
        }

        public int semesterProgress() {
            if (!hasStudyStart) {
                return 0;
            }

            LocalDate today = LocalDate.now();
            LocalDate endOfSeventhSemester = studyStart.plusMonths((long) PROJECT_SEMESTER * MONTHS_PER_SEMESTER);
            long totalDays = ChronoUnit.DAYS.between(studyStart, endOfSeventhSemester);
            long daysPassed = ChronoUnit.DAYS.between(studyStart, today);

            if (totalDays <= 0) {
                return 0;
            }

            long clampedDaysPassed = Math.max(0, Math.min(daysPassed, totalDays));
            return (int) ((clampedDaysPassed * 100) / totalDays);
        }

        public boolean isInternshipDone() {
            return hasStudyStart && !LocalDate.now().isBefore(internshipStart);
        }

        public boolean isProjectDone() {
            return hasStudyStart && !LocalDate.now().isBefore(projectStart);
        }

        public boolean isGraduated() {
            if (!hasStudyStart || !isProjectDone()) {
                return false;
            }
            // Student graduates 1 semester (6 months) after project starts
            LocalDate graduationDate = projectStart.plusMonths(6);
            return !LocalDate.now().isBefore(graduationDate);
        }

        public int internshipProgress() {
            if (!hasStudyStart || isInternshipDone()) {
                return 100;
            }

            LocalDate today = LocalDate.now();
            long totalDays = ChronoUnit.DAYS.between(studyStart, internshipStart);
            long daysPassed = ChronoUnit.DAYS.between(studyStart, today);

            if (totalDays <= 0)
                return 0;
            return (int) Math.min((daysPassed * 100) / totalDays, 100);
        }

        public int projectProgress() {
            if (!hasStudyStart || isProjectDone()) {
                return 100;
            }

            LocalDate today = LocalDate.now();
            long totalDays = ChronoUnit.DAYS.between(studyStart, projectStart);
            long daysPassed = ChronoUnit.DAYS.between(studyStart, today);

            if (totalDays <= 0)
                return 0;
            return (int) Math.min((daysPassed * 100) / totalDays, 100);
        }

        private static String semesterLabel(int semester) {
            return semester + ordinalSuffix(semester) + " semester";
        }

        private static String ordinalSuffix(int value) {
            int mod100 = value % 100;
            if (mod100 >= 11 && mod100 <= 13) {
                return "th";
            }

            return switch (value % 10) {
                case 1 -> "st";
                case 2 -> "nd";
                case 3 -> "rd";
                default -> "th";
            };
        }
    }
}