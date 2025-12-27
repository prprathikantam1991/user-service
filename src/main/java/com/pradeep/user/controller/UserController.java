package com.pradeep.user.controller;

import com.pradeep.user.dto.*;
import com.pradeep.user.entity.Role;
import com.pradeep.user.entity.User;
import com.pradeep.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * Create user with default role
     * POST /api/users
     */
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("Creating user: {}", request.getEmail());
        try {
            User user = userService.createUserWithDefaultRole(
                    request.getEmail(),
                    request.getGoogleId(),
                    request.getName(),
                    request.getPicture()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(UserDTO.fromEntity(user));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("already exists")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
            throw e;
        }
    }

    /**
     * Create or update user (idempotent - used during OAuth2 callback)
     * POST /api/users/create-or-update
     */
    @PostMapping("/create-or-update")
    public ResponseEntity<UserDTO> createOrUpdateUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("Creating or updating user: {}", request.getEmail());
        User user = userService.createOrUpdateUser(
                request.getEmail(),
                request.getGoogleId(),
                request.getName(),
                request.getPicture()
        );
        return ResponseEntity.ok(UserDTO.fromEntity(user));
    }

    /**
     * Update user info
     * PUT /api/users/{email}
     */
    @PutMapping("/{email}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable String email,
            @Valid @RequestBody UpdateUserRequest request) {
        log.info("Updating user: {}", email);
        try {
            User user = userService.updateUser(email, request.getName(), request.getPicture());
            return ResponseEntity.ok(UserDTO.fromEntity(user));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    /**
     * Get user by email with roles
     * GET /api/users/{email}
     */
    @GetMapping("/{email}")
    public ResponseEntity<UserDTO> getUserByEmail(@PathVariable String email) {
        log.info("Getting user by email: {}", email);
        return userService.getUserByEmail(email)
                .map(user -> ResponseEntity.ok(UserDTO.fromEntity(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get user by Google ID with roles
     * GET /api/users/google/{googleId}
     */
    @GetMapping("/google/{googleId}")
    public ResponseEntity<UserDTO> getUserByGoogleId(@PathVariable String googleId) {
        log.info("Getting user by Google ID: {}", googleId);
        return userService.getUserByGoogleId(googleId)
                .map(user -> ResponseEntity.ok(UserDTO.fromEntity(user)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Assign role to user
     * POST /api/users/{email}/roles
     */
    @PostMapping("/{email}/roles")
    public ResponseEntity<UserDTO> assignRoleToUser(
            @PathVariable String email,
            @Valid @RequestBody RoleAssignmentRequest request) {
        log.info("Assigning role {} to user {}", request.getRoleName(), email);
        try {
            User user = userService.assignRoleToUser(email, request.getRoleName());
            return ResponseEntity.ok(UserDTO.fromEntity(user));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    /**
     * Remove role from user
     * DELETE /api/users/{email}/roles/{roleName}
     */
    @DeleteMapping("/{email}/roles/{roleName}")
    public ResponseEntity<UserDTO> removeRoleFromUser(
            @PathVariable String email,
            @PathVariable Role.RoleName roleName) {
        log.info("Removing role {} from user {}", roleName, email);
        try {
            User user = userService.removeRoleFromUser(email, roleName);
            return ResponseEntity.ok(UserDTO.fromEntity(user));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    /**
     * Get user roles
     * GET /api/users/{email}/roles
     */
    @GetMapping("/{email}/roles")
    public ResponseEntity<Set<String>> getUserRoles(@PathVariable String email) {
        log.info("Getting roles for user: {}", email);
        try {
            Set<Role> roles = userService.getUserRoles(email);
            Set<String> roleNames = roles.stream()
                    .map(role -> role.getName().name())
                    .collect(Collectors.toSet());
            return ResponseEntity.ok(roleNames);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }

    /**
     * Get Spring Security authorities for user
     * GET /api/users/{email}/authorities
     */
    @GetMapping("/{email}/authorities")
    public ResponseEntity<AuthoritiesResponse> getUserAuthorities(@PathVariable String email) {
        log.info("Getting authorities for user: {}", email);
        Collection<GrantedAuthority> authorities = userService.getUserAuthorities(email);
        Collection<String> authorityStrings = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new AuthoritiesResponse(authorityStrings));
    }

    /**
     * Get Spring Security authorities for user by Google ID
     * GET /api/users/google/{googleId}/authorities
     */
    @GetMapping("/google/{googleId}/authorities")
    public ResponseEntity<AuthoritiesResponse> getUserAuthoritiesByGoogleId(@PathVariable String googleId) {
        log.info("Getting authorities for user by Google ID: {}", googleId);
        Collection<GrantedAuthority> authorities = userService.getUserAuthoritiesByGoogleId(googleId);
        Collection<String> authorityStrings = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new AuthoritiesResponse(authorityStrings));
    }
}

