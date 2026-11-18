package com.valenlabs.iopms.common;

import com.valenlabs.iopms.user.User;
import com.valenlabs.iopms.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Resolves the logged-in user from the JWT filter's SecurityContext. */
@Component
@RequiredArgsConstructor
public class CurrentUser {

    private final UserRepository users;

    public User get() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return users.findByEmail(email)
                .orElseThrow(() -> new ApiException(401, "Not logged in"));
    }
}
