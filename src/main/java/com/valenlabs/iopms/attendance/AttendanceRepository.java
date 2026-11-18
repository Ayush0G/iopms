package com.valenlabs.iopms.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByInternIdAndCheckOutIsNull(Long internId);

    List<Attendance> findByInternIdOrderByDateDescCheckInDesc(Long internId);

    List<Attendance> findByInternIdAndDateBetweenOrderByDateDesc(Long internId, LocalDate from, LocalDate to);

    List<Attendance> findByDateOrderByInternAsc(LocalDate date);

    List<Attendance> findByInternIdAndCheckOutIsNullAndDateBefore(Long internId, LocalDate date);

    long countByDate(LocalDate date);

    long countByCheckOutIsNullAndDate(LocalDate date);

    long countByCheckOutIsNullAndDateBefore(LocalDate date);

    @Query("select coalesce(sum(a.minutes), 0) from Attendance a where a.date = :date")
    long sumMinutesByDate(@Param("date") LocalDate date);
}
