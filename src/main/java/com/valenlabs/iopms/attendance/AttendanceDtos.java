package com.valenlabs.iopms.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/** Request shapes for attendance. */
public final class AttendanceDtos {

    private AttendanceDtos() {
    }

    /** Admin correction; reason is mandatory per the assessment rules. */
    public record CorrectionRequest(@NotNull LocalDateTime checkIn, LocalDateTime checkOut,
                                    @NotBlank String reason) {
    }

    public record AttendanceResponse(Long id, Long internId, String internName, LocalDate date,
                                     LocalDateTime checkIn, LocalDateTime checkOut, Long minutes,
                                     String correction) {

        public static AttendanceResponse from(Attendance a) {
            return new AttendanceResponse(a.getId(), a.getIntern().getId(), a.getIntern().getName(),
                    a.getDate(), a.getCheckIn(), a.getCheckOut(), a.getMinutes(),
                    a.getCorrection() == Attendance.CorrectionState.CORRECTED ? "CORRECTED" : "NONE");
        }
    }
}
