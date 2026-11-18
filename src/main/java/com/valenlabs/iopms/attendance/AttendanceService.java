package com.valenlabs.iopms.attendance;

import com.valenlabs.iopms.attendance.AttendanceDtos.AttendanceResponse;
import com.valenlabs.iopms.attendance.AttendanceDtos.CorrectionRequest;
import com.valenlabs.iopms.common.ApiException;
import com.valenlabs.iopms.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * Module B business rules live here: one open session at a time, no end without start,
 * server-side timestamps, interns never edit attendance, admin corrections are audited.
 */
@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendance;
    private final AttendanceAuditRepository audits;

    @Transactional
    public AttendanceResponse start(User intern) {
        attendance.findByInternIdAndCheckOutIsNull(intern.getId()).ifPresent(s -> {
            throw new ApiException(400, "You already have an open work session");
        });
        Attendance session = new Attendance();
        session.setIntern(intern);
        session.setDate(LocalDate.now());
        session.setCheckIn(LocalDateTime.now());
        return AttendanceResponse.from(attendance.save(session));
    }

    @Transactional
    public AttendanceResponse end(User intern) {
        Attendance session = attendance.findByInternIdAndCheckOutIsNull(intern.getId())
                .orElseThrow(() -> new ApiException(400, "No open work session to end"));
        session.complete(LocalDateTime.now());
        return AttendanceResponse.from(session);
    }

    /** Intern's own history; month param filters to one month (default: current). */
    public List<AttendanceResponse> history(Long internId, YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        return attendance.findByInternIdAndDateBetweenOrderByDateDesc(internId, from, to)
                .stream().map(AttendanceResponse::from).toList();
    }

    public long totalMinutes(Long internId, YearMonth month) {
        return history(internId, month).stream()
                .mapToLong(a -> a.minutes() == null ? 0 : a.minutes()).sum();
    }

    /** Open sessions from previous days = incomplete attendance records. */
    public List<AttendanceResponse> incomplete(Long internId) {
        return attendance.findByInternIdAndCheckOutIsNullAndDateBefore(internId, LocalDate.now())
                .stream().map(AttendanceResponse::from).toList();
    }

    /** Admin view: everyone's sessions on one date. */
    public List<AttendanceResponse> byDate(LocalDate date) {
        return attendance.findByDateOrderByInternAsc(date).stream()
                .map(AttendanceResponse::from).toList();
    }

    /** Admin correction with mandatory reason, recorded in the audit table. */
    @Transactional
    public AttendanceResponse correct(Long attendanceId, CorrectionRequest req, User admin) {
        Attendance session = attendance.findById(attendanceId)
                .orElseThrow(() -> new ApiException(404, "Attendance record not found"));
        if (req.checkOut() != null && req.checkOut().isBefore(req.checkIn())) {
            throw new ApiException(400, "Check-out must be after check-in");
        }

        AttendanceAudit audit = new AttendanceAudit();
        audit.setAttendance(session);
        audit.setAdmin(admin);
        audit.setOldCheckIn(session.getCheckIn());
        audit.setOldCheckOut(session.getCheckOut());
        audit.setNewCheckIn(req.checkIn());
        audit.setNewCheckOut(req.checkOut());
        audit.setReason(req.reason());
        audit.setAt(LocalDateTime.now());
        audits.save(audit);

        session.setCheckIn(req.checkIn());
        session.setCheckOut(req.checkOut());
        if (req.checkOut() == null) {
            session.setMinutes(null);
            session.setCorrection(Attendance.CorrectionState.NONE);
        } else {
            session.complete(req.checkOut());
            session.setCorrection(Attendance.CorrectionState.CORRECTED);
        }
        return AttendanceResponse.from(session);
    }

    public List<AttendanceAudit> auditHistory(Long attendanceId) {
        return audits.findByAttendanceIdOrderByAtDesc(attendanceId);
    }
}
