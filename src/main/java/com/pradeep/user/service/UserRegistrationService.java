package com.pradeep.user.service;

import com.pradeep.user.entity.Role;
import com.pradeep.user.entity.User;
import com.pradeep.user.repository.RoleRepository;
import com.pradeep.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates a new user in its own transaction so a unique-key race can be
 * recovered by the caller without poisoning the caller's transaction.
 */
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User register(String email, String googleId, String name, String picture) {
        Role defaultRole = roleRepository.findByName(Role.RoleName.USER)
                .orElseThrow(() -> new IllegalStateException("Default USER role not found"));

        User user = User.builder()
                .email(email)
                .googleId(googleId)
                .name(name)
                .picture(picture)
                .build();
        user.getRoles().add(defaultRole);
        return userRepository.saveAndFlush(user);
    }
}
