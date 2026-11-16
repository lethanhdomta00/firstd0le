package com.erp.security;

import com.erp.model.Role;
import com.erp.model.User;
import com.erp.repository.UserRepository;
import com.erp.service.RolePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RolePermissionService rolePermissionService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        List<GrantedAuthority> authorities = new ArrayList<>();
        Role role = user.getRole() != null ? user.getRole() : Role.USER;

        // 1. Prefix ROLE_ cho Spring Security hasRole(...)
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));

        // 2. Các quyền hạn động nạp từ Ma trận RBAC trong Database cho hasAuthority(...)
        Set<String> dynamicPerms = rolePermissionService.getPermissionsForRole(role.name());
        if (dynamicPerms != null) {
            for (String perm : dynamicPerms) {
                authorities.add(new SimpleGrantedAuthority(perm));
            }
        }

        boolean isLocked = "LOCKED".equalsIgnoreCase(user.getStatus());

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountLocked(isLocked)
                .disabled(isLocked)
                .build();
    }
}
