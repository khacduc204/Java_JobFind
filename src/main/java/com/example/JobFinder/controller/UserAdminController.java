package com.example.JobFinder.controller;

import com.example.JobFinder.model.Role;
import com.example.JobFinder.model.User;
import com.example.JobFinder.repository.RoleRepository;
import com.example.JobFinder.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/users")
@Slf4j
@SuppressWarnings("null")
public class UserAdminController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    
    // Upload directory configuration
    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/avatars/";
    
    public UserAdminController(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        
        // Create upload directory if it doesn't exist
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", UPLOAD_DIR);
            }
        } catch (IOException e) {
            log.error("Could not create upload directory: {}", UPLOAD_DIR, e);
        }
    }

    @GetMapping
    public String listUsers(Model model) {
        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);
        return "admin/users/users";
    }

    @GetMapping("/add")
    public String showAddUserForm(Model model) {
        List<Role> roles = roleRepository.findAll();
        model.addAttribute("roles", roles);
        model.addAttribute("user", new User());
        return "admin/users/add-user";
    }

    @PostMapping("/add")
    public String addUser(
            @RequestParam("name") String name,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("roleId") Integer roleId,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            RedirectAttributes redirectAttributes) {

        // Validate email
        if (userRepository.existsByEmailIgnoreCase(email)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Email đã tồn tại trong hệ thống");
            return "redirect:/admin/users/add";
        }

        // Validate password length
        if (password.length() < 8) {
            redirectAttributes.addFlashAttribute("errorMessage", "Mật khẩu cần có tối thiểu 8 ký tự");
            return "redirect:/admin/users/add";
        }

        // Create user
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        user.setRole(role);

        // Handle avatar upload
        if (avatar != null && !avatar.isEmpty()) {
            try {
                String avatarPath = saveAvatar(avatar);
                user.setAvatarPath(avatarPath);
            } catch (IOException e) {
                log.error("Error uploading avatar", e);
                redirectAttributes.addFlashAttribute("warningMessage", "Tạo người dùng thành công nhưng ảnh đại diện chưa được lưu");
            }
        }

        userRepository.save(user);
        redirectAttributes.addFlashAttribute("successMessage", "Tạo người dùng mới thành công");
        return "redirect:/admin/users";
    }

    @GetMapping("/edit/{id}")
    public String showEditUserForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng");
            return "redirect:/admin/users";
        }
        
        List<Role> roles = roleRepository.findAll();
        model.addAttribute("user", user);
        model.addAttribute("roles", roles);
        return "admin/users/edit-user";
    }

    @PostMapping("/edit/{id}")
    public String updateUser(
            @PathVariable Integer id,
            @RequestParam("name") String name,
            @RequestParam("roleId") Integer roleId,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            RedirectAttributes redirectAttributes) {

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng");
            return "redirect:/admin/users";
        }

        user.setName(name);
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        user.setRole(role);

        // Handle avatar upload
        if (avatar != null && !avatar.isEmpty()) {
            try {
                // Delete old avatar if exists
                if (user.getAvatarPath() != null) {
                    deleteAvatar(user.getAvatarPath());
                }
                String avatarPath = saveAvatar(avatar);
                user.setAvatarPath(avatarPath);
            } catch (IOException e) {
                log.error("Error uploading avatar", e);
                redirectAttributes.addFlashAttribute("warningMessage", "Lỗi upload ảnh: " + e.getMessage());
            }
        }

        userRepository.save(user);
        redirectAttributes.addFlashAttribute("successMessage", "Đã lưu thay đổi");
        return "redirect:/admin/users";
    }

    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy người dùng");
            return "redirect:/admin/users";
        }

        // Delete avatar if exists
        if (user.getAvatarPath() != null) {
            deleteAvatar(user.getAvatarPath());
        }

        userRepository.delete(user);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa người dùng");
        return "redirect:/admin/users";
    }

    private String saveAvatar(MultipartFile file) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }
        
        // Validate file size (max 2MB)
        long maxSize = 2 * 1024 * 1024; // 2MB
        if (file.getSize() > maxSize) {
            throw new IOException("File size exceeds maximum limit of 2MB");
        }
        
        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("File must be an image");
        }
        
        // Create upload directory if not exists
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        } else {
            // Default to jpg if no extension
            extension = ".jpg";
        }
        String filename = UUID.randomUUID().toString() + extension;
        
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        log.info("Saved avatar: {}", filename);
        return "/uploads/avatars/" + filename;
    }

    private void deleteAvatar(String avatarPath) {
        try {
            Path path = Paths.get("src/main/resources/static" + avatarPath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.error("Error deleting avatar: " + avatarPath, e);
        }
    }
}
