package com.valenlabs.iopms.admin;

import com.valenlabs.iopms.attendance.AttendanceDtos.AttendanceResponse;
import com.valenlabs.iopms.attendance.AttendanceService;
import com.valenlabs.iopms.common.ApiException;
import com.valenlabs.iopms.common.WebUtils;
import com.valenlabs.iopms.project.ProjectDtos.SubmissionResponse;
import com.valenlabs.iopms.project.ProjectService;
import com.valenlabs.iopms.report.ReportDtos.ReportResponse;
import com.valenlabs.iopms.report.ReportService;
import com.valenlabs.iopms.task.TaskDtos.TaskResponse;
import com.valenlabs.iopms.task.TaskService;
import com.valenlabs.iopms.user.User;
import com.valenlabs.iopms.user.UserDtos.ProfileResponse;
import com.valenlabs.iopms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;

/** Module G: everything about one intern in one payload, with optional date filter. */
@Service
@RequiredArgsConstructor
public class InternReportService {

    private final UserRepository users;
    private final AttendanceService attendanceService;
    private final TaskService taskService;
    private final ReportService reportService;
    private final ProjectService projectService;

    public InternReportResponse build(Long internId, YearMonth month) {
        User intern = users.findById(internId)
                .orElseThrow(() -> new ApiException(404, "Intern not found"));
        List<AttendanceResponse> attendance = attendanceService.history(internId, month);
        return new InternReportResponse(
                ProfileResponse.from(intern),
                attendance,
                attendance.stream().mapToLong(a -> a.minutes() == null ? 0 : a.minutes()).sum(),
                taskService.forIntern(internId),
                reportService.mine(internId),
                projectService.submissionsByIntern(internId));
    }

    public record InternReportResponse(ProfileResponse profile,
                                       List<AttendanceResponse> attendance,
                                       long totalMinutes,
                                       List<TaskResponse> tasks,
                                       List<ReportResponse> reports,
                                       List<SubmissionResponse> submissions) {
    }

    /** Flat CSV with a labeled section per table; good enough for the "CSV or PDF" requirement. */
    public String csv(Long internId, YearMonth month) {
        InternReportResponse r = build(internId, month);
        StringBuilder sb = new StringBuilder("IOPMS intern report,,,\n");
        ProfileResponse p = r.profile();
        sb.append("Name,").append(WebUtils.csv(p.name()))
                .append("\nEmail,").append(WebUtils.csv(p.email()))
                .append("\nActive,").append(p.active())
                .append("\nCollege,").append(WebUtils.csv(p.college()))
                .append("\nJoined,").append(p.joinedDate())
                .append("\nEnd date,").append(p.endDate() == null ? "" : p.endDate())
                .append("\nTotal working hours,").append(r.totalMinutes() / 60).append("h ")
                .append(r.totalMinutes() % 60).append("m\n");

        sb.append("\nATTENDANCE\nDate,Check-in,Check-out,Minutes,Corrected\n");
        for (AttendanceResponse a : r.attendance()) {
            sb.append(a.date()).append(',').append(a.checkIn()).append(',').append(a.checkOut()).append(',')
                    .append(a.minutes() == null ? "" : a.minutes()).append(',').append(a.correction()).append('\n');
        }

        sb.append("\nTASKS\nTitle,Priority,Deadline,Status,Progress,Admin remarks\n");
        for (TaskResponse t : r.tasks()) {
            sb.append(WebUtils.csv(t.title())).append(',').append(t.priority()).append(',').append(t.deadline())
                    .append(',').append(t.status()).append(',').append(t.progress()).append(',')
                    .append(WebUtils.csv(t.adminRemarks())).append('\n');
        }

        sb.append("\nDAILY REPORTS\nDate,Tasks completed,Progress,Status,Admin remarks\n");
        for (ReportResponse rep : r.reports()) {
            sb.append(rep.date()).append(',').append(WebUtils.csv(rep.tasksCompleted())).append(',')
                    .append(rep.progress()).append(',').append(rep.status()).append(',')
                    .append(WebUtils.csv(rep.adminRemarks())).append('\n');
        }

        sb.append("\nPROJECT SUBMISSIONS\nProject,Version,Repo URL,Status,Admin comments,Submitted at\n");
        for (SubmissionResponse s : r.submissions()) {
            sb.append(WebUtils.csv(s.projectTitle())).append(',').append(s.version()).append(',')
                    .append(WebUtils.csv(s.repoUrl())).append(',').append(s.status()).append(',')
                    .append(WebUtils.csv(s.adminComments())).append(',').append(s.submittedAt()).append('\n');
        }
        return sb.toString();
    }
}
