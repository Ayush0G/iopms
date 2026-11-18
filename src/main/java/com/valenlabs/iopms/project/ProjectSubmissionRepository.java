package com.valenlabs.iopms.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectSubmissionRepository extends JpaRepository<ProjectSubmission, Long> {

    List<ProjectSubmission> findByProjectIdOrderByVersionDesc(Long projectId);

    List<ProjectSubmission> findByInternIdOrderBySubmittedAtDesc(Long internId);

    long countByStatus(com.valenlabs.iopms.common.ReviewStatus status);
}
