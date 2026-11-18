package com.valenlabs.iopms.user;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Intern or admin account. Deactivated accounts keep their history, so rows are never deleted.
 * Table name "users" because "user" is reserved in PostgreSQL.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String passwordHash;
    private String phone;
    private String college;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean active = true;
    private LocalDate joinedDate;
    private LocalDate endDate;
}
