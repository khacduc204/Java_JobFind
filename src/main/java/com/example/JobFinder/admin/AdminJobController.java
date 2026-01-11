package com.example.JobFinder.admin;

import com.example.JobFinder.model.Category;
import com.example.JobFinder.model.Employer;
import com.example.JobFinder.model.Job;
import com.example.JobFinder.repository.CategoryRepository;
import com.example.JobFinder.repository.EmployerRepository;
import com.example.JobFinder.repository.JobRepository;
import com.example.JobFinder.repository.JobViewRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/jobs")
@PreAuthorize("hasAuthority('manage_jobs')")
public class AdminJobController {

    private final JobRepository jobRepository;
    private final EmployerRepository employerRepository;
    private final CategoryRepository categoryRepository;
    private final JobViewRepository jobViewRepository;

    public AdminJobController(JobRepository jobRepository, 
                            EmployerRepository employerRepository,
                            CategoryRepository categoryRepository,
                            JobViewRepository jobViewRepository) {
        this.jobRepository = jobRepository;
        this.employerRepository = employerRepository;
        this.categoryRepository = categoryRepository;
        this.jobViewRepository = jobViewRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String listJobs(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "") String status,
            @RequestParam(required = false, defaultValue = "1") int page,
            Model model,
            HttpSession session) {

        // Get flash message if any
        if (session.getAttribute("admin_job_flash") != null) {
            @SuppressWarnings("unchecked")
            Map<String, String> flash = (Map<String, String>) session.getAttribute("admin_job_flash");
            model.addAttribute("flashType", flash.get("type"));
            model.addAttribute("flashMessage", flash.get("message"));
            session.removeAttribute("admin_job_flash");
        }

        // Pagination
        int pageSize = 20;
        Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "updatedAt", "createdAt"));

        // Get jobs with filters
        List<Job> jobs = new ArrayList<>();
        long totalJobs;
        
        if (keyword.isEmpty() && status.isEmpty()) {
            Page<Job> jobPage = jobRepository.findAll(pageable);
            jobs = jobPage.getContent();
            totalJobs = jobPage.getTotalElements();
        } else {
            // Manual filtering
            List<Job> allJobs = jobRepository.findAll();
            jobs = allJobs.stream()
                .filter(j -> {
                    boolean matchKeyword = keyword.isEmpty() || 
                        (j.getTitle() != null && j.getTitle().toLowerCase().contains(keyword.toLowerCase())) ||
                        (j.getEmployer() != null && j.getEmployer().getCompanyName() != null && 
                         j.getEmployer().getCompanyName().toLowerCase().contains(keyword.toLowerCase()));
                    
                    boolean matchStatus = status.isEmpty() || 
                        (j.getStatus() != null && j.getStatus().equals(status));
                    
                    return matchKeyword && matchStatus;
                })
                .sorted(Comparator.comparing(Job::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                    .thenComparing(Job::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .skip((long) (page - 1) * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
            
            totalJobs = allJobs.stream()
                .filter(j -> {
                    boolean matchKeyword = keyword.isEmpty() || 
                        (j.getTitle() != null && j.getTitle().toLowerCase().contains(keyword.toLowerCase())) ||
                        (j.getEmployer() != null && j.getEmployer().getCompanyName() != null && 
                         j.getEmployer().getCompanyName().toLowerCase().contains(keyword.toLowerCase()));
                    
                    boolean matchStatus = status.isEmpty() || 
                        (j.getStatus() != null && j.getStatus().equals(status));
                    
                    return matchKeyword && matchStatus;
                })
                .count();
        }

        // Calculate statistics
        Map<String, Long> statusCounts = new HashMap<>();
        statusCounts.put("draft", jobRepository.countByStatus("draft"));
        statusCounts.put("published", jobRepository.countByStatus("published"));
        statusCounts.put("closed", jobRepository.countByStatus("closed"));
        
        long totalJobsCount = statusCounts.values().stream().mapToLong(Long::longValue).sum();

        // Prepare job data with view counts
        List<Map<String, Object>> jobList = jobs.stream().map(job -> {
            Map<String, Object> jobData = new HashMap<>();
            jobData.put("id", job.getId());
            jobData.put("title", job.getTitle());
            jobData.put("status", job.getStatus());
            jobData.put("quantity", job.getQuantity());
            jobData.put("deadline", job.getDeadline());
            jobData.put("createdAt", job.getCreatedAt());
            jobData.put("updatedAt", job.getUpdatedAt());
            
            // Employer info
            if (job.getEmployer() != null) {
                jobData.put("employerName", job.getEmployer().getCompanyName());
                jobData.put("employerEmail", job.getEmployer().getUser() != null ? 
                    job.getEmployer().getUser().getEmail() : "");
            } else {
                jobData.put("employerName", "N/A");
                jobData.put("employerEmail", "");
            }
            
            // View count
            long viewCount = jobViewRepository.countByJobId(job.getId());
            jobData.put("viewCount", viewCount);
            
            return jobData;
        }).collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) totalJobs / pageSize);

        model.addAttribute("jobs", jobList);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalJobs", totalJobsCount);
        model.addAttribute("statusCounts", statusCounts);
        model.addAttribute("filterKeyword", keyword);
        model.addAttribute("filterStatus", status);

        return "admin/jobs/index";
    }

    @GetMapping("/{id}/edit")
    @Transactional(readOnly = true)
    public String showEditForm(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        Optional<Job> jobOpt = jobRepository.findByIdWithDetails(id);
        
        if (jobOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("flashType", "danger");
            redirectAttributes.addFlashAttribute("flashMessage", "Không tìm thấy tin tuyển dụng");
            return "redirect:/admin/jobs";
        }

        Job job = jobOpt.get();
        
        // Get all employers
        List<Employer> employers = employerRepository.findAll();
        
        // Get all categories
        List<Category> allCategories = categoryRepository.findAll();
        
        // Get selected category IDs
        Set<Integer> selectedCategoryIds = job.getCategories().stream()
            .map(Category::getId)
            .collect(Collectors.toSet());

        model.addAttribute("job", job);
        model.addAttribute("employers", employers);
        model.addAttribute("allCategories", allCategories);
        model.addAttribute("selectedCategoryIds", selectedCategoryIds);
        
        return "admin/jobs/edit";
    }

    @PostMapping("/{id}/edit")
    @Transactional
    public String updateJob(
            @PathVariable Integer id,
            @RequestParam Integer employerId,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam(required = false) String jobRequirements,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String salary,
            @RequestParam(required = false) String employmentType,
            @RequestParam String status,
            @RequestParam(required = false) Integer quantity,
            @RequestParam(required = false) String deadline,
            @RequestParam(required = false) List<Integer> categories,
            RedirectAttributes redirectAttributes,
            HttpSession session) {

        try {
            Optional<Job> jobOpt = jobRepository.findById(id);
            
            if (jobOpt.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy tin tuyển dụng");
            }

            Job job = jobOpt.get();

            // Validate employer
            Employer employer = employerRepository.findById(employerId)
                    .orElseThrow(() -> new IllegalArgumentException("Nhà tuyển dụng không tồn tại"));

            // Validate fields
            if (title == null || title.trim().isEmpty()) {
                throw new IllegalArgumentException("Tiêu đề không được để trống");
            }

            if (description == null || description.trim().isEmpty()) {
                throw new IllegalArgumentException("Mô tả công việc không được để trống");
            }

            // Validate status
            List<String> allowedStatuses = Arrays.asList("draft", "published", "closed");
            if (!allowedStatuses.contains(status)) {
                throw new IllegalArgumentException("Trạng thái không hợp lệ");
            }

            // Update job fields
            job.setEmployer(employer);
            job.setTitle(title.trim());
            job.setDescription(description.trim());
            job.setJobRequirements(jobRequirements != null ? jobRequirements.trim() : null);
            job.setLocation(location != null && !location.trim().isEmpty() ? location.trim() : null);
            job.setSalary(salary != null && !salary.trim().isEmpty() ? salary.trim() : null);
            job.setEmploymentType(employmentType != null && !employmentType.trim().isEmpty() ? employmentType.trim() : null);
            job.setStatus(status);
            job.setQuantity(quantity);
            
            // Parse deadline
            if (deadline != null && !deadline.trim().isEmpty()) {
                try {
                    job.setDeadline(LocalDate.parse(deadline.trim(), DateTimeFormatter.ISO_LOCAL_DATE));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Định dạng ngày không hợp lệ");
                }
            } else {
                job.setDeadline(null);
            }

            // Update categories
            if (categories != null && !categories.isEmpty()) {
                Set<Category> categorySet = new HashSet<>();
                for (Integer catId : categories) {
                    categoryRepository.findById(catId).ifPresent(categorySet::add);
                }
                job.setCategories(categorySet);
            } else {
                job.getCategories().clear();
            }

            jobRepository.save(job);

            Map<String, String> flash = new HashMap<>();
            flash.put("type", "success");
            flash.put("message", "Cập nhật tin tuyển dụng thành công");
            session.setAttribute("admin_job_flash", flash);

            return "redirect:/admin/jobs";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/jobs/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/update-status")
    @Transactional
    public String updateStatus(
            @PathVariable Integer id,
            @RequestParam String status,
            RedirectAttributes redirectAttributes,
            HttpSession session) {

        try {
            Optional<Job> jobOpt = jobRepository.findById(id);
            
            if (jobOpt.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy tin tuyển dụng");
            }

            Job job = jobOpt.get();

            // Validate status
            List<String> allowedStatuses = Arrays.asList("draft", "published", "closed");
            if (!allowedStatuses.contains(status)) {
                throw new IllegalArgumentException("Trạng thái không hợp lệ");
            }

            job.setStatus(status);
            jobRepository.save(job);

            Map<String, String> flash = new HashMap<>();
            flash.put("type", "success");
            flash.put("message", "Cập nhật trạng thái thành công");
            session.setAttribute("admin_job_flash", flash);

        } catch (Exception e) {
            Map<String, String> flash = new HashMap<>();
            flash.put("type", "danger");
            flash.put("message", e.getMessage());
            session.setAttribute("admin_job_flash", flash);
        }

        return "redirect:/admin/jobs";
    }

    @PostMapping("/{id}/delete")
    @Transactional
    public String deleteJob(
            @PathVariable Integer id,
            HttpSession session) {

        try {
            Optional<Job> jobOpt = jobRepository.findById(id);
            
            if (jobOpt.isEmpty()) {
                throw new IllegalArgumentException("Không tìm thấy tin tuyển dụng");
            }

            jobRepository.deleteById(id);

            Map<String, String> flash = new HashMap<>();
            flash.put("type", "success");
            flash.put("message", "Xóa tin tuyển dụng thành công");
            session.setAttribute("admin_job_flash", flash);

        } catch (Exception e) {
            Map<String, String> flash = new HashMap<>();
            flash.put("type", "danger");
            flash.put("message", "Không thể xóa tin tuyển dụng: " + e.getMessage());
            session.setAttribute("admin_job_flash", flash);
        }

        return "redirect:/admin/jobs";
    }
}
