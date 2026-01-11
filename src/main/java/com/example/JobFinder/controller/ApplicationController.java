package com.example.JobFinder.controller;

import com.example.JobFinder.model.Application;
import com.example.JobFinder.model.Candidate;
import com.example.JobFinder.model.Job;
import com.example.JobFinder.model.User;
import com.example.JobFinder.repository.ApplicationRepository;
import com.example.JobFinder.repository.CandidateRepository;
import com.example.JobFinder.repository.JobRepository;
import com.example.JobFinder.repository.UserRepository;
import com.example.JobFinder.service.NotificationService;
import com.example.JobFinder.service.EmailService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/applications")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('apply_jobs')")
public class ApplicationController {

    private final ApplicationRepository applicationRepository;
    private final CandidateRepository candidateRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private static final List<String> STATUS_STAGES = List.of("applied", "viewed", "shortlisted", "hired");

    /**
     * Get candidate from authenticated user
     */
    private Candidate getCandidateFromAuth(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        try {
            Object principal = authentication.getPrincipal();
            if (principal instanceof UserDetails userDetails) {
                String email = userDetails.getUsername();
                Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email);

                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    Optional<Candidate> candidateOpt = candidateRepository.findByUserId(user.getId());
                    return candidateOpt.orElse(null);
                }
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    /**
     * Danh sách ứng tuyển của candidate
     */
    @GetMapping
    public String myApplications(
            @RequestParam(required = false, defaultValue = "") String status,
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "1") int page,
            Authentication authentication,
            Model model,
            HttpSession session) {

        Candidate candidate = getCandidateFromAuth(authentication);
        if (candidate == null) {
            return "redirect:/auth/login";
        }

        // Get flash message
        if (session.getAttribute("application_flash") != null) {
            @SuppressWarnings("unchecked")
            Map<String, String> flash = (Map<String, String>) session.getAttribute("application_flash");
            model.addAttribute("flashType", flash.get("type"));
            model.addAttribute("flashMessage", flash.get("message"));
            session.removeAttribute("application_flash");
        }

        // Pagination
        int pageSize = 12;
        List<Application> allApplications = applicationRepository.findByCandidateId(candidate.getId());
        
        // Filter
        List<Application> filteredApplications = allApplications.stream()
            .filter(app -> status.isEmpty() || app.getStatus().equals(status))
            .filter(app -> {
                if (keyword.isEmpty()) return true;
                String lowerKeyword = keyword.toLowerCase();
                String jobTitle = app.getJob() != null ? app.getJob().getTitle() : "";
                String employerName = app.getJob() != null && app.getJob().getEmployer() != null
                    ? app.getJob().getEmployer().getCompanyName() : "";
                return jobTitle.toLowerCase().contains(lowerKeyword) ||
                       employerName.toLowerCase().contains(lowerKeyword);
            })
            .collect(Collectors.toList());

        long total = filteredApplications.size();
        int totalPages = (int) Math.ceil((double) total / pageSize);
        totalPages = Math.max(totalPages, 1);
        if (page < 1) {
            page = 1;
        }
        if (page > totalPages) {
            page = totalPages;
        }

        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, filteredApplications.size());
        
        List<Application> pageApplications = filteredApplications.subList(fromIndex, toIndex);

        // Convert to map
        List<Map<String, Object>> applicationList = pageApplications.stream()
            .map(app -> {
                Map<String, Object> appMap = new HashMap<>();
                appMap.put("id", app.getId());
                appMap.put("status", app.getStatus());
                appMap.put("statusLabel", getStatusLabel(app.getStatus()));
                appMap.put("appliedAt", app.getAppliedAt());
                appMap.put("coverLetter", app.getCoverLetter());
                appMap.put("statusIndex", getStatusIndex(app.getStatus()));

                if (app.getJob() != null) {
                    appMap.put("jobTitle", app.getJob().getTitle());
                    appMap.put("jobId", app.getJob().getId());
                    appMap.put("jobLocation", app.getJob().getLocation());
                    appMap.put("jobSalary", app.getJob().getSalary());

                    if (app.getJob().getEmployer() != null) {
                        appMap.put("employerName", app.getJob().getEmployer().getCompanyName());
                    }
                } else {
                    appMap.put("jobTitle", "N/A");
                }

                return appMap;
            })
            .collect(Collectors.toList());

        // Statistics
        Map<String, Long> statusCounts = new HashMap<>();
        statusCounts.put("applied", allApplications.stream().filter(a -> "applied".equals(a.getStatus())).count());
        statusCounts.put("viewed", allApplications.stream().filter(a -> "viewed".equals(a.getStatus())).count());
        statusCounts.put("shortlisted", allApplications.stream().filter(a -> "shortlisted".equals(a.getStatus())).count());
        statusCounts.put("rejected", allApplications.stream().filter(a -> "rejected".equals(a.getStatus())).count());
        statusCounts.put("hired", allApplications.stream().filter(a -> "hired".equals(a.getStatus())).count());

        if (candidate.getUser() != null) {
            notificationService.markAllAsRead(candidate.getUser().getId());
        }

        model.addAttribute("applications", applicationList);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("total", total);
        model.addAttribute("statusCounts", statusCounts);
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterKeyword", keyword);
        model.addAttribute("pageTitle", "Ứng tuyển của tôi - JobFind");
        model.addAttribute("pageStyles", List.of("applications.css"));
        model.addAttribute("statusStages", STATUS_STAGES);
        model.addAttribute("statusFilters", List.of(
            buildStatusFilter("", "Tất cả", total),
            buildStatusFilter("applied", "Đã ứng tuyển", statusCounts.getOrDefault("applied", 0L)),
            buildStatusFilter("viewed", "Đã xem", statusCounts.getOrDefault("viewed", 0L)),
            buildStatusFilter("shortlisted", "Được shortlist", statusCounts.getOrDefault("shortlisted", 0L)),
            buildStatusFilter("hired", "Đã tuyển", statusCounts.getOrDefault("hired", 0L)),
            buildStatusFilter("rejected", "Từ chối", statusCounts.getOrDefault("rejected", 0L))
        ));

        return "frontend/applications/list";
    }

    /**
     * Chi tiết ứng tuyển
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public String showApplication(
            @PathVariable Integer id,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        Candidate candidate = getCandidateFromAuth(authentication);
        if (candidate == null) {
            return "redirect:/auth/login";
        }

        Optional<Application> applicationOpt = applicationRepository.findByIdWithDetails(id);
        if (applicationOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("flashType", "danger");
            redirectAttributes.addFlashAttribute("flashMessage", "Không tìm thấy hồ sơ ứng tuyển");
            return "redirect:/applications";
        }

        Application application = applicationOpt.get();

        // Check ownership
        if (!application.getCandidate().getId().equals(candidate.getId())) {
            redirectAttributes.addFlashAttribute("flashType", "danger");
            redirectAttributes.addFlashAttribute("flashMessage", "Bạn không có quyền xem hồ sơ này");
            return "redirect:/applications";
        }

        Map<String, Object> appData = new HashMap<>();
        appData.put("id", application.getId());
        appData.put("status", application.getStatus());
        appData.put("statusLabel", getStatusLabel(application.getStatus()));
        appData.put("statusIndex", getStatusIndex(application.getStatus()));
        appData.put("appliedAt", application.getAppliedAt());
        appData.put("coverLetter", application.getCoverLetter());
        appData.put("resumeSnapshot", application.getResumeSnapshot());

        if (application.getJob() != null) {
            Map<String, Object> jobData = new HashMap<>();
            jobData.put("id", application.getJob().getId());
            jobData.put("title", application.getJob().getTitle());
            jobData.put("description", application.getJob().getDescription());
            jobData.put("location", application.getJob().getLocation());
            jobData.put("salary", application.getJob().getSalary());

            if (application.getJob().getEmployer() != null) {
                jobData.put("employerName", application.getJob().getEmployer().getCompanyName());
            }

            appData.put("job", jobData);
        }

        model.addAttribute("application", appData);
        model.addAttribute("pageTitle", "Chi tiết ứng tuyển - JobFind");
        model.addAttribute("pageStyles", List.of("applications.css"));
        model.addAttribute("statusStages", STATUS_STAGES);

        return "frontend/applications/detail";
    }

    /**
     * Ứng tuyển vào công việc
     */
    @PostMapping("/apply")
    @Transactional
    public String applyJob(
            @RequestParam Integer jobId,
            @RequestParam(required = false) String coverLetter,
            @RequestParam(required = false) String cvOption,
            @RequestParam(required = false) MultipartFile cvFile,
            Authentication authentication,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Candidate candidate = getCandidateFromAuth(authentication);
        if (candidate == null) {
            redirectAttributes.addFlashAttribute("flashType", "warning");
            redirectAttributes.addFlashAttribute("flashMessage", "Vui lòng đăng nhập để ứng tuyển");
            return "redirect:/auth/login";
        }

        try {
            // Check job exists and is published
            Optional<Job> jobOpt = jobRepository.findById(jobId);
            if (jobOpt.isEmpty() || !"published".equals(jobOpt.get().getStatus())) {
                Map<String, String> flash = new HashMap<>();
                flash.put("type", "danger");
                flash.put("message", "Tin tuyển dụng không còn khả dụng");
                session.setAttribute("application_flash", flash);
                return "redirect:/jobs/" + jobId;
            }

            Job job = jobOpt.get();

            // Check if already applied
            boolean alreadyApplied = applicationRepository.existsByCandidateIdAndJobId(
                candidate.getId(), jobId
            );

            if (alreadyApplied) {
                Map<String, String> flash = new HashMap<>();
                flash.put("type", "info");
                flash.put("message", "Bạn đã ứng tuyển vị trí này trước đó");
                session.setAttribute("application_flash", flash);
                return "redirect:/jobs/" + jobId;
            }

            // Handle CV
            String resumeSnapshot = null;
            if ("upload".equals(cvOption) && cvFile != null && !cvFile.isEmpty()) {
                // Upload new CV
                resumeSnapshot = handleCvUpload(cvFile, candidate);
            } else if ("existing".equals(cvOption)) {
                // Use existing CV
                resumeSnapshot = "CV đã lưu trên hệ thống";
            }

            // Create application
            Application application = new Application();
            application.setJob(job);
            application.setCandidate(candidate);
            application.setCoverLetter(coverLetter);
            application.setResumeSnapshot(resumeSnapshot);
            application.setStatus("applied");

            applicationRepository.save(application);

            emailService.sendNewApplicationToEmployer(application);

            Map<String, String> flash = new HashMap<>();
            flash.put("type", "success");
            flash.put("message", "Ứng tuyển thành công! Nhà tuyển dụng sẽ xem xét hồ sơ của bạn.");
            session.setAttribute("application_flash", flash);

            return "redirect:/applications";

        } catch (Exception e) {
            Map<String, String> flash = new HashMap<>();
            flash.put("type", "danger");
            flash.put("message", "Có lỗi xảy ra: " + e.getMessage());
            session.setAttribute("application_flash", flash);
            return "redirect:/jobs/" + jobId;
        }
    }

    /**
     * Rút đơn ứng tuyển
     */
    @PostMapping("/{id}/withdraw")
    @Transactional
    public String withdrawApplication(
            @PathVariable Integer id,
            Authentication authentication,
            HttpSession session) {

        Candidate candidate = getCandidateFromAuth(authentication);
        if (candidate == null) {
            return "redirect:/auth/login";
        }

        try {
            Optional<Application> applicationOpt = applicationRepository.findById(id);
            if (applicationOpt.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy hồ sơ ứng tuyển");
            }

            Application application = applicationOpt.get();

            // Check ownership
            if (!application.getCandidate().getId().equals(candidate.getId())) {
                throw new IllegalArgumentException("Bạn không có quyền thực hiện thao tác này");
            }

            // Check if can withdraw
            if ("hired".equals(application.getStatus()) || "rejected".equals(application.getStatus())) {
                throw new IllegalArgumentException("Không thể rút đơn ở trạng thái hiện tại");
            }

            applicationRepository.delete(application);

            Map<String, String> flash = new HashMap<>();
            flash.put("type", "success");
            flash.put("message", "Đã rút đơn ứng tuyển thành công");
            session.setAttribute("application_flash", flash);

        } catch (Exception e) {
            Map<String, String> flash = new HashMap<>();
            flash.put("type", "danger");
            flash.put("message", e.getMessage());
            session.setAttribute("application_flash", flash);
        }

        return "redirect:/applications";
    }

    /**
     * Handle CV file upload
     */
    private String handleCvUpload(MultipartFile file, Candidate candidate) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File rỗng");
        }

        // Validate file type
        String contentType = file.getContentType();
        if (contentType == null || !isValidCvType(contentType)) {
            throw new IOException("Chỉ chấp nhận file PDF, DOC, DOCX");
        }

        // Validate file size (5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IOException("File vượt quá 5MB");
        }

        // Create upload directory
        Path uploadDir = Paths.get("uploads/cv");
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // Generate filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
            ? originalFilename.substring(originalFilename.lastIndexOf("."))
            : ".pdf";
        String filename = "cv_" + candidate.getId() + "_" + System.currentTimeMillis() + extension;

        // Save file
        Path filePath = uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), filePath);

        return "CV: " + originalFilename;
    }

    /**
     * Check valid CV file type
     */
    private boolean isValidCvType(String contentType) {
        return contentType.equals("application/pdf") ||
               contentType.equals("application/msword") ||
               contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    private Map<String, Object> buildStatusFilter(String value, String label, long count) {
        Map<String, Object> filter = new HashMap<>();
        filter.put("value", value);
        filter.put("label", label);
        filter.put("count", count);
        return filter;
    }

    private int getStatusIndex(String status) {
        if (status == null) {
            return -1;
        }
        return switch (status) {
            case "applied" -> 0;
            case "viewed" -> 1;
            case "shortlisted" -> 2;
            case "hired" -> 3;
            default -> -1;
        };
    }

    private String getStatusLabel(String status) {
        if (status == null) {
            return "Chưa cập nhật";
        }
        return switch (status) {
            case "applied" -> "Đã ứng tuyển";
            case "viewed" -> "Nhà tuyển dụng đã xem";
            case "shortlisted" -> "Được shortlist";
            case "rejected" -> "Từ chối";
            case "hired" -> "Đã tuyển dụng";
            default -> status;
        };
    }
}
