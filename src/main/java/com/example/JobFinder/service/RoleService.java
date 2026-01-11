package com.example.JobFinder.service;

import com.example.JobFinder.dto.RoleSummary;
import com.example.JobFinder.model.Permission;
import com.example.JobFinder.model.Role;
import com.example.JobFinder.repository.PermissionRepository;
import com.example.JobFinder.repository.RoleRepository;
import com.example.JobFinder.repository.UserRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoleService {

    private static final Set<Integer> SYSTEM_ROLE_IDS = Set.of(1, 2, 3);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository, UserRepository userRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
    }

    public List<RoleSummary> getRoleSummaries() {
        List<Role> roles = roleRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        List<Integer> roleIds = roles.stream()
            .map(Role::getId)
            .filter(Objects::nonNull)
            .toList();

        Map<Integer, Long> userCounts = roleIds.isEmpty()
            ? Map.of()
            : buildUserCountMap(roleIds);

        return roles.stream()
            .map(role -> new RoleSummary(role, userCounts.getOrDefault(role.getId(), 0L)))
            .toList();
    }

    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    public long countRoles() {
        return roleRepository.count();
    }

    public long countPermissions() {
        return permissionRepository.count();
    }

    public long countUsers() {
        return userRepository.count();
    }

    public Set<Integer> getSystemRoleIds() {
        return SYSTEM_ROLE_IDS;
    }

    public Role getRoleById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Thiếu mã vai trò");
        }
        return roleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy vai trò"));
    }

    @Transactional
    public Role createRole(String name, String description, List<Integer> permissionIds) {
        String sanitizedName = sanitizeName(name);
        ensureNameUnique(sanitizedName, null);

        Role role = new Role();
        role.setName(sanitizedName);
        role.setDescription(sanitizeDescription(description));
        role.setPermissions(resolvePermissions(permissionIds));
        return roleRepository.save(role);
    }

    @Transactional
    public Role updateRole(Integer id, String name, String description, List<Integer> permissionIds) {
        Role role = getRoleById(id);
        String sanitizedName = sanitizeName(name);
        ensureNameUnique(sanitizedName, role.getId());

        role.setName(sanitizedName);
        role.setDescription(sanitizeDescription(description));
        role.setPermissions(resolvePermissions(permissionIds));
        return roleRepository.save(role);
    }

    @Transactional
    public void deleteRole(Integer id) {
        Role role = getRoleById(id);
        long usage = userRepository.countByRoleId(role.getId());
        if (usage > 0) {
            throw new IllegalStateException("Không thể xóa vai trò đang được sử dụng");
        }
        roleRepository.delete(role);
    }

    @Transactional
    public Permission createPermission(String name, String description) {
        String normalizedName = sanitizePermissionName(name);
        if (permissionRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new IllegalArgumentException("Tên quyền đã tồn tại");
        }

        Permission permission = new Permission();
        permission.setName(normalizedName);
        permission.setDescription(sanitizeDescription(description));
        return permissionRepository.save(permission);
    }

    private Map<Integer, Long> buildUserCountMap(List<Integer> roleIds) {
        List<UserRepository.RoleUserCount> counts = userRepository.countUsersByRoleIds(roleIds);
        return new HashMap<>(counts.stream()
            .collect(Collectors.toMap(UserRepository.RoleUserCount::getRoleId, UserRepository.RoleUserCount::getUserCount)));
    }

    private String sanitizeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên vai trò không được để trống");
        }
        return name.trim();
    }

    private String sanitizeDescription(String description) {
        if (description == null || description.trim().isEmpty()) {
            return null;
        }
        return description.trim();
    }

    private void ensureNameUnique(String name, Integer ignoreId) {
        roleRepository.findByNameIgnoreCase(name)
            .ifPresent(existing -> {
                if (ignoreId == null || !existing.getId().equals(ignoreId)) {
                    throw new IllegalArgumentException("Tên vai trò đã tồn tại");
                }
            });
    }

    private Set<Permission> resolvePermissions(List<Integer> permissionIds) {
        List<Integer> normalizedIds = normalizePermissionIds(permissionIds);
        if (normalizedIds.isEmpty()) {
            return new HashSet<>();
        }

        List<Permission> permissions = permissionRepository.findAllById(normalizedIds);
        if (permissions.size() != normalizedIds.size()) {
            throw new IllegalArgumentException("Không tìm thấy đủ quyền đã chọn");
        }
        return new HashSet<>(permissions);
    }

    private List<Integer> normalizePermissionIds(List<Integer> permissionIds) {
        if (permissionIds == null) {
            return List.of();
        }
        return permissionIds.stream()
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    private String sanitizePermissionName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên quyền không được để trống");
        }
        String normalized = name.trim().toLowerCase().replaceAll("\\s+", "_");
        if (!normalized.matches("^[a-z0-9_]{3,50}$")) {
            throw new IllegalArgumentException("Tên quyền chỉ gồm chữ cái thường, số và dấu gạch dưới (3-50 ký tự)");
        }
        return normalized;
    }
}
