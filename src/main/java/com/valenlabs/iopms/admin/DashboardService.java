package com.valenlabs.iopms.admin;

import com.valenlabs.iopms.attendance.AttendanceRepository;
import com.valenlabs.iopms.report.ReportService;
import com.valenlabs.iopms.task.TaskStatus;
import com.valenlabs.iopms.task.TaskService;
import com.valenlabs.iopms.user.Role;
import com.valenlabs.iopms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/** Aggregates the Module F dashboard numbers from the feature services/repositories. */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository users;
    private final AttendanceRepository attendance;
    private final TaskService tasks;
    private final ReportService reports;
    private final com.valenlabs.iopms.project.ProjectService projects;

    public DashboardResponse load() {
        LocalDate today = LocalDate.now();

        long interns = users.countByRole(Role.INTERN);
        long activeInterns = users.countByRoleAndActiveTrue(Role.INTERN);
        long completedInternships = users.countByRoleAndEndDateLessThanEqual(Role.INTERN, today);
        long workingNow = attendance.countByCheckOutIsNullAndDate(today);

        long presentToday = attendance.countByDate(today);
        long hoursToday = attendance.sumMinutesByDate(today) / 60;
        long incompleteSessions = attendance.countByCheckOutIsNullAndDateBefore(today);

        TaskService.TaskSummary task = tasks.summary();
        ReportService.ReportSummary report = reports.summary(today);
        com.valenlabs.iopms.project.ProjectService.ProjectSummary project = projects.summary();

        return new DashboardResponse(
                new TeamOverview(interns, activeInterns, completedInternships, workingNow),
                new AttendanceOverview(presentToday, hoursToday, incompleteSessions),
                new TaskOverview(task.total(), task.completed(), task.pending(), task.overdue()),
                new ReportingOverview(report.submittedToday(), report.missingToday(),
                        report.awaitingReview(), report.approved()),
                new ProjectOverview(project.active(), project.completed(), project.pendingSubmissions()));
    }

    public record DashboardResponse(TeamOverview team, AttendanceOverview attendance, TaskOverview tasks,
                                    ReportingOverview reports, ProjectOverview projects) {
    }

    public record TeamOverview(long interns, long active, long completedInternships, long workingNow) {
    }

    public record AttendanceOverview(long presentToday, long hoursToday, long incompleteSessions) {
    }

    public record TaskOverview(long total, long completed, long pending, long overdue) {
    }

    public record ReportingOverview(long submittedToday, long missingToday, long awaitingReview, long approved) {
    }

    public record ProjectOverview(long active, long completed, long pendingSubmissions) {
    }
}
