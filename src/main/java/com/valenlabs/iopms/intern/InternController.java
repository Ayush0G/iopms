package com.valenlabs.iopms.intern;

import com.valenlabs.iopms.attendance.AttendanceDtos.AttendanceResponse;
import com.valenlabs.iopms.attendance.AttendanceService;
import com.valenlabs.iopms.common.CurrentUser;
import com.valenlabs.iopms.common.WebUtils;
import com.valenlabs.iopms.project.ProjectDtos.ProjectResponse;
import com.valenlabs.iopms.project.ProjectDtos.SubmissionRequest;
import com.valenlabs.iopms.project.ProjectDtos.SubmissionResponse;
import com.valenlabs.iopms.project.ProjectService;
import com.valenlabs.iopms.report.ReportDtos.ReportRequest;
import com.valenlabs.iopms.report.ReportDtos.ReportResponse;
import com.valenlabs.iopms.report.ReportService;
import com.valenlabs.iopms.task.TaskDtos.ProgressRequest;
import com.valenlabs.iopms.task.TaskDtos.TaskResponse;
import com.valenlabs.iopms.task.TaskService;
import com.valenlabs.iopms.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Every /api/intern endpoint. The intern id always comes from the JWT, never from a request
 * parameter, so an intern can only ever touch their own data.
 */
@RestController
@RequestMapping("/api/intern")
@RequiredArgsConstructor
public class InternController {

    private final CurrentUser currentUser;
    private final AttendanceService attendance;
    private final TaskService tasks;
    private final ReportService reports;
    private final ProjectService projects;

    // ---- Attendance: check-in/check-out is separate from portal login ----
    @PostMapping("/attendance/start")
    public AttendanceResponse startWork() {
        return attendance.start(currentUser.get());
    }

    @PostMapping("/attendance/end")
    public AttendanceResponse endWork() {
        return attendance.end(currentUser.get());
    }

    @GetMapping("/attendance")
    public List<AttendanceResponse> myAttendance(@RequestParam(required = false) String month) {
        User me = currentUser.get();
        return attendance.history(me.getId(), WebUtils.month(month));
    }

    @GetMapping("/attendance/incomplete")
    public List<AttendanceResponse> myIncompleteSessions() {
        return attendance.incomplete(currentUser.get().getId());
    }

    // ---- Tasks ----
    @GetMapping("/tasks")
    public List<TaskResponse> myTasks() {
        return tasks.forIntern(currentUser.get().getId());
    }

    @PutMapping("/tasks/{id}/progress")
    public TaskResponse updateTaskProgress(@PathVariable Long id, @Valid @RequestBody ProgressRequest req) {
        return tasks.updateProgress(currentUser.get().getId(), id, req);
    }

    // ---- Daily reports ----
    @PostMapping("/reports")
    public ReportResponse submitReport(@Valid @RequestBody ReportRequest req) {
        return reports.submit(currentUser.get(), req);
    }

    @GetMapping("/reports")
    public List<ReportResponse> myReports() {
        return reports.mine(currentUser.get().getId());
    }

    // ---- Projects ----
    @GetMapping("/projects")
    public List<ProjectResponse> myProjects() {
        return projects.forIntern(currentUser.get().getId());
    }

    @PostMapping("/projects/{id}/submissions")
    public SubmissionResponse submitProjectWork(@PathVariable Long id,
                                                @Valid @RequestBody SubmissionRequest req) {
        return projects.submit(currentUser.get(), id, req);
    }

    /** Own submission history including admin feedback. */
    @GetMapping("/submissions")
    public List<SubmissionResponse> mySubmissions() {
        return projects.submissionsByIntern(currentUser.get().getId());
    }
}
