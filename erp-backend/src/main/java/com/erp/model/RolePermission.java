package com.erp.model;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "role_permissions", uniqueConstraints = {
    @UniqueConstraint(name = "uk_role_permission", columnNames = {"role_name", "permission_code"})
})
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_name", nullable = false, length = 50)
    private String roleName;

    @Column(name = "permission_code", nullable = false, length = 100)
    private String permissionCode;
}
