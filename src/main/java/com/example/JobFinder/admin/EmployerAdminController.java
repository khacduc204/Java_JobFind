package com.example.JobFinder.admin;

import com.example.JobFinder.dto.EmployerStatsDTO;
import com.example.JobFinder.model.Employer;
import com.example.JobFinder.model.User;
import com.example.JobFinder.repository.EmployerRepository;
import com.example.JobFinder.repository.RoleRepository;
import com.example.JobFinder.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/admin/employers")
public class EmployerAdminController {

    private final EmployerRepository employerRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/logos/";
    private static final long MAX_FILE_SIZE = 3 * 1024 * 1024; // 3MB

    public EmployerAdminController(EmployerRepository employerRepository, UserRepository userRepository, RoleRepository roleRepository) {
        this.employerRepository = employerRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;

        // Create upload directory if it doesn't exist
        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String listEmployers(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "") String location,
            @RequestParam(required = false, defaultValue = "") String status,
            Model model,
            HttpSession session) {

        // Get flash message if any
        if (session.getAttribute("admin_employer_flash") != null) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, String> flash = (java.util.Map<String, String>) session.getAttribute("admin_employer_flash");
            model.addAttribute("flashType", flash.get("type"));
            model.addAttribute("flashMessage", flash.get("message"));
            session.removeAttribute("admin_employer_flash");
        }

        // Get employers with filters
        List<Employer> employers;
        if (keyword.isEmpty() && location.isEmpty()) {
            employers = employerRepository.findAllWithUser();
        } else {
            employers = employerRepository.findByFilters(keyword, location);
        }

        // Calculate statistics
        long totalEmployers = employerRepository.count();
        long activeEmployers = employers.stream()
                .filter(e -> e.getUser() != null && e.getUser().getId() != null)
                .count();
        long inactiveEmployers = totalEmployers - activeEmployers;

        EmployerStatsDTO stats = new EmployerStatsDTO();
        stats.setTotalEmployers(totalEmployers);
        stats.setActiveEmployers(activeEmployers);
        stats.setInactiveEmployers(inactiveEmployers);
        stats.setTotalPublishedJobs(0); // TODO: Implement when Job entity is ready

        model.addAttribute("employers", employers);
        model.addAttribute("stats", stats);
        model.addAttribute("filterKeyword", keyword);
        model.addAttribute("filterLocation", location);
        model.addAttribute("filterStatus", status);

        return "admin/employers/employers";
    }

    @GetMapping("/add")
    @Transactional(readOnly = true)
    public String showAddForm(Model model) {
        // Get users with employer role (role_id = 2)
        List<User> employerUsers = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && u.getRole().getId() == 2)
                .filter(u -> !employerRepository.existsByUserId(u.getId()))
                .toList();

        model.addAttribute("employerUsers", employerUsers);
        model.addAttribute("employer", new Employer());

        return "admin/employers/add-employer";
    }

    @PostMapping("/add")
    @Transactional
    public String addEmployer(
            @RequestParam Integer userId,
            @RequestParam String companyName,
            @RequestParam(required = false) String website,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String about,
            @RequestParam(required = false) MultipartFile companyLogo,
            RedirectAttributes redirectAttributes,
            HttpSession session) {

        try {
            // Validate user
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("Tài khoản không tồn tại"));

            if (user.getRole() == null || user.getRole().getId() != 2) {
                throw new IllegalArgumentException("Tài khoản không phải là nhà tuyển dụng");
            }

            if (employerRepository.existsByUserId(userId)) {
                throw new IllegalArgumentException("Tài khoản này đã có hồ sơ doanh nghiệp");
            }

            // Validate company name
            if (companyName == null || companyName.trim().isEmpty()) {
                throw new IllegalArgumentException("Tên công ty không được để trống");
            }

            // Handle logo upload
            String logoPath = null;
            if (companyLogo != null && !companyLogo.isEmpty()) {
                logoPath = saveLogo(companyLogo);
            }

            // Create employer
            Employer employer = new Employer();
            employer.setUser(user);
            employer.setCompanyName(companyName.trim());
            employer.setWebsite(website != null && !website.trim().isEmpty() ? website.trim() : null);
            employer.setAddress(address != null && !address.trim().isEmpty() ? address.trim() : null);
            employer.setAbout(about != null && !about.trim().isEmpty() ? about.trim() : null);
            employer.setLogoPath(logoPath);

            employerRepository.save(employer);

            session.setAttribute("admin_employer_flash", java.util.Map.of(
                    "type", "success",
                    "message", "Thêm mới hồ sơ nhà tuyển dụng thành công."
            ));

            return "redirect:/admin/employers";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            redirectAttributes.addFlashAttribute("companyName", companyName);
            redirectAttributes.addFlashAttribute("website", website);
            redirectAttributes.addFlashAttribute("address", address);
            redirectAttributes.addFlashAttribute("about", about);
            return "redirect:/admin/employers/add";
        }
    }

    @GetMapping("/edit/{id}")
    @Transactional(readOnly = true)
    public String showEditForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        Employer employer = employerRepository.findByIdWithUser(id)
                .orElse(null);

        if (employer == null) {
            redirectAttributes.addFlashAttribute("error", "Nhà tuyển dụng không tồn tại");
            return "redirect:/admin/employers";
        }

        model.addAttribute("employer", employer);
        return "admin/employers/edit-employer";
    }

    @PostMapping("/edit/{id}")
    @Transactional
    public String updateEmployer(
            @PathVariable Integer id,
            @RequestParam String companyName,
            @RequestParam(required = false) String website,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String about,
            @RequestParam(required = false) MultipartFile companyLogo,
            @RequestParam(required = false) boolean removeLogo,
            RedirectAttributes redirectAttributes,
            HttpSession session) {

        try {
            Employer employer = employerRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Nhà tuyển dụng không tồn tại"));

            // Validate company name
            if (companyName == null || companyName.trim().isEmpty()) {
                throw new IllegalArgumentException("Tên công ty không được để trống");
            }

            // Handle logo removal
            if (removeLogo && employer.getLogoPath() != null) {
                deleteLogo(employer.getLogoPath());
                employer.setLogoPath(null);
            }

            // Handle new logo upload
            if (companyLogo != null && !companyLogo.isEmpty()) {
                // Delete old logo if exists
                if (employer.getLogoPath() != null) {
                    deleteLogo(employer.getLogoPath());
                }
                String logoPath = saveLogo(companyLogo);
                employer.setLogoPath(logoPath);
            }

            // Update employer
            employer.setCompanyName(companyName.trim());
            employer.setWebsite(website != null && !website.trim().isEmpty() ? website.trim() : null);
            employer.setAddress(address != null && !address.trim().isEmpty() ? address.trim() : null);
            employer.setAbout(about != null && !about.trim().isEmpty() ? about.trim() : null);

            employerRepository.save(employer);

            session.setAttribute("admin_employer_flash", java.util.Map.of(
                    "type", "success",
                    "message", "Cập nhật hồ sơ nhà tuyển dụng thành công."
            ));

            return "redirect:/admin/employers";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/employers/edit/" + id;
        }
    }

    @GetMapping("/delete/{id}")
    @Transactional
    public String deleteEmployer(@PathVariable Integer id, HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            Employer employer = employerRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Nhà tuyển dụng không tồn tại"));

            // Delete logo if exists
            if (employer.getLogoPath() != null) {
                deleteLogo(employer.getLogoPath());
            }

            employerRepository.delete(employer);

            session.setAttribute("admin_employer_flash", java.util.Map.of(
                    "type", "success",
                    "message", "Xóa nhà tuyển dụng thành công."
            ));

        } catch (Exception e) {
            session.setAttribute("admin_employer_flash", java.util.Map.of(
                    "type", "danger",
                    "message", "Lỗi: " + e.getMessage()
            ));
        }

        return "redirect:/admin/employers";
    }

    private String saveLogo(MultipartFile file) throws IOException {
        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Kích thước file không được vượt quá 3MB");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File phải là ảnh (PNG, JPG, GIF, WEBP)");
        }

        // Generate unique filename
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String filename = UUID.randomUUID().toString() + extension;

        // Save file
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/logos/" + filename;
    }

    private void deleteLogo(String logoPath) {
        if (logoPath != null && !logoPath.isEmpty()) {
            try {
                String filename = logoPath.substring(logoPath.lastIndexOf("/") + 1);
                Path filePath = Paths.get(UPLOAD_DIR + filename);
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Log error but don't throw exception
                System.err.println("Could not delete logo: " + e.getMessage());
            }
        }
    }
}
