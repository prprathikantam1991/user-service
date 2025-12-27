package com.pradeep.user.service;

import com.pradeep.user.entity.Role;
import com.pradeep.user.entity.User;
import com.pradeep.user.repository.RoleRepository;
import com.pradeep.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /**
     * Create a new user and assign default USER role atomically
     */
    @Transactional
    public User createUserWithDefaultRole(String email, String googleId, String name, String picture) {
        log.info("Creating new user with default role: {}", email);
        
        // Check if user already exists
        Optional<User> existingUser = userRepository.findByEmail(email)
                .or(() -> userRepository.findByGoogleId(googleId));
        
        if (existingUser.isPresent()) {
            log.warn("User already exists: {}", email);
            throw new RuntimeException("User already exists: " + email);
        }
        
        // Get default USER role
        Role defaultRole = roleRepository.findByName(Role.RoleName.USER)
                .orElseThrow(() -> new RuntimeException("Default USER role not found. Please initialize roles."));
        
        // Create user with default role
        User newUser = User.builder()
                .email(email)
                .googleId(googleId)
                .name(name)
                .picture(picture)
                .build();
        
        newUser.getRoles().add(defaultRole);
        
        return userRepository.save(newUser);
    }

    /**
     * Update user information (name, picture)
     */
    @Transactional
    public User updateUser(String email, String name, String picture) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    boolean updated = false;
                    if (name != null && !name.equals(user.getName())) {
                        user.setName(name);
                        updated = true;
                    }
                    if (picture != null && !picture.equals(user.getPicture())) {
                        user.setPicture(picture);
                        updated = true;
                    }
                    if (updated) {
                        log.info("Updating user info: {}", email);
                        return userRepository.save(user);
                    }
                    return user;
                })
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    /**
     * Create or update user (used during OAuth2 callback)
     * If user exists, updates info; if not, creates with default role
     */
    @Transactional
    public User createOrUpdateUser(String email, String googleId, String name, String picture) {
        // Try to find user by email first, then by Google ID
        Optional<User> existingUser = userRepository.findByEmail(email)
                .or(() -> userRepository.findByGoogleId(googleId));

        if (existingUser.isPresent()) {
            // User exists - update info
            User user = existingUser.get();
            boolean updated = false;
            if (name != null && !name.equals(user.getName())) {
                user.setName(name);
                updated = true;
            }
            if (picture != null && !picture.equals(user.getPicture())) {
                user.setPicture(picture);
                updated = true;
            }
            if (updated) {
                log.info("Updating user info: {}", email);
                return userRepository.save(user);
            }
            return user;
        } else {
            // User doesn't exist - create with default role
            return createUserWithDefaultRole(email, googleId, name, picture);
        }
    }

    /**
     * Get user by email with roles
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmailWithRoles(email);
    }

    /**
     * Get user by Google ID with roles
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByGoogleId(String googleId) {
        return userRepository.findByGoogleIdWithRoles(googleId);
    }

    /**
     * Assign a role to a user by email
     */
    @Transactional
    public User assignRoleToUser(String email, Role.RoleName roleName) {
        User user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        if (!user.getRoles().contains(role)) {
            user.getRoles().add(role);
            userRepository.save(user);
            log.info("Assigned role {} to user {}", roleName, email);
        } else {
            log.debug("User {} already has role {}", email, roleName);
        }
        
        return user;
    }

    /**
     * Remove a role from a user by email
     */
    @Transactional
    public User removeRoleFromUser(String email, Role.RoleName roleName) {
        User user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        if (user.getRoles().contains(role)) {
            user.getRoles().remove(role);
            userRepository.save(user);
            log.info("Removed role {} from user {}", roleName, email);
        } else {
            log.debug("User {} does not have role {}", email, roleName);
        }
        
        return user;
    }

    /**
     * Get user's roles
     */
    @Transactional(readOnly = true)
    public Set<Role> getUserRoles(String email) {
        return userRepository.findByEmailWithRoles(email)
                .map(User::getRoles)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    /**
     * Get Spring Security authorities for a user
     */
    @Transactional(readOnly = true)
    public Collection<GrantedAuthority> getUserAuthorities(String email) {
        return userRepository.findByEmailWithRoles(email)
                .map(user -> {
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    user.getRoles().forEach(role -> {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().name()));
                    });
                    return authorities;
                })
                .orElse(Collections.emptyList());
    }

    /**
     * Get Spring Security authorities for a user by Google ID
     */
    @Transactional(readOnly = true)
    public Collection<GrantedAuthority> getUserAuthoritiesByGoogleId(String googleId) {
        return userRepository.findByGoogleIdWithRoles(googleId)
                .map(user -> {
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    user.getRoles().forEach(role -> {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().name()));
                    });
                    return authorities;
                })
                .orElse(Collections.emptyList());
    }
}

