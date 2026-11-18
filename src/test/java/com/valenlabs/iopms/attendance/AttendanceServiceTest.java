package com.valenlabs.iopms.attendance;

import com.valenlabs.iopms.attendance.AttendanceDtos.CorrectionRequest;
import com.valenlabs.iopms.common.ApiException;
import com.valenlabs.iopms.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests for the Module B business rules. */
@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendance;
    @Mock
    private AttendanceAuditRepository audits;
    @InjectMocks
    private AttendanceService service;

    private User user(long id) {
        User u = new User();
        u.setId(id);
        u.setName("Intern " + id);
        return u;
    }

    private Attendance openSession(User intern, LocalDateTime checkIn) {
        Attendance a = new Attendance();
        a.setIntern(intern);
        a.setDate(checkIn.toLocalDate());
        a.setCheckIn(checkIn);
        return a;
    }

    @Test
    void startRejectsSecondOpenSession() {
        User u = user(1);
        when(attendance.findByInternIdAndCheckOutIsNull(1L))
                .thenReturn(Optional.of(openSession(u, LocalDateTime.now().minusHours(1))));

        ApiException ex = assertThrows(ApiException.class, () -> service.start(u));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void endWithoutOpenSessionFails() {
        User u = user(1);
        when(attendance.findByInternIdAndCheckOutIsNull(1L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.end(u));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void endComputesHoursFromServerClock() {
        User u = user(1);
        when(attendance.findByInternIdAndCheckOutIsNull(1L))
                .thenReturn(Optional.of(openSession(u, LocalDateTime.now().minusHours(1))));

        AttendanceDtos.AttendanceResponse resp = service.end(u);
        long expected = Duration.between(resp.checkIn(), resp.checkOut()).toMinutes();
        assertEquals(expected, resp.minutes());
        assertEquals(60, resp.minutes(), 1, "about one hour worked");
    }

    @Test
    void correctionWritesAuditAndRecomputes() {
        User u = user(1);
        User admin = user(9);
        LocalDateTime originalIn = LocalDateTime.now().minusHours(2);
        Attendance session = openSession(u, originalIn);
        session.setId(5L);
        when(attendance.findById(5L)).thenReturn(Optional.of(session));
        LocalDateTime in = LocalDateTime.now().minusHours(3);
        LocalDateTime out = LocalDateTime.now().minusHours(1);

        AttendanceDtos.AttendanceResponse resp =
                service.correct(5L, new CorrectionRequest(in, out, "forgot to end"), admin);

        assertEquals(Duration.between(in, out).toMinutes(), resp.minutes());
        assertEquals(Attendance.CorrectionState.CORRECTED, session.getCorrection());
        assertEquals(in, session.getCheckIn());

        ArgumentCaptor<AttendanceAudit> audit = ArgumentCaptor.forClass(AttendanceAudit.class);
        verify(audits).save(audit.capture());
        assertEquals("forgot to end", audit.getValue().getReason());
        assertEquals(admin.getId(), audit.getValue().getAdmin().getId());
        assertEquals(originalIn, audit.getValue().getOldCheckIn());
    }

    @Test
    void correctionRejectsCheckOutBeforeCheckIn() {
        User u = user(1);
        Attendance session = openSession(u, LocalDateTime.now().minusHours(2));
        when(attendance.findById(5L)).thenReturn(Optional.of(session));

        ApiException ex = assertThrows(ApiException.class, () -> service.correct(5L,
                new CorrectionRequest(LocalDateTime.now(), LocalDateTime.now().minusHours(1), "bad data"),
                user(9)));
        assertEquals(400, ex.getStatus());
        verify(attendance, org.mockito.Mockito.never()).save(any());
    }
}
