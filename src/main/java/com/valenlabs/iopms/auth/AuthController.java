package com.valenlabs.iopms.user;

import com.valenlabs.iopms.auth.JwtService;
import com.valenlabs.iopms.common.ApiException;
import com.valenlabs.iopms.user.UserDtos.LoginRequest;
import com.valenlabs.iopms.user.UserDtos.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Login only; portal auth is separate from attendance check-in (Module B rule). */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        User user = users.findByEmail(req.email())
                .orElseThrow(() -> new ApiException(401, "Invalid email or password"));
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            throw new ApiException(401, "Invalid email or password");
        }
        if (!user.isActive()) {
            throw new ApiException(403, "This account has been deactivated");
        }
        return new LoginResponse(jwtService.issue(user), user.getRole().name(), user.getName());
    }
}
