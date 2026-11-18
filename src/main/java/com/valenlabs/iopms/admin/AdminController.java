package com.valenlabs.iopms.admin;

import com.valenlabs.iopms.admin.InternReportService.InternReportResponse;
import com.valenlabs.iopms.attendance.AttendanceAudit;
import com.valenlabs.iopms.attendance.AttendanceDtos.AttendanceResponse;
import com.valenlabs.iopms.attendance.AttendanceDtos.CorrectionRequest;
import com.valenlabs.iopms.attendance.AttendanceService;
import com.valenlabs.iopms.common.CurrentUser;
import com.valenlabs.iopms.common.ReviewRequest;
import com.valenlabs.iopms.common.ReviewStatus;
import com.valenlabs.iopms.common.WebUtils;
import com.valenlabs.iopms.project.ProjectDtos.ProjectRequest;
import com.valenlabs.iopms.project.ProjectDtos.ProjectResponse;
import com.valenlabs.iopms.project.ProjectDtos.SubmissionResponse;
import com.valenlabs.iopms.project.ProjectService;
import com.valenlabs.iopms.report.ReportDtos;
import com.valenlabs.iopms.report.ReportDtos.ReportResponse;
import com.valenlabs.iopms.report.ReportService;
import com.valenlabs.iopms.task.TaskDtos.TaskRequest;
import com.valenlabs.iopms.task.TaskDtos.TaskResponse;
import com.valenlabs.iopms.task.TaskService;
import com.valenlabs.iopms.task.TaskStatus;
import com.valenlabs.iopms.user.InternService;
import com.valenlabs.iopms.user.UserDtos.InternCreateRequest;
import com.valenlabs.iopms.user.UserDtos.InternUpdateRequest;
import com.valenlabs.iopms.user.UserDtos.ProfileResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Every /api/admin endpoint, routing to the feature services. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final InternService interns;
    private final AttendanceService attendance;
    private final TaskService tasks;
    private final ReportService reports;
    private final ProjectService projects;
    private final DashboardService dashboard;
    private final InternReportService internReports;
    private final CurrentUser currentUser;

    // ---- Module F: dashboard ----
    @GetMapping("/dashboard")
    public DashboardService.DashboardResponse dashboard() {
        return dashboard.load();
    }

    // ---- Module A: intern accounts ----
    @GetMapping("/interns")
    public List<ProfileResponse> interns() {
        return interns.list();
    }

    @PostMapping("/interns")
    public ProfileResponse createIntern(@Valid @RequestBody InternCreateRequest req) {
        return interns.create(req);
    }

    @PutMapping("/interns/{id}")
    public ProfileResponse updateIntern(@PathVariable Long id, @Valid @RequestBody InternUpdateRequest req) {
        return interns.update(id, req);
    }

    // ---- Module G: consolidated intern report + CSV ----
    @GetMapping("/interns/{id}/report")
    public InternReportResponse internReport(@PathVariable Long id,
                                             @RequestParam(required = false) String month) {
        return internReports.build(id, WebUtils.month(month));
    }

    @GetMapping("/interns/{id}/report.csv")
    public ResponseEntity<String> internReportCsv(@PathVariable Long id,
                                                  @RequestParam(required = false) String month) {
        String csv = internReports.csv(id, WebUtils.month(month));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=intern-" + id + "-report.csv")
                .contentType(MediaType.valueOf("text/csv"))
                .body(csv);
    }

    // ---- Module B: attendance ----
    @GetMapping("/attendance")
    public List<AttendanceResponse> attendanceByDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return attendance.byDate(date);
    }

    @GetMapping("/attendance/intern/{internId}")
    public List<AttendanceResponse> attendanceForIntern(@PathVariable Long internId,
                                                        @RequestParam(required = false) String month) {
        return attendance.history(internId, WebUtils.month(month));
    }

    @PostMapping("/attendance/{id}/correct")
    public AttendanceResponse correctAttendance(@PathVariable Long id,
                                                @Valid @RequestBody CorrectionRequest req) {
        return attendance.correct(id, req, currentUser.get());
    }

    @GetMapping("/attendance/{id}/audit")
    public List<AttendanceAudit> attendanceAudit(@PathVariable Long id) {
        return attendance.auditHistory(id);
    }

    // ---- Module C: tasks ----
    @GetMapping("/tasks")
    public List<TaskResponse> tasks(@RequestParam(required = false) Long internId,
                                    @RequestParam(required = false) TaskStatus status) {
        return tasks.filter(internId, status);
    }

    @PostMapping("/tasks")
    public TaskResponse createTask(@Valid @RequestBody TaskRequest req) {
        return tasks.create(req);
    }

    @PutMapping("/tasks/{id}")
    public TaskResponse updateTask(@PathVariable Long id, @Valid @RequestBody TaskRequest req) {
        return tasks.update(id, req);
    }

    // ---- Module D: daily reports ----
    @GetMapping("/reports")
    public List<ReportResponse> reports(@RequestParam(required = false) Long internId,
                                        @RequestParam(required = false)
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                        @RequestParam(required = false) ReviewStatus status) {
        return reports.adminList(internId, date, status);
    }

    @PostMapping("/reports/{id}/review")
    public ReportResponse reviewReport(@PathVariable Long id, @Valid @RequestBody ReviewRequest req) {
        return reports.review(id, req);
    }

    @GetMapping("/reports/missing")
    public List<ReportDtos.MissingIntern> missingReports(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reports.missing(date);
    }

    // ---- Module E: projects and submissions ----
    @GetMapping("/projects")
    public List<ProjectResponse> projects() {
        return projects.list();
    }

    @PostMapping("/projects")
    public ProjectResponse createProject(@Valid @RequestBody ProjectRequest req) {
        return projects.create(req);
    }

    @PutMapping("/projects/{id}")
    public ProjectResponse updateProject(@PathVariable Long id, @Valid @RequestBody ProjectRequest req) {
        return projects.update(id, req);
    }

    @GetMapping("/projects/{id}/submissions")
    public List<SubmissionResponse> projectSubmissions(@PathVariable Long id) {
        return projects.submissionsForProject(id);
    }

    @PostMapping("/submissions/{id}/review")
    public SubmissionResponse reviewSubmission(@PathVariable Long id, @Valid @RequestBody ReviewRequest req) {
        return projects.review(id, req);
    }
}
