package com.valenlabs.iopms.project;

import com.valenlabs.iopms.common.ReviewStatus;
import com.valenlabs.iopms.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** One deliverable submission; each new submission gets the next version number. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class ProjectSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Project project;

    @ManyToOne(optional = false)
    private User intern;

    private String repoUrl;
    private String notes;
    private LocalDateTime submittedAt;
    private int version;

    @Enumerated(EnumType.STRING)
    private ReviewStatus status = ReviewStatus.PENDING;

    private String adminComments;
}
