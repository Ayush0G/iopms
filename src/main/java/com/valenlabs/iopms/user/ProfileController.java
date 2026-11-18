package com.valenlabs.iopms.user;

import com.valenlabs.iopms.common.CurrentUser;
import com.valenlabs.iopms.user.UserDtos.ProfileResponse;
import com.valenlabs.iopms.user.UserDtos.ProfileUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Any logged-in user views and edits their own permitted profile fields. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ProfileController {

    private final CurrentUser currentUser;

    @GetMapping("/me")
    public ProfileResponse me() {
        return ProfileResponse.from(currentUser.get());
    }

    @PutMapping("/me")
    public ProfileResponse update(@Valid @RequestBody ProfileUpdateRequest req) {
        User user = currentUser.get();
        user.setName(req.name());
        user.setPhone(req.phone());
        user.setCollege(req.college());
        return ProfileResponse.from(user);
    }
}
