package com.example.JobFinder.controller;

import com.example.JobFinder.dto.ApplicationDetailView;
import com.example.JobFinder.model.Application;
import com.example.JobFinder.model.Employer;
import com.example.JobFinder.model.Job;
import com.example.JobFinder.model.User;
import com.example.JobFinder.repository.ApplicationRepository;
import com.example.JobFinder.repository.CategoryRepository;
import com.example.JobFinder.repository.EmployerRepository;
import com.example.JobFinder.repository.JobRepository;
import com.example.JobFinder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/employer")
@RequiredArgsConstructor
public class EmployerController {
    
    private final EmployerRepository employerRepository;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final CategoryRepository categoryRepository;

    private static final String LOGO_UPLOAD_DIR = "uploads/logos/";
    private static final long MAX_LOGO_SIZE = 3 * 1024 * 1024; // 3MB

    @PostConstruct
    public void ensureLogoDirectoryExists() {
        File uploadDir = new File(LOGO_UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        migrateLegacyLogos();
    }
    
    /**
     * Get employer from authenticated user
     */
    private Employer getEmployerFromAuth(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        try {
            String email = authentication.getName();
            Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                return employerRepository.findByUserId(user.getId()).orElse(null);
            }
        } catch (Exception e) {
            return null;
        }
        
        return null;
    }

    @GetMapping("/dashboard")
    public String employerDashboard(Authentication authentication, Model model) {
        Employer employer = getEmployerFromAuth(authentication);
        
        if (employer == null) {
            return "redirect:/auth/login";
        }
        
        // Fetch employer with user to avoid LazyInitializationException
        employer = employerRepository.findByIdWithUser(employer.getId()).orElse(employer);
        
        // Statistics
        Long totalJobs = jobRepository.countByEmployerId(employer.getId());
        Long activeJobs = jobRepository.countByEmployerIdAndStatus(employer.getId(), "active");
        
        // Recent applications (last 5 days)
        LocalDateTime fiveDaysAgo = LocalDateTime.now().minusDays(5);
        Long recentApplications = applicationRepository.countByJobEmployerIdAndAppliedAtAfter(
            employer.getId(), fiveDaysAgo
        );
        
        // Get latest applications with candidate info
        List<Object[]> latestApplicationsData = applicationRepository
            .findLatestApplicationsByEmployerId(employer.getId());
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        List<Map<String, Object>> latestApplications = latestApplicationsData.stream()
            .limit(10)
            .map(row -> {
                Map<String, Object> app = new HashMap<>();
                app.put("id", row[0]); // application.id
                app.put("candidateName", row[1] != null ? row[1] : "Ứng viên"); // candidate name
                app.put("candidateEmail", row[2] != null ? row[2] : ""); // candidate email
                app.put("jobTitle", row[3] != null ? row[3] : ""); // job title
                LocalDateTime appliedAt = (LocalDateTime) row[4];
                app.put("appliedAt", appliedAt != null ? appliedAt.format(formatter) : "");
                return app;
            })
            .collect(Collectors.toList());
        
        model.addAttribute("employer", employer);
        model.addAttribute("totalJobs", totalJobs);
        model.addAttribute("activeJobs", activeJobs);
        model.addAttribute("recentApplications", recentApplications);
        model.addAttribute("latestApplications", latestApplications);
        model.addAttribute("pageTitle", "Bảng điều khiển nhà tuyển dụng");
        model.addAttribute("currentDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        
        return "employer/dashboard";
    }
    
    /**
     * Redirect root to dashboard
     */
    @GetMapping
    public String index() {
        return "redirect:/employer/dashboard";
    }
    
    /**
     * Danh sách tin tuyển dụng
     */
    @GetMapping("/jobs")
    public String listJobs(
            @RequestParam(required = false, defaultValue = "1") int page,
            Authentication authentication,
            Model model,
            HttpSession session) {
        
        Employer employer = getEmployerFromAuth(authentication);
        
        if (employer == null) {
            return "redirect:/auth/login";
        }
        
        // Fetch employer with user to avoid LazyInitializationException
        employer = employerRepository.findByIdWithUser(employer.getId()).orElse(employer);
        
        int perPage = 10;
        Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<Job> jobPage = jobRepository.findByEmployerId(employer.getId(), pageable);
        
        // Get application count for each job
        List<Map<String, Object>> jobs = jobPage.getContent().stream()
            .map(job -> {
                Map<String, Object> jobMap = new HashMap<>();
                jobMap.put("id", job.getId());
                jobMap.put("title", job.getTitle());
                jobMap.put("status", job.getStatus());
                jobMap.put("location", job.getLocation());
                jobMap.put("salary", job.getSalary());
                jobMap.put("quantity", job.getQuantity());
                jobMap.put("deadline", job.getDeadline());
                jobMap.put("updatedAt", job.getUpdatedAt());
                jobMap.put("applicationCount", applicationRepository.countByJobId(job.getId()));
                return jobMap;
            })
            .collect(Collectors.toList());
        
        model.addAttribute("jobs", jobs);
        model.addAttribute("employer", employer);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", jobPage.getTotalPages());
        model.addAttribute("pageTitle", "Tin tuyển dụng của bạn");
        
        // Flash message
        if (session.getAttribute("flashMessage") != null) {
            model.addAttribute("flashMessage", session.getAttribute("flashMessage"));
            model.addAttribute("flashType", session.getAttribute("flashType"));
            session.removeAttribute("flashMessage");
            session.removeAttribute("flashType");
        }
        
        return "employer/jobs";
    }
    
    /**
     * Form tạo tin mới
     */
    @GetMapping("/jobs/create")
    public String createJobForm(Authentication authentication, Model model) {
        Employer employer = getEmployerFromAuth(authentication);
        
        if (employer == null) {
            return "redirect:/auth/login";
        }
        
        // Fetch employer with user to avoid LazyInitializationException
        employer = employerRepository.findByIdWithUser(employer.getId()).orElse(employer);
        
        model.addAttribute("employer", employer);
        model.addAttribute("categories", categoryRepository.findAllOrderByName());
        model.addAttribute("pageTitle", "Đăng tin tuyển dụng mới");
        model.addAttribute("isEdit", false);
        
        return "employer/job-form";
    }
    
    /**
     * Form sửa tin
     */
    @GetMapping("/jobs/edit/{id}")
    public String editJobForm(
            @PathVariable Integer id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        Employer employer = getEmployerFromAuth(authentication);
        
        if (employer == null) {
            return "redirect:/auth/login";
        }
        
        // Fetch employer with user to avoid LazyInitializationException
        employer = employerRepository.findByIdWithUser(employer.getId()).orElse(employer);
        
        Optional<Job> jobOpt = jobRepository.findByIdWithDetails(id);
        
        if (jobOpt.isEmpty() || !jobOpt.get().getEmployer().getId().equals(employer.getId())) {
            redirectAttributes.addFlashAttribute("flashType", "danger");
            redirectAttributes.addFlashAttribute("flashMessage", "Không tìm thấy tin tuyển dụng");
            return "redirect:/employer/jobs";
        }
        
        Job job = jobOpt.get();
        
        model.addAttribute("employer", employer);
        model.addAttribute("job", job);
        model.addAttribute("categories", categoryRepository.findAllOrderByName());
        model.addAttribute("selectedCategories", job.getCategories().stream()
            .map(cat -> cat.getId())
            .collect(Collectors.toList()));
        model.addAttribute("pageTitle", "Chỉnh sửa tin tuyển dụng");
        model.addAttribute("isEdit", true);
        
        return "employer/job-form";
    }
    
    /**
     * Lưu tin mới
     */
    @PostMapping("/jobs/create")
    public String createJob(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam(required = false) String jobRequirements,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String salary,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) Integer quantity,
            @RequestParam(required = false) String deadline,
            @RequestParam(required = false) List<Integer> categoryIds,
            @RequestParam(defaultValue = "draft") String status,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        Employer employer = getEmployerFromAuth(authentication);
        
        if (employer == null) {
            return "redirect:/auth/login";
        }
        
        try {
            Job job = new Job();
            job.setEmployer(employer);
            job.setTitle(title);
            job.setDescription(description);
            job.setJobRequirements(jobRequirements);
            job.setLocation(location);
            job.setSalary(salary);
            job.setEmploymentType(employmentType != null ? employmentType : "Full-time");
            job.setQuantity(quantity);
            job.setStatus(status);
            
            if (deadline != null && !deadline.isEmpty()) {
                job.setDeadline(LocalDate.parse(deadline));
            }
            
            // Set categories
            if (categoryIds != null && !categoryIds.isEmpty()) {
                job.setCategories(categoryRepository.findAllById(categoryIds).stream().collect(Collectors.toSet()));
            }
            
            jobRepository.save(job);
            
            redirectAttributes.addFlashAttribute("flashType", "success");
            redirectAttributes.addFlashAttribute("flashMessage", "Đã tạo tin tuyển dụng thành công");
            
            return "redirect:/employer/jobs";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("flashType", "danger");
            redirectAttributes.addFlashAttribute("flashMessage", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/employer/jobs/create";
        }
    }
    
    /**
     * Cập nhật tin
     */
    @PostMapping("/jobs/edit/{id}")
    public String updateJob(
            @PathVariable Integer id,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam(required = false) String jobRequirements,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String salary,
            @RequestParam(required = false) String employmentType,
            @RequestParam(required = false) Integer quantity,
            @RequestParam(required = false) String deadline,
            @RequestParam(required = false) List<Integer> categoryIds,
            @RequestParam(defaultValue = "draft") String status,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        Employer employer = getEmployerFromAuth(authentication);
        
        if (employer == null) {
            return "redirect:/auth/login";
        }
        
        Optional<Job> jobOpt = jobRepository.findById(id);
        
        if (jobOpt.isEmpty() || !jobOpt.get().getEmployer().getId().equals(employer.getId())) {
            redirectAttributes.addFlashAttribute("flashType", "danger");
            redirectAttributes.addFlashAttribute("flashMessage", "Không tìm thấy tin tuyển dụng");
            return "redirect:/employer/jobs";
        }
        
        try {
            Job job = jobOpt.get();
            job.setTitle(title);
            job.setDescription(description);
            job.setJobRequirements(jobRequirements);
            job.setLocation(location);
            job.setSalary(salary);
            job.setEmploymentType(employmentType != null ? employmentType : "Full-time");
            job.setQuantity(quantity);
            job.setStatus(status);
            
            if (deadline != null && !deadline.isEmpty()) {
                job.setDeadline(LocalDate.parse(deadline));
            } else {
                job.setDeadline(null);
            }
            
            // Update categories
            job.getCategories().clear();
            if (categoryIds != null && !categoryIds.isEmpty()) {
                job.setCategories(categoryRepository.findAllById(categoryIds).stream().collect(Collectors.toSet()));
            }
            
            jobRepository.save(job);
            
            redirectAttributes.addFlashAttribute("flashType", "success");
            redirectAttributes.addFlashAttribute("flashMessage", "Đã cập nhật tin tuyển dụng thành công");
            
            return "redirect:/employer/jobs";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("flashType", "danger");
            redirectAttributes.addFlashAttribute("flashMessage", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/employer/jobs/edit/" + id;
        }
    }
    
    /**
     * Xóa tin
     */
    @PostMapping("/jobs/delete/{id}")
    public String deleteJob(
            @PathVariable Integer id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        Employer employer = getEmployerFromAuth(authentication);
        
        if (employer == null) {
            return "redirect:/auth/login";
        }
        
        Optional<Job> jobOpt = jobRepository.findById(id);
        
        if (jobOpt.isEmpty() || !jobOpt.get().getEmployer().getId().equals(employer.getId())) {
            redirectAttributes.addFlashAttribute("flashType", "danger");
            redirectAttributes.addFlashAttribute("flashMessage", "Không tìm thấy tin tuyển dụng");
            return "redirect:/employer/jobs";
        }
        
        try {
            jobRepository.delete(jobOpt.get());
            
            redirectAttributes.addFlashAttribute("flashType", "success");
            redirectAttributes.addFlashAttribute("flashMessage", "Đã xóa tin tuyển dụng thành công");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("flashType", "danger");
            redirectAttributes.addFlashAttribute("flashMessage", "Có lỗi xảy ra: " + e.getMessage());
        }
        
        return "redirect:/employer/jobs";
    }
    
    /**
     * Trang quản lý hồ sơ ứng viên
     */
    @GetMapping("/applications")
    public String listApplications(
            @RequestParam(required = false) Integer jobId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "1") int page,
            Authentication authentication,
            Model model,
            HttpSession session) {
        
        Employer employer = getEmployerFromAuth(authentication);
        
        if (employer == null) {
            return "redirect:/auth/login";
        }
        
        int perPage = 12;
        Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by(Sort.Direction.DESC, "appliedAt"));
        
        // Get applications based on filters
        Page<Application> applicationPage;
        if (jobId != null) {
            applicationPage = applicationRepository.findByJobIdWithDetails(jobId, pageable);
        } else if (status != null && !status.isEmpty()) {
            applicationPage = applicationRepository.findByJobEmployerIdAndStatus(employer.getId(), status, pageable);
        } else {
            applicationPage = applicationRepository.findByJobEmployerIdWithDetails(employer.getId(), pageable);
        }
        
        // Get all jobs for filter dropdown
        List<Job> employerJobs = jobRepository.findByEmployerId(employer.getId());
        
        // Fetch employer with user to avoid LazyInitializationException
        employer = employerRepository.findByIdWithUser(employer.getId()).orElse(employer);
        
        model.addAttribute("applications", applicationPage.getContent());
        model.addAttribute("employer", employer);
        model.addAttribute("employerJobs", employerJobs);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", applicationPage.getTotalPages());
        model.addAttribute("selectedJobId", jobId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("pageTitle", "Hồ sơ ứng viên");
        
        // Flash message
        if (session.getAttribute("flashMessage") != null) {
            model.addAttribute("flashMessage", session.getAttribute("flashMessage"));
            model.addAttribute("flashType", session.getAttribute("flashType"));
            session.removeAttribute("flashMessage");
            session.removeAttribute("flashType");
        }
        
        return "employer/applications";
    }
    
    /**
     * Xem chi tiết hồ sơ ứng viên
     */
    @GetMapping("/applications/{id}")
    public String viewApplication(
            @PathVariable Integer id,
            Authentication authentication,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        Employer employer = getEmployerFromAuth(authentication);
        
        if (employer == null) {
            return "redirect:/auth/login";
        }
        
        Optional<Application> applicationOpt = applicationRepository.findByIdWithFullDetails(id);
        
        if (applicationOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("flashType", "danger");
            redirectAttributes.addFlashAttribute("flashMessage", "Không tìm thấy hồ sơ ứng viên");
            return "redirect:/employer/applications";
        }
        
        Application application = applicationOpt.get();
        
        // Debug logging
        System.out.println("=== APPLICATION DEBUG ===");
        System.out.println("Application ID: " + application.getId());
        System.out.println("Job: " + (application.getJob() != null ? application.getJob().getTitle() : "NULL"));
        System.out.println("Candidate: " + (application.getCandidate() != null ? application.getCandidate().getId() : "NULL"));
        System.out.println("Candidate User: " + (application.getCandidate() != null && application.getCandidate().getUser() != null ? application.getCandidate().getUser().getName() : "NULL"));
        System.out.println("Status: " + application.getStatus());
        System.out.println("Cover Letter: " + (application.getCoverLetter() != null ? "Yes" : "NULL"));
        if (application.getCandidate() != null) {
            System.out.println("Candidate Headline: " + application.getCandidate().getHeadline());
            System.out.println("Candidate Summary: " + (application.getCandidate().getSummary() != null ? "Yes" : "NULL"));
        }
        if (application.getJob() != null) {
            System.out.println("Job Description: " + (application.getJob().getDescription() != null ? "Yes" : "NULL"));
            System.out.println("Job Requirements: " + (application.getJob().getJobRequirements() != null ? "Yes" : "NULL"));
        }
        System.out.println("========================");
        
        // Verify ownership
        if (application.getJob() == null || !application.getJob().getEmployer().getId().equals(employer.getId())) {
            redirectAttributes.addFlashAttribute("flashType", "danger");
            redirectAttributes.addFlashAttribute("flashMessage", "Bạn không có quyền xem hồ sơ này");
            return "redirect:/employer/applications";
        }
        
        // Mark as viewed if status is 'applied'
        if ("applied".equals(application.getStatus())) {
            application.setStatus("viewed");
            applicationRepository.save(application);
            // Reload to ensure all lazy relationships are fetched
            applicationOpt = applicationRepository.findByIdWithFullDetails(id);
            application = applicationOpt.get();
        }
        
        // Fetch employer with user to avoid LazyInitializationException
        employer = employerRepository.findByIdWithUser(employer.getId()).orElse(employer);
        
        ApplicationDetailView detailView = buildApplicationDetailView(application);
        
        model.addAttribute("application", application);
        model.addAttribute("applicationDetail", detailView);
        model.addAttribute("employer", employer);
        model.addAttribute("pageTitle", "Chi tiết hồ sơ ứng viên");
        
        // Debug: print to verify model content before rendering
        System.out.println("MODEL ATTRIBUTES:");
        System.out.println("- application added to model: " + model.containsAttribute("application"));
        System.out.println("- application ID in model: " + application.getId());
        System.out.println("- candidate present: " + (application.getCandidate() != null));
        if (application.getCandidate() != null) {
            System.out.println("  * candidate ID: " + application.getCandidate().getId());
            System.out.println("  * candidate headline: " + application.getCandidate().getHeadline());
            System.out.println("  * candidate summary: " + application.getCandidate().getSummary());
            System.out.println("  * candidate location: " + application.getCandidate().getLocation());
            System.out.println("  * candidate skills: " + application.getCandidate().getSkills());
            System.out.println("  * candidate experience: " + application.getCandidate().getExperience());
            System.out.println("  * candidate user present: " + (application.getCandidate().getUser() != null));
            if (application.getCandidate().getUser() != null) {
                System.out.println("    - user name: " + application.getCandidate().getUser().getName());
                System.out.println("    - user email: " + application.getCandidate().getUser().getEmail());
                System.out.println("    - user phone: " + application.getCandidate().getUser().getPhone());
            }
        }
        System.out.println("- job present: " + (application.getJob() != null));
        if (application.getJob() != null) {
            System.out.println("  * job ID: " + application.getJob().getId());
            System.out.println("  * job title: " + application.getJob().getTitle());
            System.out.println("  * job description has data: " + (application.getJob().getDescription() != null));
            System.out.println("  * job requirements has data: " + (application.getJob().getJobRequirements() != null));
            System.out.println("  * job employment type: " + application.getJob().getEmploymentType());
            System.out.println("  * job salary: " + application.getJob().getSalary());
        }
        
        // Flash message
        if (session.getAttribute("flashMessage") != null) {
            model.addAttribute("flashMessage", session.getAttribute("flashMessage"));
            model.addAttribute("flashType", session.getAttribute("flashType"));
            session.removeAttribute("flashMessage");
            session.removeAttribute("flashType");
        }
        
        return "employer/application-detail";
    }

    private ApplicationDetailView buildApplicationDetailView(Application application) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        String candidateName = "Ứng viên";
        String candidateEmail = "";
        String candidatePhone = "";
        String candidateHeadline = null;
        String candidateSummary = null;
        String candidateExperience = null;
        String candidateLocation = null;
        String candidateSkills = null;

        if (application.getCandidate() != null) {
            candidateHeadline = application.getCandidate().getHeadline();
            candidateSummary = application.getCandidate().getSummary();
            candidateExperience = application.getCandidate().getExperience();
            candidateLocation = application.getCandidate().getLocation();
            candidateSkills = application.getCandidate().getSkills();

            if (application.getCandidate().getUser() != null) {
                if (application.getCandidate().getUser().getName() != null && !application.getCandidate().getUser().getName().isEmpty()) {
                    candidateName = application.getCandidate().getUser().getName();
                }
                candidateEmail = safeString(application.getCandidate().getUser().getEmail());
                candidatePhone = safeString(application.getCandidate().getUser().getPhone());
            }
        }

        String jobTitle = "Không rõ";
        String jobLocation = null;
        String jobEmploymentType = null;
        String jobSalary = null;
        String jobDescription = null;
        String jobRequirements = null;

        if (application.getJob() != null) {
            if (application.getJob().getTitle() != null && !application.getJob().getTitle().isEmpty()) {
                jobTitle = application.getJob().getTitle();
            }
            jobLocation = application.getJob().getLocation();
            jobEmploymentType = application.getJob().getEmploymentType();
            jobSalary = application.getJob().getSalary();
            jobDescription = application.getJob().getDescription();
            jobRequirements = application.getJob().getJobRequirements();
        }

        return ApplicationDetailView.builder()
            .applicationId(application.getId())
            .status(application.getStatus())
            .appliedAtFormatted(application.getAppliedAt() != null ? application.getAppliedAt().format(dateTimeFormatter) : "")
            .candidateName(candidateName)
            .candidateEmail(candidateEmail)
            .candidatePhone(candidatePhone)
            .candidateHeadline(candidateHeadline)
            .candidateSummary(candidateSummary)
            .candidateExperience(candidateExperience)
            .candidateLocation(candidateLocation)
            .candidateSkills(candidateSkills)
            .coverLetter(application.getCoverLetter())
            .resumeSnapshot(application.getResumeSnapshot())
            .jobTitle(jobTitle)
            .jobLocation(jobLocation)
            .jobEmploymentType(jobEmploymentType)
            .jobSalary(jobSalary)
            .jobDescription(jobDescription)
            .jobRequirements(jobRequirements)
            .build();
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String saveLogo(MultipartFile file) throws IOException {
        if (file.getSize() > MAX_LOGO_SIZE) {
            throw new IllegalArgumentException("Kích thước file không được vượt quá 3MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File phải là ảnh PNG, JPG, GIF hoặc WEBP");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                : "";
        String filename = UUID.randomUUID() + extension;

        Path uploadPath = Paths.get(LOGO_UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return "/uploads/logos/" + filename;
    }

    private void deleteLogo(String logoPath) {
        if (logoPath == null || logoPath.isEmpty()) {
            return;
        }

        try {
            String filename = extractFileName(logoPath);
            Path filePath = Paths.get(LOGO_UPLOAD_DIR).resolve(filename);
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
            // Suppress cleanup issues to avoid interrupting update flow
        }
    }

    private void migrateLegacyLogos() {
        try {
            List<Employer> employers = employerRepository.findAll();
            for (Employer employer : employers) {
                moveLegacyLogoIfNecessary(employer.getLogoPath());
            }
        } catch (Exception e) {
            System.err.println("Không thể đồng bộ logo cũ: " + e.getMessage());
        }
    }

    private void moveLegacyLogoIfNecessary(String logoPath) throws IOException {
        if (logoPath == null || logoPath.trim().isEmpty()) {
            return;
        }

        Path targetPath = resolveLogoStoragePath(logoPath);
        if (Files.exists(targetPath)) {
            return;
        }

        Path legacyPath = Paths.get("src/main/resources/static")
                .resolve(stripLeadingSlash(logoPath));

        if (Files.exists(legacyPath)) {
            Files.createDirectories(targetPath.getParent());
            Files.copy(legacyPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path resolveLogoStoragePath(String storedPath) {
        String filename = extractFileName(storedPath);
        return Paths.get(LOGO_UPLOAD_DIR).resolve(filename).toAbsolutePath().normalize();
    }

    private String extractFileName(String storedPath) {
        if (storedPath == null || storedPath.isEmpty()) {
            return "";
        }
        String normalized = storedPath.replace("\\", "/");
        int idx = normalized.lastIndexOf('/');
        return idx >= 0 ? normalized.substring(idx + 1) : normalized;
    }

    private String stripLeadingSlash(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String normalized = value.startsWith("/") ? value.substring(1) : value;
        return normalized.replace("\\", "/");
    }
    
    /**
     * Cập nhật trạng thái hồ sơ
     */
    @PostMapping("/applications/{id}/update-status")
    public String updateApplicationStatus(
            @PathVariable Integer id,
            @RequestParam String status,
            @RequestParam(required = false) String note,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        Employer employer = getEmployerFromAuth(authentication);
        
        if (employer == null) {
            return "redirect:/auth/login";
        }
        
        Optional<Application> applicationOpt = applicationRepository.findById(id);
        
        if (applicationOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("flashType", "danger");
            redirectAttributes.addFlashAttribute("flashMessage", "Không tìm thấy hồ sơ ứng viên");
            return "redirect:/employer/applications";
        }
        
        Application application = applicationOpt.get();
        
        // Verify ownership
        if (!application.getJob().getEmployer().getId().equals(employer.getId())) {
            redirectAttributes.addFlashAttribute("flashType", "danger");
            redirectAttributes.addFlashAttribute("flashMessage", "Bạn không có quyền cập nhật hồ sơ này");
            return "redirect:/employer/applications";
        }
        
        // Validate rejected status requires note
        if ("rejected".equals(status) && (note == null || note.trim().isEmpty())) {
            redirectAttributes.addFlashAttribute("flashType", "danger");
            redirectAttributes.addFlashAttribute("flashMessage", "Bạn cần ghi chú lý do từ chối");
            return "redirect:/employer/applications/" + id;
        }
        
        application.setStatus(status);
        if (note != null && !note.trim().isEmpty()) {
            application.setCoverLetter(note); // Using coverLetter field for decision note
        }
        
        applicationRepository.save(application);
        
        redirectAttributes.addFlashAttribute("flashType", "success");
        redirectAttributes.addFlashAttribute("flashMessage", "Đã cập nhật trạng thái hồ sơ thành công");
        
        return "redirect:/employer/applications/" + id;
    }
    
    /**
     * Trang hồ sơ công ty
     */
    @GetMapping("/profile")
    public String viewProfile(
            Authentication authentication,
            Model model,
            HttpSession session) {
        
        Employer employer = getEmployerFromAuth(authentication);
        
        if (employer == null) {
            return "redirect:/auth/login";
        }
        
        // Fetch employer with user details to avoid LazyInitializationException
        employer = employerRepository.findByIdWithUser(employer.getId()).orElse(employer);
        
        model.addAttribute("employer", employer);
        model.addAttribute("pageTitle", "Hồ sơ doanh nghiệp");
        
        // Flash message
        if (session.getAttribute("flashMessage") != null) {
            model.addAttribute("flashMessage", session.getAttribute("flashMessage"));
            model.addAttribute("flashType", session.getAttribute("flashType"));
            session.removeAttribute("flashMessage");
            session.removeAttribute("flashType");
        }
        
        return "employer/profile";
    }
    
    /**
     * Cập nhật hồ sơ công ty
     */
    @PostMapping("/profile/update")
    public String updateProfile(
            @RequestParam String companyName,
            @RequestParam(required = false) String website,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String about,
            @RequestParam(required = false) MultipartFile companyLogo,
            @RequestParam(required = false, defaultValue = "false") boolean removeLogo,
            @RequestParam(required = false) String contactName,
            @RequestParam(required = false) String contactPhone,
            Authentication authentication,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        
        Employer employer = getEmployerFromAuth(authentication);
        
        if (employer == null) {
            return "redirect:/auth/login";
        }

        employer = employerRepository.findByIdWithUser(employer.getId()).orElse(employer);

        try {
            if (companyName == null || companyName.trim().isEmpty()) {
                throw new IllegalArgumentException("Tên công ty không được để trống");
            }

            if (removeLogo && employer.getLogoPath() != null) {
                deleteLogo(employer.getLogoPath());
                employer.setLogoPath(null);
            }

            if (companyLogo != null && !companyLogo.isEmpty()) {
                if (employer.getLogoPath() != null) {
                    deleteLogo(employer.getLogoPath());
                }
                employer.setLogoPath(saveLogo(companyLogo));
            }

            employer.setCompanyName(companyName.trim());
            employer.setWebsite(normalize(website));
            employer.setAddress(normalize(address));
            employer.setAbout(normalize(about));

            if (employer.getUser() != null) {
                User user = employer.getUser();
                if (contactName != null) {
                    user.setName(normalize(contactName));
                }
                if (contactPhone != null) {
                    user.setPhone(normalize(contactPhone));
                }
                userRepository.save(user);
            }

            employerRepository.save(employer);
            
            redirectAttributes.addFlashAttribute("flashType", "success");
            redirectAttributes.addFlashAttribute("flashMessage", "Đã cập nhật thông tin công ty thành công");
            session.setAttribute("flashType", "success");
            session.setAttribute("flashMessage", "Đã cập nhật thông tin công ty thành công");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("flashType", "danger");
            redirectAttributes.addFlashAttribute("flashMessage", "Có lỗi xảy ra: " + e.getMessage());
            session.setAttribute("flashType", "danger");
            session.setAttribute("flashMessage", "Có lỗi xảy ra: " + e.getMessage());
        }
        
        return "redirect:/employer/profile";
    }
}
