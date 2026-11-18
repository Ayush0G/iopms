package com.valenlabs.iopms.report;

import com.valenlabs.iopms.common.ApiException;
import com.valenlabs.iopms.common.ReviewStatus;
import com.valenlabs.iopms.common.ReviewRequest;
import com.valenlabs.iopms.report.ReportDtos.ReportRequest;
import com.valenlabs.iopms.report.ReportDtos.ReportResponse;
import com.valenlabs.iopms.user.User;
import com.valenlabs.iopms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Daily work reports: intern submits, admin reviews (Module D). */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reports;
    private final UserRepository users;

    /** One report per date; second attempt is rejected, not overwritten. */
    @Transactional
    public ReportResponse submit(User intern, ReportRequest req) {
        if (reports.existsByUserIdAndDate(intern.getId(), req.date())) {
            throw new ApiException(409, "You already submitted a report for " + req.date());
        }
        DailyReport report = new DailyReport();
        report.setUser(intern);
        report.setDate(req.date());
        report.setTasksCompleted(req.tasksCompleted());
        report.setWorkPerformed(req.workPerformed());
        report.setTimeSpent(req.timeSpent());
        report.setProgress(req.progress());
        report.setChallenges(req.challenges());
        report.setPendingWork(req.pendingWork());
        report.setNextDayPlan(req.nextDayPlan());
        report.setLinks(req.links());
        report.setSubmittedAt(java.time.LocalDateTime.now());
        return ReportResponse.from(reports.save(report));
    }

    public List<ReportResponse> mine(Long internId) {
        return reports.filter(internId, null, null).stream().map(ReportResponse::from).toList();
    }

    public List<ReportResponse> adminList(Long internId, LocalDate date, ReviewStatus status) {
        return reports.filter(internId, date, status).stream().map(ReportResponse::from).toList();
    }

    /** Only APPROVED and CHANGES_REQUESTED are review outcomes. */
    @Transactional
    public ReportResponse review(Long id, ReviewRequest req) {
        DailyReport report = reports.findById(id).orElseThrow(() -> new ApiException(404, "Report not found"));
        if (req.status() == ReviewStatus.PENDING) {
            throw new ApiException(400, "Review status must be APPROVED or CHANGES_REQUESTED");
        }
        report.setStatus(req.status());
        report.setAdminRemarks(req.remarks());
        return ReportResponse.from(report);
    }

    /** Active interns with no report for the date. */
    public List<ReportDtos.MissingIntern> missing(LocalDate date) {
        return users.findAll().stream()
                .filter(u -> u.getRole() == com.valenlabs.iopms.user.Role.INTERN && u.isActive())
                .filter(u -> !reports.existsByUserIdAndDate(u.getId(), date))
                .map(u -> new ReportDtos.MissingIntern(u.getId(), u.getName(), u.getEmail()))
                .toList();
    }

    /** Numbers for the admin dashboard. */
    public ReportSummary summary(LocalDate today) {
        long submitted = reports.countByDate(today);
        long activeInterns = users.findAll().stream()
                .filter(u -> u.getRole() == com.valenlabs.iopms.user.Role.INTERN && u.isActive()).count();
        return new ReportSummary(submitted, activeInterns - submitted,
                reports.countByStatus(ReviewStatus.PENDING), reports.countByStatus(ReviewStatus.APPROVED));
    }

    public record ReportSummary(long submittedToday, long missingToday, long awaitingReview, long approved) {
    }
}
