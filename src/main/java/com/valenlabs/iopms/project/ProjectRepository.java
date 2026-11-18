package com.valenlabs.iopms.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findByInternsId(Long internId);

    long countByStatus(ProjectStatus status);
}
