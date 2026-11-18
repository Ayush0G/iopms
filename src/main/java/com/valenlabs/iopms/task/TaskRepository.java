package com.valenlabs.iopms.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    /** One query serves every admin filter combination (internId/status both optional). */
    @Query("select t from Task t where (:internId is null or t.assignee.id = :internId) "
            + "and (:status is null or t.status = :status) order by t.deadline asc nulls last")
    List<Task> filter(@Param("internId") Long internId, @Param("status") TaskStatus status);

    List<Task> findByAssigneeIdOrderByDeadlineAsc(Long assigneeId);

    long countByStatus(TaskStatus status);

    long countByDeadlineBeforeAndStatusNot(LocalDate deadline, TaskStatus status);
}
