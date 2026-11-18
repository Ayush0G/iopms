package com.valenlabs.iopms.report;

import com.valenlabs.iopms.common.ReviewStatus;
import com.valenlabs.iopms.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One daily report per intern per date, enforced by the unique index. */
@Entity
@Table(name = "daily_reports",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "date"}))
@Getter
@Setter
@NoArgsConstructor
public class DailyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User user;

    private LocalDate date;
    private String tasksCompleted;
    private String workPerformed;
    private String timeSpent;
    private int progress;
    private String challenges;
    private String pendingWork;
    private String nextDayPlan;
    private String links;
    private LocalDateTime submittedAt;

    @Enumerated(EnumType.STRING)
    private ReviewStatus status = ReviewStatus.PENDING;

    private String adminRemarks;
}
