package com.valenlabs.iopms.project;

import com.valenlabs.iopms.common.ReviewStatus;
import com.valenlabs.iopms.user.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Request and response shapes for projects and submissions. */
public final class ProjectDtos {

    private ProjectDtos() {
    }

    public record ProjectRequest(@NotBlank String title, String description, @NotBlank String supervisor,
                                 @NotNull @PastOrPresent LocalDate startDate, LocalDate expectedEndDate,
                                 ProjectStatus status, @NotNull List<Long> internIds) {
    }

    public record ProjectResponse(Long id, String title, String description, String supervisor,
                                  LocalDate startDate, LocalDate expectedEndDate, String status,
                                  List<Long> internIds, List<String> internNames) {

        public static ProjectResponse from(Project p) {
            return new ProjectResponse(p.getId(), p.getTitle(), p.getDescription(), p.getSupervisor(),
                    p.getStartDate(), p.getExpectedEndDate(), p.getStatus().name(),
                    p.getInterns().stream().map(User::getId).toList(),
                    p.getInterns().stream().map(User::getName).toList());
        }
    }

    public record SubmissionRequest(@NotBlank @Pattern(regexp = "^https?://\\S+$",
            message = "repoUrl must be an http(s) URL") String repoUrl, String notes) {
    }

    public record SubmissionResponse(Long id, Long projectId, String projectTitle, Long internId,
                                     String internName, String repoUrl, String notes,
                                     LocalDateTime submittedAt, int version, String status,
                                     String adminComments) {

        public static SubmissionResponse from(ProjectSubmission s) {
            return new SubmissionResponse(s.getId(), s.getProject().getId(), s.getProject().getTitle(),
                    s.getIntern().getId(), s.getIntern().getName(), s.getRepoUrl(), s.getNotes(),
                    s.getSubmittedAt(), s.getVersion(), s.getStatus().name(), s.getAdminComments());
        }
    }
}
