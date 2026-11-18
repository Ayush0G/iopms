package com.valenlabs.iopms.attendance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByInternIdAndCheckOutIsNull(Long internId);

    List<Attendance> findByInternIdOrderByDateDescCheckInDesc(Long internId);

    List<Attendance> findByInternIdAndDateBetweenOrderByDateDesc(Long internId, LocalDate from, LocalDate to);

    List<Attendance> findByDateOrderByInternAsc(LocalDate date);

    List<Attendance> findByInternIdAndCheckOutIsNullAndDateBefore(Long internId, LocalDate date);
}
