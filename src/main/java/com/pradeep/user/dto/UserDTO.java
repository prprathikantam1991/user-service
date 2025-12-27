package com.pradeep.user.dto;

import com.pradeep.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private String email;
    private String googleId;
    private String name;
    private String picture;
    private Set<String> roles; // Role names as strings
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserDTO fromEntity(com.pradeep.user.entity.User user) {
        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .googleId(user.getGoogleId())
                .name(user.getName())
                .picture(user.getPicture())
                .roles(user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

