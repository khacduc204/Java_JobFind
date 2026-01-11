package com.example.JobFinder.admin;

import com.example.JobFinder.model.Application;
import com.example.JobFinder.repository.ApplicationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/applications")
@PreAuthorize("hasAuthority('view_applications')")
public class AdminApplicationController {

    private final ApplicationRepository applicationRepository;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private static final List<String> STATUS_ORDER = List.of(
        "applied", "viewed", "shortlisted", "rejected", "hired", "withdrawn"
    );

    private static final Map<String, String> STATUS_LABELS = Map.of(
        "applied", "Đã ứng tuyển",
        "viewed", "Nhà tuyển dụng đã xem",
        "shortlisted", "Đã chọn phỏng vấn",
        "rejected", "Bị từ chối",
        "hired", "Đã trúng tuyển",
        "withdrawn", "Đã rút đơn"
    );

    private static final Map<String, String> STATUS_BADGES = Map.of(
        "applied", "secondary",
        "viewed", "info",
        "shortlisted", "warning",
        "rejected", "danger",
        "hired", "success",
        "withdrawn", "dark"
    );

    private static final Map<String, String> STATUS_DESCRIPTIONS = Map.of(
        "applied", "Ứng viên đã gửi hồ sơ và chờ xét duyệt",
        "viewed", "Nhà tuyển dụng đã xem hồ sơ",
        "shortlisted", "Ứng viên nằm trong danh sách phỏng vấn",
        "rejected", "Hồ sơ đã bị từ chối và cần ghi chú phản hồi",
        "hired", "Ứng viên đã được nhận việc",
        "withdrawn", "Ứng viên đã chủ động rút hồ sơ"
    );

    public AdminApplicationController(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    private static double calculateStatusProgress(String status) {
        int idx = STATUS_ORDER.indexOf(status);
        if (idx < 0 || STATUS_ORDER.isEmpty()) {
            return 0;
        }
        return ((idx + 1) * 100.0) / STATUS_ORDER.size();
    }

    private static String truncate(String value, int limit) {
        if (value == null || value.isBlank() || value.length() <= limit) {
            return value;
        }
        if (limit <= 3) {
            return value.substring(0, Math.max(0, limit));
        }
        return value.substring(0, limit - 3) + "...";
    }

    private static List<String> extractSkillTags(String skills) {
        if (skills == null || skills.isBlank()) {
            return Collections.emptyList();
        }

        String trimmed = skills.trim();
        try {
            if (trimmed.startsWith("[")) {
                List<String> parsed = OBJECT_MAPPER.readValue(trimmed, STRING_LIST_TYPE);
                return parsed.stream()
                    .map(token -> token == null ? "" : token.trim())
                    .filter(token -> !token.isEmpty())
                    .limit(12)
                    .collect(Collectors.toList());
            }
        } catch (Exception ignored) {
            // Fallback to manual parsing below
        }

        return Arrays.stream(trimmed.split("[,;\\n]"))
            .map(token -> token
                .replace("\"", "")
                .replace("[", "")
                .replace("]", "")
                .trim())
            .filter(token -> !token.isEmpty())
            .limit(12)
            .collect(Collectors.toList());
    }

    private static String toPublicUrl(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return null;
        }

        String normalized = storedPath.replace("\\", "/").trim();
        if (normalized.startsWith("http://") || normalized.startsWith("https://")) {
            return normalized;
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized;
    }

    @GetMapping
    @Transactional(readOnly = true)
        public String listApplications(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "") String status,
            @RequestParam(required = false) Integer jobId,
            @RequestParam(required = false) Integer employerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false, defaultValue = "1") int page,
            Model model,
            HttpSession session) {

        // Get flash message if any
        if (session.getAttribute("admin_application_flash") != null) {
            @SuppressWarnings("unchecked")
            Map<String, String> flash = (Map<String, String>) session.getAttribute("admin_application_flash");
            model.addAttribute("flashType", flash.get("type"));
            model.addAttribute("flashMessage", flash.get("message"));
            session.removeAttribute("admin_application_flash");
        }

        // Pagination
        int pageSize = 20;
        int safePage = Math.max(page, 1);
        Pageable pageable = PageRequest.of(safePage - 1, pageSize, Sort.by(Sort.Direction.DESC, "appliedAt"));

        LocalDateTime filterFrom = dateFrom != null ? dateFrom.atStartOfDay() : null;
        LocalDateTime filterTo = dateTo != null ? dateTo.atTime(LocalTime.MAX) : null;

        // Get applications with filters
        Page<Application> applicationPage = applicationRepository.findAllWithFilters(
            keyword, status, jobId, employerId, filterFrom, filterTo, pageable
        );

        List<Application> applications = applicationPage.getContent();
        int totalPages = applicationPage.getTotalPages();
        long totalRecords = applicationPage.getTotalElements();

        // Calculate statistics
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (String statusKey : STATUS_ORDER) {
            statusCounts.put(statusKey, applicationRepository.countByStatus(statusKey));
        }
        
        long totalCount = statusCounts.values().stream().mapToLong(Long::longValue).sum();
        
        // Last 7 days
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long last7Days = applicationRepository.countSince(sevenDaysAgo);
        
        // Last 30 days
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long last30Days = applicationRepository.countSince(thirtyDaysAgo);
        
        // Awaiting review (applied + viewed)
        long awaitingReview = statusCounts.getOrDefault("applied", 0L) + statusCounts.getOrDefault("viewed", 0L);
        long shortlistedCount = statusCounts.getOrDefault("shortlisted", 0L);
        long hiredCount = statusCounts.getOrDefault("hired", 0L);

        List<Map<String, Object>> statusOverview = STATUS_ORDER.stream().map(statusKey -> {
            long count = statusCounts.getOrDefault(statusKey, 0L);
            double percentage = totalCount > 0 ? (count * 100.0) / totalCount : 0;
            Map<String, Object> entry = new HashMap<>();
            entry.put("key", statusKey);
            entry.put("label", STATUS_LABELS.getOrDefault(statusKey, statusKey));
            entry.put("badge", STATUS_BADGES.getOrDefault(statusKey, "secondary"));
            entry.put("description", STATUS_DESCRIPTIONS.getOrDefault(statusKey, ""));
            entry.put("count", count);
            entry.put("percentage", Math.round(percentage * 10) / 10.0);
            return entry;
        }).collect(Collectors.toList());

        // Prepare application data
        List<Map<String, Object>> applicationList = applications.stream().map(app -> {
            Map<String, Object> appData = new HashMap<>();
            appData.put("id", app.getId());
            appData.put("status", app.getStatus());
            appData.put("statusLabel", STATUS_LABELS.getOrDefault(app.getStatus(), app.getStatus()));
            appData.put("statusBadge", STATUS_BADGES.getOrDefault(app.getStatus(), "secondary"));
            appData.put("statusDescription", STATUS_DESCRIPTIONS.getOrDefault(app.getStatus(), ""));
            appData.put("statusProgress", Math.round(calculateStatusProgress(app.getStatus())));
            appData.put("appliedAt", app.getAppliedAt());
            appData.put("coverLetter", app.getCoverLetter());
            appData.put("notePreview", truncate(app.getCoverLetter(), 80));
            
            // Candidate info
            if (app.getCandidate() != null) {
                appData.put("candidateName", app.getCandidate().getUser() != null ? 
                    app.getCandidate().getUser().getName() : "N/A");
                appData.put("candidateEmail", app.getCandidate().getUser() != null ? 
                    app.getCandidate().getUser().getEmail() : "");
                appData.put("candidateId", app.getCandidate().getId());
                appData.put("candidateLocation", app.getCandidate().getLocation());
                appData.put("candidatePhone", app.getCandidate().getUser() != null ?
                    app.getCandidate().getUser().getPhone() : "");
            } else {
                appData.put("candidateName", "N/A");
                appData.put("candidateEmail", "");
                appData.put("candidateId", null);
                appData.put("candidateLocation", null);
                appData.put("candidatePhone", null);
            }
            
            // Job info
            if (app.getJob() != null) {
                appData.put("jobTitle", app.getJob().getTitle());
                appData.put("jobId", app.getJob().getId());
                appData.put("jobLocation", app.getJob().getLocation());
                
                // Employer info
                if (app.getJob().getEmployer() != null) {
                    appData.put("employerName", app.getJob().getEmployer().getCompanyName());
                    appData.put("employerId", app.getJob().getEmployer().getId());
                } else {
                    appData.put("employerName", "N/A");
                    appData.put("employerId", null);
                }
            } else {
                appData.put("jobTitle", "N/A");
                appData.put("jobId", null);
                appData.put("employerName", "N/A");
                appData.put("employerId", null);
                appData.put("jobLocation", null);
            }
            
            return appData;
        }).collect(Collectors.toList());

        model.addAttribute("applications", applicationList);
        model.addAttribute("currentPage", safePage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalRecords", totalRecords);
        model.addAttribute("totalApplications", totalCount);
        model.addAttribute("last7Days", last7Days);
        model.addAttribute("last30Days", last30Days);
        model.addAttribute("awaitingReview", awaitingReview);
        model.addAttribute("shortlistedCount", shortlistedCount);
        model.addAttribute("hiredCount", hiredCount);
        model.addAttribute("displayedCount", applicationList.size());
        model.addAttribute("statusCounts", statusCounts);
        model.addAttribute("statusBreakdown", statusOverview);
        model.addAttribute("statusOrder", STATUS_ORDER);
        model.addAttribute("statusLabels", STATUS_LABELS);
        model.addAttribute("statusBadgeMap", STATUS_BADGES);
        model.addAttribute("statusDescriptions", STATUS_DESCRIPTIONS);
        model.addAttribute("filterKeyword", keyword);
        model.addAttribute("filterStatus", status);
        model.addAttribute("filterJobId", jobId);
        model.addAttribute("filterEmployerId", employerId);
        model.addAttribute("filterDateFrom", dateFrom != null ? dateFrom.toString() : "");
        model.addAttribute("filterDateTo", dateTo != null ? dateTo.toString() : "");
        model.addAttribute("statusOptions", STATUS_ORDER);

        return "admin/applications/index";
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public String showApplication(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Application> applicationOpt = applicationRepository.findByIdWithDetails(id);
        
        if (applicationOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("flashType", "danger");
            redirectAttributes.addFlashAttribute("flashMessage", "Không tìm thấy hồ sơ ứng tuyển");
            return "redirect:/admin/applications";
        }

        Application application = applicationOpt.get();
        String currentStatus = Optional.ofNullable(application.getStatus()).orElse("applied");
        int currentStatusIndex = STATUS_ORDER.indexOf(currentStatus);
        if (currentStatusIndex < 0) {
            currentStatusIndex = 0;
        }
        int progressPercent = (int) Math.round(calculateStatusProgress(currentStatus));

        List<Map<String, Object>> statusSteps = new ArrayList<>();
        for (int i = 0; i < STATUS_ORDER.size(); i++) {
            String statusKey = STATUS_ORDER.get(i);
            Map<String, Object> step = new HashMap<>();
            step.put("key", statusKey);
            step.put("label", STATUS_LABELS.getOrDefault(statusKey, statusKey));
            step.put("description", STATUS_DESCRIPTIONS.getOrDefault(statusKey, ""));
            step.put("badge", STATUS_BADGES.getOrDefault(statusKey, "secondary"));
            step.put("order", i + 1);
            step.put("completed", i <= currentStatusIndex);
            step.put("current", statusKey.equals(currentStatus));
            statusSteps.add(step);
        }
        
        // Prepare application data
        Map<String, Object> appData = new HashMap<>();
        appData.put("id", application.getId());
        appData.put("status", currentStatus);
        appData.put("statusLabel", STATUS_LABELS.getOrDefault(currentStatus, currentStatus));
        appData.put("statusBadge", STATUS_BADGES.getOrDefault(currentStatus, "secondary"));
        appData.put("statusDescription", STATUS_DESCRIPTIONS.getOrDefault(currentStatus, ""));
        appData.put("statusSteps", statusSteps);
        appData.put("statusProgress", progressPercent);
        appData.put("appliedAt", application.getAppliedAt());
        appData.put("coverLetter", application.getCoverLetter());
        appData.put("resumeSnapshot", application.getResumeSnapshot());
        appData.put("decisionNote", application.getDecisionNote());
        
        // Candidate info
        if (application.getCandidate() != null) {
            Map<String, Object> candidateData = new HashMap<>();
            candidateData.put("id", application.getCandidate().getId());
            candidateData.put("headline", application.getCandidate().getHeadline());
            candidateData.put("summary", application.getCandidate().getSummary());
            candidateData.put("location", application.getCandidate().getLocation());
            candidateData.put("skills", application.getCandidate().getSkills());
            candidateData.put("experience", application.getCandidate().getExperience());
            candidateData.put("cvPath", application.getCandidate().getCvPath());
            candidateData.put("cvUrl", toPublicUrl(application.getCandidate().getCvPath()));
            
            if (application.getCandidate().getUser() != null) {
                candidateData.put("name", application.getCandidate().getUser().getName());
                candidateData.put("email", application.getCandidate().getUser().getEmail());
                candidateData.put("phone", application.getCandidate().getUser().getPhone());
            }
            
            appData.put("candidate", candidateData);
            appData.put("candidateId", candidateData.get("id"));
            appData.put("candidateName", candidateData.getOrDefault("name", "Ứng viên"));
            appData.put("candidateEmail", candidateData.get("email"));
            appData.put("candidatePhone", candidateData.get("phone"));
            appData.put("candidateLocation", candidateData.get("location"));
            appData.put("candidateSummary", candidateData.get("summary"));
            appData.put("candidateHeadline", candidateData.get("headline"));
            appData.put("candidateCvUrl", candidateData.get("cvUrl"));
        } else {
            appData.put("candidateName", "Ứng viên");
        }
        List<String> skillTags = application.getCandidate() != null ?
            extractSkillTags(application.getCandidate().getSkills()) : Collections.emptyList();
        appData.put("skillTags", skillTags);
        
        // Job info
        if (application.getJob() != null) {
            Map<String, Object> jobData = new HashMap<>();
            jobData.put("id", application.getJob().getId());
            jobData.put("title", application.getJob().getTitle());
            jobData.put("description", application.getJob().getDescription());
            jobData.put("location", application.getJob().getLocation());
            jobData.put("salary", application.getJob().getSalary());
            jobData.put("employmentType", application.getJob().getEmploymentType());
            jobData.put("status", application.getJob().getStatus());
            
            if (application.getJob().getEmployer() != null) {
                jobData.put("employerName", application.getJob().getEmployer().getCompanyName());
                jobData.put("employerId", application.getJob().getEmployer().getId());
                jobData.put("employerEmail", application.getJob().getEmployer().getUser() != null ? 
                    application.getJob().getEmployer().getUser().getEmail() : "");
            }
            
            appData.put("job", jobData);
            appData.put("jobId", jobData.get("id"));
            appData.put("jobTitle", jobData.get("title"));
            appData.put("jobDescription", jobData.get("description"));
            appData.put("jobLocation", jobData.get("location"));
            appData.put("jobSalary", jobData.get("salary"));
            appData.put("jobEmploymentType", jobData.get("employmentType"));
            appData.put("employerName", jobData.get("employerName"));
            appData.put("employerEmail", jobData.get("employerEmail"));
        } else {
            appData.put("employerName", "Không xác định");
        }

        model.addAttribute("application", appData);
        model.addAttribute("statusOptions", STATUS_ORDER);
        model.addAttribute("statusLabels", STATUS_LABELS);
        model.addAttribute("statusBadgeMap", STATUS_BADGES);
        
        return "admin/applications/show";
    }

    @PostMapping("/{id}/update-status")
    @Transactional
    public String updateStatus(
            @PathVariable Integer id,
            @RequestParam String status,
            HttpSession session) {

        try {
            Optional<Application> applicationOpt = applicationRepository.findById(id);
            
            if (applicationOpt.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy hồ sơ ứng tuyển");
            }

            Application application = applicationOpt.get();

            // Validate status
            if (!STATUS_ORDER.contains(status)) {
                throw new IllegalArgumentException("Trạng thái không hợp lệ");
            }

            application.setStatus(status);
            applicationRepository.save(application);

            Map<String, String> flash = new HashMap<>();
            flash.put("type", "success");
            flash.put("message", "Cập nhật trạng thái thành công");
            session.setAttribute("admin_application_flash", flash);

        } catch (Exception e) {
            Map<String, String> flash = new HashMap<>();
            flash.put("type", "danger");
            flash.put("message", e.getMessage());
            session.setAttribute("admin_application_flash", flash);
        }

        return "redirect:/admin/applications/" + id;
    }

    @PostMapping("/{id}/delete")
    @Transactional
    public String deleteApplication(
            @PathVariable Integer id,
            HttpSession session) {

        try {
            Optional<Application> applicationOpt = applicationRepository.findById(id);
            
            if (applicationOpt.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy hồ sơ ứng tuyển");
            }

            applicationRepository.deleteById(id);

            Map<String, String> flash = new HashMap<>();
            flash.put("type", "success");
            flash.put("message", "Xóa hồ sơ ứng tuyển thành công");
            session.setAttribute("admin_application_flash", flash);

        } catch (Exception e) {
            Map<String, String> flash = new HashMap<>();
            flash.put("type", "danger");
            flash.put("message", "Không thể xóa hồ sơ: " + e.getMessage());
            session.setAttribute("admin_application_flash", flash);
        }

        return "redirect:/admin/applications";
    }
}
