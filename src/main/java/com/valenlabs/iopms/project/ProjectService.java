package com.valenlabs.iopms.project;

import com.valenlabs.iopms.common.ApiException;
import com.valenlabs.iopms.common.ReviewRequest;
import com.valenlabs.iopms.common.ReviewStatus;
import com.valenlabs.iopms.project.ProjectDtos.ProjectRequest;
import com.valenlabs.iopms.project.ProjectDtos.ProjectResponse;
import com.valenlabs.iopms.project.ProjectDtos.SubmissionRequest;
import com.valenlabs.iopms.project.ProjectDtos.SubmissionResponse;
import com.valenlabs.iopms.user.User;
import com.valenlabs.iopms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Projects, submissions with version history, and reviews. */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projects;
    private final ProjectSubmissionRepository submissions;
    private final UserRepository users;

    public List<ProjectResponse> list() {
        return projects.findAll().stream().map(ProjectResponse::from).toList();
    }

    /** Only the projects this intern is assigned to. */
    public List<ProjectResponse> forIntern(Long internId) {
        return projects.findByInternsId(internId).stream().map(ProjectResponse::from).toList();
    }

    @Transactional
    public ProjectResponse create(ProjectRequest req) {
        Project project = new Project();
        apply(project, req);
        return ProjectResponse.from(projects.save(project));
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest req) {
        Project project = projects.findById(id).orElseThrow(() -> new ApiException(404, "Project not found"));
        apply(project, req);
        return ProjectResponse.from(project);
    }

    private void apply(Project project, ProjectRequest req) {
        project.setTitle(req.title());
        project.setDescription(req.description());
        project.setSupervisor(req.supervisor());
        project.setStartDate(req.startDate());
        project.setExpectedEndDate(req.expectedEndDate());
        if (req.status() != null) {
            project.setStatus(req.status());
        }
        List<User> interns = new ArrayList<>();
        for (Long id : req.internIds()) {
            User intern = users.findById(id)
                    .filter(u -> u.getRole() == com.valenlabs.iopms.user.Role.INTERN)
                    .orElseThrow(() -> new ApiException(404, "Intern not found: id " + id));
            interns.add(intern);
        }
        project.setInterns(interns);
    }

    public List<SubmissionResponse> submissionsForProject(Long projectId) {
        return submissions.findByProjectIdOrderByVersionDesc(projectId).stream()
                .map(SubmissionResponse::from).toList();
    }

    public List<SubmissionResponse> submissionsByIntern(Long internId) {
        return submissions.findByInternIdOrderBySubmittedAtDesc(internId).stream()
                .map(SubmissionResponse::from).toList();
    }

    /** A new submission appends to history; earlier versions stay for the audit trail. */
    @Transactional
    public SubmissionResponse submit(User intern, Long projectId, SubmissionRequest req) {
        Project project = projects.findById(projectId)
                .orElseThrow(() -> new ApiException(404, "Project not found"));
        if (project.getInterns().stream().noneMatch(u -> u.getId().equals(intern.getId()))) {
            throw new ApiException(403, "You are not assigned to this project");
        }
        ProjectSubmission submission = new ProjectSubmission();
        submission.setProject(project);
        submission.setIntern(intern);
        submission.setRepoUrl(req.repoUrl());
        submission.setNotes(req.notes());
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setVersion((int) (submissions.findByProjectIdOrderByVersionDesc(projectId).stream()
                .filter(s -> s.getIntern().getId().equals(intern.getId())).count() + 1));
        return SubmissionResponse.from(submissions.save(submission));
    }

    @Transactional
    public SubmissionResponse review(Long submissionId, ReviewRequest req) {
        ProjectSubmission submission = submissions.findById(submissionId)
                .orElseThrow(() -> new ApiException(404, "Submission not found"));
        if (req.status() == ReviewStatus.PENDING) {
            throw new ApiException(400, "Review status must be APPROVED or CHANGES_REQUESTED");
        }
        submission.setStatus(req.status());
        submission.setAdminComments(req.remarks());
        return SubmissionResponse.from(submission);
    }

    /** Numbers for the admin dashboard. */
    public ProjectSummary summary() {
        return new ProjectSummary(projects.countByStatus(ProjectStatus.ACTIVE),
                projects.countByStatus(ProjectStatus.COMPLETED),
                submissions.countByStatus(ReviewStatus.PENDING));
    }

    public record ProjectSummary(long active, long completed, long pendingSubmissions) {
    }
}
