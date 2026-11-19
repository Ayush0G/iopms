package com.valenlabs.iopms.user;

import com.valenlabs.iopms.common.ApiException;
import com.valenlabs.iopms.user.UserDtos.InternCreateRequest;
import com.valenlabs.iopms.user.UserDtos.InternUpdateRequest;
import com.valenlabs.iopms.user.UserDtos.ProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Admin management of intern accounts (Module A). Admins are seeded, never created via API. */
@Service
@RequiredArgsConstructor
public class InternService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public List<ProfileResponse> list() {
        return users.findAll().stream()
                .filter(u -> u.getRole() == Role.INTERN)
                .map(ProfileResponse::from)
                .toList();
    }

    @Transactional
    public ProfileResponse create(InternCreateRequest req) {
        if (users.existsByEmail(req.email())) {
            throw new ApiException(409, "An account with this email already exists");
        }
        User user = new User();
        user.setName(req.name());
        user.setEmail(req.email());
        user.setPasswordHash(encoder.encode(req.password()));
        user.setCollege(req.college());
        user.setRole(Role.INTERN);
        user.setJoinedDate(req.joinedDate());
        User saved = users.save(user);
        return ProfileResponse.from(saved);
    }

    @Transactional
    public ProfileResponse update(Long id, InternUpdateRequest req) {
        User user = users.findById(id)
                .orElseThrow(() -> new ApiException(404, "Intern not found"));
        if (req.endDate() != null && req.joinedDate() != null && req.endDate().isBefore(req.joinedDate())) {
            throw new ApiException(400, "End date cannot be before the joining date");
        }
        user.setName(req.name());
        user.setPhone(req.phone());
        user.setCollege(req.college());
        user.setJoinedDate(req.joinedDate());
        user.setEndDate(req.endDate());
        if (req.active() != null) {
            user.setActive(req.active());
        }
        if (req.password() != null) {
            user.setPasswordHash(encoder.encode(req.password()));
        }
        return ProfileResponse.from(user);
    }
}
