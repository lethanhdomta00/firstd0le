package com.erp.dto;

import com.erp.model.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleMatrixDto {
    private String role;
    private String title;
    private String description;
    private Set<String> permissions;

    public static RoleMatrixDto fromRole(Role role) {
        return RoleMatrixDto.builder()
                .role(role.name())
                .title(role.getTitle())
                .description(role.getDescription())
                .permissions(role.getPermissions())
                .build();
    }

    public static List<RoleMatrixDto> getAllMatrix() {
        return Arrays.stream(Role.values())
                .map(RoleMatrixDto::fromRole)
                .toList();
    }
}
