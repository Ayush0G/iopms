package com.valenlabs.iopms.attendance;

import com.valenlabs.iopms.user.User;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Immutable audit row: every admin correction with before/after values and the reason. */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class AttendanceAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Attendance attendance;

    @ManyToOne(optional = false)
    private User admin;

    private LocalDateTime oldCheckIn;
    private LocalDateTime oldCheckOut;
    private LocalDateTime newCheckIn;
    private LocalDateTime newCheckOut;
    private String reason;
    private LocalDateTime at;
}
