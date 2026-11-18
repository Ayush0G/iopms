package com.valenlabs.iopms.report;

import com.valenlabs.iopms.common.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<DailyReport, Long> {

    Optional<DailyReport> findByUserIdAndDate(Long userId, LocalDate date);

    boolean existsByUserIdAndDate(Long userId, LocalDate date);

    /** One query serves every admin filter combination. */
    @Query("select r from DailyReport r where (:internId is null or r.user.id = :internId) "
            + "and (:date is null or r.date = :date) and (:status is null or r.status = :status) "
            + "order by r.date desc")
    List<DailyReport> filter(@Param("internId") Long internId, @Param("date") LocalDate date,
                             @Param("status") ReviewStatus status);

    long countByDate(LocalDate date);

    long countByStatus(ReviewStatus status);
}
