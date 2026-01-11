package com.example.JobFinder.controller;

import com.example.JobFinder.dto.RoleFormData;
import com.example.JobFinder.model.Permission;
import com.example.JobFinder.model.Role;
import com.example.JobFinder.service.RoleService;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/roles")
@PreAuthorize("hasAuthority('manage_users')")
public class RoleAdminController {

    private final RoleService roleService;

    public RoleAdminController(RoleService roleService) {
        this.roleService = roleService;
    }

    @ModelAttribute("allPermissions")
    public List<Permission> loadPermissions() {
        return roleService.getAllPermissions();
    }

    @GetMapping
    public String listRoles(Model model) {
        model.addAttribute("roles", roleService.getRoleSummaries());
        model.addAttribute("totalRoles", roleService.countRoles());
        model.addAttribute("totalPermissions", roleService.countPermissions());
        model.addAttribute("totalUsers", roleService.countUsers());
        model.addAttribute("systemRoleIds", roleService.getSystemRoleIds());
        return "admin/roles/index";
    }

    @GetMapping("/manage")
    public String manageRole(@RequestParam(value = "id", required = false) Integer id,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        Map<String, Object> attributes = model.asMap();
        RoleFormData formData = (RoleFormData) attributes.get("formData");

        if (formData == null) {
            if (id != null) {
                try {
                    Role role = roleService.getRoleById(id);
                    formData = new RoleFormData(
                        role.getId(),
                        role.getName(),
                        role.getDescription(),
                        role.getPermissions().stream()
                            .map(Permission::getId)
                            .toList()
                    );
                } catch (IllegalArgumentException ex) {
                    redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
                    return "redirect:/admin/roles";
                }
            } else {
                formData = new RoleFormData(null, "", "", List.of());
            }
        }

        model.addAttribute("formData", formData);
        model.addAttribute("isEdit", formData.id() != null);
        return "admin/roles/manage";
    }

    @PostMapping("/save")
    public String saveRole(@RequestParam(value = "id", required = false) Integer id,
                           @RequestParam("name") String name,
                           @RequestParam(value = "description", required = false) String description,
                           @RequestParam(value = "permissionIds", required = false) List<Integer> permissionIds,
                           RedirectAttributes redirectAttributes) {
        try {
            if (id == null) {
                roleService.createRole(name, description, permissionIds);
                redirectAttributes.addFlashAttribute("successMessage", "Đã tạo vai trò mới");
            } else {
                roleService.updateRole(id, name, description, permissionIds);
                redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật vai trò");
            }
            return "redirect:/admin/roles";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("formData", new RoleFormData(id, name, description, permissionIds));
            String target = id == null ? "/admin/roles/manage" : "/admin/roles/manage?id=" + id;
            return "redirect:" + target;
        }
    }

    @PostMapping("/delete/{id}")
    public String deleteRole(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            roleService.deleteRole(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa vai trò");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/roles";
    }

    @PostMapping("/permissions")
    public String createPermission(@RequestParam("name") String name,
                                   @RequestParam(value = "description", required = false) String description,
                                   RedirectAttributes redirectAttributes) {
        try {
            roleService.createPermission(name, description);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm quyền mới");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/roles";
    }
}
