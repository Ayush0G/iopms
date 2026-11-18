package com.valenlabs.iopms.attendance;

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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One work session. Timestamps are always set by the server; interns have no endpoint to edit them.
 * minutes only becomes non-null when the session ends.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Attendance {

    public enum CorrectionState { NONE, CORRECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private User intern;

    private LocalDate date;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private Long minutes;

    @Enumerated(EnumType.STRING)
    private CorrectionState correction = CorrectionState.NONE;

    /** Working hours are always derived here, never on the client. */
    public void complete(LocalDateTime end) {
        this.checkOut = end;
        this.minutes = Duration.between(checkIn, end).toMinutes();
    }
}
