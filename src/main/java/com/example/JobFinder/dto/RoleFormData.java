package com.example.JobFinder.dto;

import java.util.List;

public record RoleFormData(Integer id, String name, String description, List<Integer> permissionIds) {

    public RoleFormData {
        permissionIds = permissionIds == null ? List.of() : List.copyOf(permissionIds);
    }
}
