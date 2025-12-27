package com.pradeep.user.dto;

import com.pradeep.user.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleAssignmentRequest {
    @NotNull(message = "Role name is required")
    private Role.RoleName roleName;
}

