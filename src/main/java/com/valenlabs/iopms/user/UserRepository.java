package com.valenlabs.iopms.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByRole(Role role);

    long countByRoleAndActiveTrue(Role role);

    long countByRoleAndEndDateLessThanEqual(Role role, LocalDate date);
}
