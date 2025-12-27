package com.pradeep.user.config;

import com.pradeep.user.entity.Role;
import com.pradeep.user.repository.RoleRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleInitializer {

    private final RoleRepository roleRepository;

    @PostConstruct
    public void initializeRoles() {
        log.info("Initializing default roles...");
        
        for (Role.RoleName roleName : Role.RoleName.values()) {
            roleRepository.findByName(roleName).orElseGet(() -> {
                log.info("Creating role: {}", roleName);
                Role role = Role.builder()
                        .name(roleName)
                        .description(getRoleDescription(roleName))
                        .build();
                return roleRepository.save(role);
            });
        }
        
        log.info("Role initialization completed");
    }

    private String getRoleDescription(Role.RoleName roleName) {
        return switch (roleName) {
            case ADMIN -> "Administrator with full system access";
            case HR -> "Human Resources with employee management access";
            case MANAGER -> "Manager with department management access";
            case USER -> "Regular user with basic access";
        };
    }
}

