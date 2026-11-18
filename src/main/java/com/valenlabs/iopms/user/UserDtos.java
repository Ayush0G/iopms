package com.valenlabs.iopms.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Request and response shapes for profiles and admin intern management. */
public final class UserDtos {

    private UserDtos() {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record LoginResponse(String token, String role, String name) {
    }

    /** What any user may see of an account (never the password hash). */
    public record ProfileResponse(Long id, String name, String email, String role, boolean active,
                                  String phone, String college, LocalDate joinedDate, LocalDate endDate) {

        public static ProfileResponse from(User u) {
            return new ProfileResponse(u.getId(), u.getName(), u.getEmail(), u.getRole().name(), u.isActive(),
                    u.getPhone(), u.getCollege(), u.getJoinedDate(), u.getEndDate());
        }
    }

    /** Fields a user may change about themselves. */
    public record ProfileUpdateRequest(@NotBlank String name, String phone, String college) {
    }

    /** Admin creates an intern with a temporary password. */
    public record InternCreateRequest(@NotBlank String name, @NotBlank @Email String email,
                                      @NotBlank @Size(min = 8) String password, String college,
                                      @NotNull @PastOrPresent LocalDate joinedDate) {
    }

    /** Admin edit; password resets the intern's password when present. */
    public record InternUpdateRequest(@NotBlank String name, String phone, String college, Boolean active,
                                      LocalDate joinedDate, LocalDate endDate,
                                      @Size(min = 8, message = "password must be at least 8 characters")
                                      @Pattern(regexp = "^(?!\\s*$).*", message = "password must not be blank")
                                      String password) {
    }
}
