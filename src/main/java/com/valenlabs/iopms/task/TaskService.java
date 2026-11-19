package com.valenlabs.iopms.task;

import com.valenlabs.iopms.common.ApiException;
import com.valenlabs.iopms.task.TaskDtos.ProgressRequest;
import com.valenlabs.iopms.task.TaskDtos.TaskRequest;
import com.valenlabs.iopms.task.TaskDtos.TaskResponse;
import com.valenlabs.iopms.user.User;
import com.valenlabs.iopms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Task allocation (admin) and progress updates (intern). */
@Service
@RequiredArgsConstructor
public class TaskService {

    /** Statuses an intern may set; the rest are admin decisions. */
    private static final List<TaskStatus> INTERN_ALLOWED =
            List.of(TaskStatus.NOT_STARTED, TaskStatus.IN_PROGRESS, TaskStatus.UNDER_REVIEW);

    private final TaskRepository tasks;
    private final UserRepository users;

    public List<TaskResponse> filter(Long internId, TaskStatus status) {
        return tasks.filter(internId, status).stream().map(TaskResponse::from).toList();
    }

    public List<TaskResponse> forIntern(Long internId) {
        return tasks.findByAssigneeIdOrderByDeadlineAsc(internId).stream().map(TaskResponse::from).toList();
    }

    @Transactional
    public TaskResponse create(TaskRequest req) {
        Task task = new Task();
        apply(task, req);
        return TaskResponse.from(tasks.save(task));
    }

    @Transactional
    public TaskResponse update(Long id, TaskRequest req) {
        Task task = tasks.findById(id).orElseThrow(() -> new ApiException(404, "Task not found"));
        apply(task, req);
        return TaskResponse.from(task);
    }

    private void apply(Task task, TaskRequest req) {
        if (req.deadline() != null && req.startDate() != null && req.deadline().isBefore(req.startDate())) {
            throw new ApiException(400, "Deadline cannot be before the start date");
        }
        User assignee = users.findById(req.assigneeId())
                .orElseThrow(() -> new ApiException(404, "Assigned intern not found"));
        task.setTitle(req.title());
        task.setDescription(req.description());
        task.setAssignee(assignee);
        task.setPriority(req.priority());
        task.setStartDate(req.startDate());
        task.setDeadline(req.deadline());
        if (req.status() != null) {
            task.setStatus(req.status());
        }
        if (req.adminRemarks() != null) {
            task.setAdminRemarks(req.adminRemarks());
        }
    }

    /** Intern updates progress on their own task only, and only into allowed statuses. */
    @Transactional
    public TaskResponse updateProgress(Long internId, Long taskId, ProgressRequest req) {
        Task task = tasks.findById(taskId).orElseThrow(() -> new ApiException(404, "Task not found"));
        if (!task.getAssignee().getId().equals(internId)) {
            throw new ApiException(403, "This task is not assigned to you");
        }
        if (!INTERN_ALLOWED.contains(req.status())) {
            throw new ApiException(400, "Interns may only set NOT_STARTED, IN_PROGRESS or UNDER_REVIEW");
        }
        task.setProgress(req.progress());
        task.setStatus(req.status());
        return TaskResponse.from(task);
    }

    /** Numbers for the admin dashboard. */
    public TaskSummary summary() {
        long total = tasks.count();
        long completed = tasks.countByStatus(TaskStatus.COMPLETED);
        long pending = tasks.countByStatus(TaskStatus.NOT_STARTED) + tasks.countByStatus(TaskStatus.IN_PROGRESS)
                + tasks.countByStatus(TaskStatus.UNDER_REVIEW) + tasks.countByStatus(TaskStatus.CHANGES_REQUESTED);
        long overdue = tasks.countByDeadlineBeforeAndStatusNot(LocalDate.now(), TaskStatus.COMPLETED);
        return new TaskSummary(total, completed, pending, overdue);
    }

    public record TaskSummary(long total, long completed, long pending, long overdue) {
    }
}
