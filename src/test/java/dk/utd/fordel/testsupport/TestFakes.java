package dk.utd.fordel.testsupport;

import dk.utd.fordel.domain.*;
import dk.utd.fordel.services.Authenticator;
import dk.utd.fordel.services.InvitationManager;
import dk.utd.fordel.services.ProjectManager;
import dk.utd.fordel.services.StudyLineManager;
import dk.utd.fordel.services.UserManager;
import dk.utd.fordel.utils.UnitOfWork;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TestFakes {

    private TestFakes() {
    }

    public static final class FakeUnitOfWork implements UnitOfWork {
        public int beginCount;
        public int commitCount;
        public int rollbackCount;
        public int closeCount;

        @Override
        public UnitOfWork begin() {
            beginCount++;
            return this;
        }

        @Override
        public void commit() {
            commitCount++;
        }

        @Override
        public void rollback() {
            rollbackCount++;
        }

        @Override
        public void close() {
            closeCount++;
        }
    }

    public static final class FakePasswordRepository implements Authenticator.PasswordRepository {
        private final Map<User, Authenticator.PasswordEncoding> passwords = new HashMap<>();

        @Override
        public Authenticator.PasswordEncoding getPassword(User user) throws UserDoesNotExist {
            if (!passwords.containsKey(user)) {
                throw new UserDoesNotExist(user, "no such user");
            }
            return passwords.get(user);
        }

        @Override
        public void setPassword(User user, Authenticator.PasswordEncoding encoding) throws UserDoesNotExist {
            if (!passwords.containsKey(user)) {
                throw new UserDoesNotExist(user, "no such user");
            }
            passwords.put(user, encoding);
        }

        public void put(User user, Authenticator.PasswordEncoding encoding) {
            passwords.put(user, encoding);
        }
    }

    public static final class FakeUserRepository implements UserManager.UserRepository {
        public final Map<String, LocalDateTime> passwordChangedAt = new HashMap<>();
        public final Map<String, Integer> userIds = new HashMap<>();
        public final Map<String, Integer> maxWorkLoads = new HashMap<>();
        public final Map<String, Roles> studyLineRoles = new HashMap<>();
        public final Map<String, List<Roles>> rolesByEmail = new HashMap<>();
        public final List<UserInfo> allUsers = new ArrayList<>();
        public final List<UserInfo> students = new ArrayList<>();
        public final List<UserInfo> leaders = new ArrayList<>();
        public final List<UserInfo> advisors = new ArrayList<>();
        public final List<User> deletedUsers = new ArrayList<>();
        public User lastUpdatedMaxWorkLoadUser;
        public int lastUpdatedMaxWorkLoad;
        public User lastCheckedMaxWorkLoadUser;
        public boolean checkMaxWorkLoadResult = true;

        private static String key(User user) {
            return user.getEmail().asString();
        }

        @Override
        public void create(User user, User.Data data) {
            allUsers.add(new UserInfo(String.valueOf(allUsers.size() + 1), key(user), data.name(), data.roles(),
                    data.studyLine()));
        }

        @Override
        public User.Data read(User user) throws UserDoesNotExist {
            throw new UserDoesNotExist(user, "not implemented in test fake");
        }

        @Override
        public void update(User user, User.Data data) {
        }

        @Override
        public void updateStudyStart(User user, LocalDate studyStart) {
        }

        @Override
        public void delete(User user) {
            deletedUsers.add(user);
        }

        @Override
        public int getUserIdByEmail(Email email) {
            return userIds.getOrDefault(email.asString(), 0);
        }

        @Override
        public int getMaxWorkLoad(User user) {
            return maxWorkLoads.getOrDefault(key(user), 0);
        }

        @Override
        public void updateMaxWorkLoad(User user, int newMaxLoad) {
            lastUpdatedMaxWorkLoadUser = user;
            lastUpdatedMaxWorkLoad = newMaxLoad;
            maxWorkLoads.put(key(user), newMaxLoad);
        }

        @Override
        public Roles determineStudyLineSpecificRole(User user, String studyLineName) {
            return studyLineRoles.get(key(user) + "|" + studyLineName);
        }

        @Override
        public LocalDateTime getPasswordChangedAt(User user) throws UserDoesNotExist {
            return passwordChangedAt.get(key(user));
        }

        @Override
        public void setPasswordChangedAt(User user, LocalDateTime passwordChangedAt) {
            this.passwordChangedAt.put(key(user), passwordChangedAt);
        }

        @Override
        public void grantRole(User user, Roles roleToGrant) {
        }

        @Override
        public void removeRole(User user, Roles roleToRemove) {
        }

        @Override
        public void assignStudyLine(User user, String studyLineName) {
        }

        @Override
        public void assignStudyLineRole(User user, String studLineName, Roles roleToGrant) {
        }

        @Override
        public List<Roles> getUserRoles(User user) {
            return rolesByEmail.getOrDefault(key(user), List.of());
        }

        @Override
        public List<StudyLineManager.StudyLineRepository.StudyLineInfo> getAllStudyLinesRelatedToUser(User user) {
            return List.of();
        }

        @Override
        public boolean checkMaxWorkLoad(User advisor) {
            lastCheckedMaxWorkLoadUser = advisor;
            return checkMaxWorkLoadResult;
        }

        @Override
        public List<UserInfo> getAllUsers() {
            return allUsers;
        }

        @Override
        public List<UserInfo> getAllUsersRelatedToStudyLine(int studyLineId) {
            return allUsers;
        }

        @Override
        public List<UserInfo> getAllStudentsRelatedToStudyLine(int studyLineId) {
            return students;
        }

        @Override
        public List<UserInfo> getAllLeadersRelatedToStudyLine(int studyLineId) {
            return leaders;
        }

        @Override
        public List<UserInfo> getAllAdvisorsRelatedToStudyLine(int studyLineId) {
            return advisors;
        }
    }

    public static final class FakeProjectRepository implements ProjectManager.ProjectRepository {
        public final Map<String, List<Project>> projectsByUser = new HashMap<>();
        public final Map<Integer, String> creatorEmailByProjectId = new HashMap<>();
        public final Map<String, Boolean> canViewByUserAndProject = new HashMap<>();
        public final Map<Integer, Project> projectsById = new HashMap<>();
        public final List<Project> advisorProjects = new ArrayList<>();
        public final List<Project> allProjects = new ArrayList<>();
        public Project lastCreatedProject;
        public User lastCreatedUser;
        public String lastCreatedStudyLine;
        public int lastCreatedUserId;
        public Project lastCreatedAdvisorProject;
        public String lastCreatedAdvisorStudyLine;
        public int lastCreatedAdvisorUserId;
        public Project lastUpdatedProject;
        public int lastStartedProjectId;
        public int lastKickedProjectId;
        public User lastKickedMember;
        public Integer projectToStartThatIsFinished;
        public Project lastAssignedProject;
        public User lastAssignedAdvisor;
        public int countOfAdvisorProjects;
        public int countOfAdvisorProjectsReturn = 0;
        public int nextProjectId = 1;
        public boolean throwProjectTitleAlreadyInUse;

        private static String key(User user, int projectId) {
            return user.getEmail().asString() + "|" + projectId;
        }

        @Override
        public void create(Project project, User user, int userId) throws ProjectTitleAlreadyInUse {
            if (throwProjectTitleAlreadyInUse) {
                throw new ProjectTitleAlreadyInUse("duplicate");
            }
            lastCreatedProject = project;
            lastCreatedUser = user;
            lastCreatedUserId = userId;
            projectsById.put(project.getProjectID(), project);
            allProjects.add(project);
        }

        @Override
        public void create(Project project, User user, String studyLineName, int userId)
                throws ProjectTitleAlreadyInUse {
            if (throwProjectTitleAlreadyInUse) {
                throw new ProjectTitleAlreadyInUse("duplicate");
            }
            lastCreatedProject = project;
            lastCreatedUser = user;
            lastCreatedStudyLine = studyLineName;
            lastCreatedUserId = userId;
            projectsById.put(project.getProjectID(), project);
            allProjects.add(project);
        }

        @Override
        public List<Project> getAllProjectsRelatedToUser(User user) throws Project.InvalidProject {
            return projectsByUser.getOrDefault(user.getEmail().asString(), List.of());
        }

        @Override
        public List<Project> getAllProjectsRelatedToStudyLine(int studyLineId) {
            return List.of();
        }

        @Override
        public List<Project> getAllProjectsRelatedToStudyLineAndUser(int studyLineId, User user) {
            return List.of();
        }

        @Override
        public boolean canViewProject(User user, int projectId) {
            return canViewByUserAndProject.getOrDefault(key(user, projectId), false);
        }

        @Override
        public String getProjectCreatorEmail(int projectId) {
            return creatorEmailByProjectId.get(projectId);
        }

        @Override
        public void kickMemberFromProject(int projectId, User member) {
            lastKickedProjectId = projectId;
            lastKickedMember = member;
        }

        @Override
        public List<Project> getAllAdvisorProjects(User advisor) {
            return advisorProjects;
        }

        @Override
        public int getCountOfAdvisorProjects(User advisor) {
            return countOfAdvisorProjectsReturn;
        }

        @Override
        public void startProject(int pId) throws ProjectAlreadyFinished {
            if (projectToStartThatIsFinished != null && projectToStartThatIsFinished == pId) {
                throw new ProjectAlreadyFinished("already finished");
            }
            lastStartedProjectId = pId;
        }

        @Override
        public void update(Project project) throws ProjectDoesNotExist {
            lastUpdatedProject = project;
        }

        @Override
        public void advisorCreation(Project project, String studyLineName, int userId) {
            if (throwProjectTitleAlreadyInUse) {
                throw new ProjectTitleAlreadyInUse("duplicate");
            }
            lastCreatedAdvisorProject = project;
            lastCreatedAdvisorStudyLine = studyLineName;
            lastCreatedAdvisorUserId = userId;
            allProjects.add(project);
        }

        @Override
        public void delete(Project project) throws ProjectDoesNotExist {
        }

        @Override
        public List<Project> getAllProjects() throws Project.InvalidProject {
            return allProjects;
        }

        @Override
        public void assignAdvisor(User advisor, Project project) throws AdvisorAlreadyAdvises {
            lastAssignedAdvisor = advisor;
            lastAssignedProject = project;
        }
    }

    public static final class FakeStudyLineRepository implements StudyLineManager.StudyLineRepository {
        public final List<String> studyLines = new ArrayList<>();
        public final Map<Integer, Boolean> canManageByStudyLineId = new HashMap<>();
        public final Map<Integer, Boolean> canViewByStudyLineId = new HashMap<>();
        public final Map<Integer, List<UserManager.UserRepository.UserInfo>> studentsByStudyLineId = new HashMap<>();
        public final Map<Integer, List<UserManager.UserRepository.UserInfo>> leadersByStudyLineId = new HashMap<>();
        public int lastCreatedStudyLineCount;

        @Override
        public void create(StudyLine studyLine) throws StudyLineAlreadyExists {
        }

        @Override
        public List<String> getAllStudyLines() {
            return studyLines;
        }

        @Override
        public String getStudyLineNameById(int studyLineId) {
            return studyLines.isEmpty() ? null : studyLines.get(0);
        }

        @Override
        public boolean canViewStudyLine(User user, int studyLineId) {
            return canViewByStudyLineId.getOrDefault(studyLineId, false);
        }

        @Override
        public boolean canManageStudyLine(User user, int studyLineId) {
            return canManageByStudyLineId.getOrDefault(studyLineId, false);
        }

        @Override
        public int getStudyLineIdByStudylineName(String studyLine) {
            return 0;
        }

        @Override
        public void addStudent(User user, int studyLineId) {
        }

        @Override
        public void addStudyLeader(User user, int studyLineId) {
        }

        @Override
        public void removeUser(User user, int studyLineId) {
        }
    }

    public static final class FakeInvitationRepository implements InvitationManager.InvitationRepository {
        public final List<AdvisorInvitation> advisorInvitesReceived = new ArrayList<>();
        public final List<StudentInvitation> studentInvitesReceived = new ArrayList<>();
        public final List<InvitationManager.InviteView> invitesForProject = new ArrayList<>();
        public final List<InvitationManager.StudentInviteView> studentInvitesForProject = new ArrayList<>();
        public final List<InvitationManager.AdvisorInviteView> invitesReceivedForAdvisor = new ArrayList<>();
        public User lastCreatedAdvisorInviteUser;
        public Project lastCreatedAdvisorInviteProject;
        public User lastCreatedStudentInviteUser;
        public Project lastCreatedStudentInviteProject;
        public AdvisorInvitation lastUpdatedAdvisorInvitation;
        public StudentInvitation lastUpdatedStudentInvitation;

        @Override
        public void create(User invited, Project project) {
            lastCreatedAdvisorInviteUser = invited;
            lastCreatedAdvisorInviteProject = project;
        }

        @Override
        public void createStudentInvitation(User invited, Project project) {
            lastCreatedStudentInviteUser = invited;
            lastCreatedStudentInviteProject = project;
        }

        @Override
        public void delete() {
        }

        @Override
        public void update(AdvisorInvitation advisorInvitation) throws AdvisorAlreadyInvited {
            lastUpdatedAdvisorInvitation = advisorInvitation;
        }

        @Override
        public void updateStudentInvitation(StudentInvitation studentInvitation) {
            lastUpdatedStudentInvitation = studentInvitation;
        }

        @Override
        public List<AdvisorInvitation> getAllInvitesReceived(User invited) {
            return advisorInvitesReceived;
        }

        @Override
        public List<StudentInvitation> getAllStudentInvitesReceived(User invited) {
            return studentInvitesReceived;
        }

        @Override
        public List<InvitationManager.InviteView> getInvitesForProject(int projectId) {
            return invitesForProject;
        }

        @Override
        public List<InvitationManager.StudentInviteView> getStudentInvitesForProject(int projectId) {
            return studentInvitesForProject;
        }

        @Override
        public List<InvitationManager.AdvisorInviteView> getInvitesReceivedForAdvisor(int advisorId) {
            return invitesReceivedForAdvisor;
        }
    }
}