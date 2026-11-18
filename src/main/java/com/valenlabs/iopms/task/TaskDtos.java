package com.valenlabs.iopms.task;

import com.valenlabs.iopms.user.User;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

/** Request and response shapes for tasks. */
public final class TaskDtos {

    private TaskDtos() {
    }

    /** Admin create/edit. Status and remarks can be set here too. */
    public record TaskRequest(@NotBlank String title, String description, @NotNull Long assigneeId,
                              Priority priority, @NotNull @PastOrPresent LocalDate startDate,
                              @NotNull LocalDate deadline, TaskStatus status, String adminRemarks) {
    }

    /** Intern update: progress 0-100 plus one of their allowed statuses. */
    public record ProgressRequest(@Min(0) @Max(100) int progress, @NotNull TaskStatus status) {
    }

    public record TaskResponse(Long id, String title, String description, Long assigneeId, String assigneeName,
                               String priority, LocalDate startDate, LocalDate deadline, String status,
                               int progress, String adminRemarks) {

        public static TaskResponse from(Task t) {
            User a = t.getAssignee();
            return new TaskResponse(t.getId(), t.getTitle(), t.getDescription(), a.getId(), a.getName(),
                    t.getPriority() == null ? null : t.getPriority().name(), t.getStartDate(), t.getDeadline(),
                    t.getStatus().name(), t.getProgress(), t.getAdminRemarks());
        }
    }
}
