package com.valenlabs.iopms.report;

import com.valenlabs.iopms.common.ReviewStatus;
import com.valenlabs.iopms.user.User;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Request and response shapes for daily work reports. */
public final class ReportDtos {

    private ReportDtos() {
    }

    public record ReportRequest(@NotNull @PastOrPresent LocalDate date,
                                @NotBlank String tasksCompleted,
                                @NotBlank String workPerformed,
                                @NotBlank String timeSpent,
                                @Min(0) @Max(100) int progress,
                                String challenges,
                                @NotBlank String pendingWork,
                                @NotBlank String nextDayPlan,
                                String links) {
    }

    public record ReportResponse(Long id, Long internId, String internName, LocalDate date,
                                 String tasksCompleted, String workPerformed, String timeSpent, int progress,
                                 String challenges, String pendingWork, String nextDayPlan, String links,
                                 LocalDateTime submittedAt, String status, String adminRemarks) {

        public static ReportResponse from(DailyReport r) {
            User u = r.getUser();
            return new ReportResponse(r.getId(), u.getId(), u.getName(), r.getDate(), r.getTasksCompleted(),
                    r.getWorkPerformed(), r.getTimeSpent(), r.getProgress(), r.getChallenges(),
                    r.getPendingWork(), r.getNextDayPlan(), r.getLinks(), r.getSubmittedAt(),
                    r.getStatus().name(), r.getAdminRemarks());
        }
    }

    /** Active intern with no report on a date. */
    public record MissingIntern(Long id, String name, String email) {
    }
}
