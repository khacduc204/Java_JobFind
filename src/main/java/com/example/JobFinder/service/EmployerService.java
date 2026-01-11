package com.example.JobFinder.service;

import com.example.JobFinder.model.Category;
import com.example.JobFinder.model.Employer;
import com.example.JobFinder.model.Job;
import com.example.JobFinder.repository.EmployerRepository;
import com.example.JobFinder.repository.JobRepository;
import com.example.JobFinder.repository.JobViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployerService {

    private final EmployerRepository employerRepository;
    private final JobRepository jobRepository;
    private final JobViewRepository jobViewRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final Pattern BENEFIT_SPLIT_PATTERN = Pattern.compile("[\\r\\n;,]+");

    public Map<String, Object> getDirectoryPaginated(String searchTerm, String location, String sortOrder, int page, int perPage) {
        Map<String, Object> result = new HashMap<>();
        
        Sort sort = Objects.requireNonNull(getSort(sortOrder != null ? sortOrder : "featured"));
        Pageable pageable = PageRequest.of(page - 1, perPage, sort);
        Page<Employer> employerPage;
        
        if ((searchTerm != null && !searchTerm.isEmpty()) || (location != null && !location.isEmpty())) {
            employerPage = employerRepository.findByFilters(searchTerm, location, pageable);
        } else {
            employerPage = employerRepository.findAll(pageable);
        }
        
        List<Map<String, Object>> rows = employerPage.getContent().stream()
                .map(this::employerToMap)
                .collect(Collectors.toList());
        
        result.put("rows", rows);
        result.put("total", employerPage.getTotalElements());
        result.put("totalPages", employerPage.getTotalPages());
        result.put("currentPage", page);
        
        return result;
    }

    public Map<String, Object> getDirectoryStats() {
        Map<String, Object> stats = new HashMap<>();
        long totalEmployers = employerRepository.count();
        stats.put("totalEmployers", totalEmployers);
        stats.put("activeEmployers", jobRepository.count());
        stats.put("totalJobs", jobRepository.count());
        stats.put("newThisMonth", 0);
        return stats;
    }

    public Map<String, Object> getEmployerProfile(Long id) {
        if (id == null) {
            return null;
        }
        Optional<Employer> employerOpt = employerRepository.findByIdWithUser(id.intValue());
        if (employerOpt.isEmpty()) {
            return null;
        }
        Employer employer = employerOpt.get();
        Map<String, Object> map = employerToMap(employer, true);
        if (employer.getUser() != null) {
            map.put("contact_email", employer.getUser().getEmail());
            map.put("contact_phone", employer.getUser().getPhone());
            map.put("contact_name", employer.getUser().getName());
        }
        return map;
    }

    public List<Map<String, Object>> getEmployerJobs(Long employerId) {
        if (employerId == null) {
            return Collections.emptyList();
        }
        List<Job> jobs = jobRepository.findPublishedByEmployerId(employerId.intValue());
        return jobs.stream()
                .map(this::jobToMap)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getEmployerStats(Long employerId) {
        if (employerId == null) {
            return Map.of(
                    "totalJobs", 0,
                    "recentJobs", 0,
                    "totalViews", 0,
                    "uniqueLocations", 0,
                    "locationTags", List.of(),
                    "latestActivity", "Đang cập nhật"
            );
        }

        List<Job> jobs = jobRepository.findPublishedByEmployerId(employerId.intValue());
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        Set<String> locations = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        long recentJobs = 0;
        LocalDateTime latestActivity = null;

        for (Job job : jobs) {
            if (job.getLocation() != null && !job.getLocation().isBlank()) {
                locations.add(job.getLocation().trim());
            }
            if (job.getCreatedAt() != null && !job.getCreatedAt().isBefore(thirtyDaysAgo)) {
                recentJobs++;
            }
            LocalDateTime activity = job.getUpdatedAt() != null ? job.getUpdatedAt() : job.getCreatedAt();
            if (activity != null && (latestActivity == null || activity.isAfter(latestActivity))) {
                latestActivity = activity;
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalJobs", jobs.size());
        stats.put("recentJobs", recentJobs);
        stats.put("totalViews", jobViewRepository.countByEmployerId(employerId.intValue()));
        stats.put("uniqueLocations", locations.size());
        stats.put("locationTags", new ArrayList<>(locations));
        stats.put("latestActivity", latestActivity != null
                ? latestActivity.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "Đang cập nhật");
        return stats;
    }

    public List<String> getEmployerBenefits(Long employerId) {
        if (employerId == null) {
            return getDefaultBenefits();
        }
        Optional<String> benefitsRaw = fetchTextColumn(employerId.intValue(), "benefits");
        if (benefitsRaw.isPresent()) {
            List<String> parsed = BENEFIT_SPLIT_PATTERN.splitAsStream(benefitsRaw.get())
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .distinct()
                    .collect(Collectors.toList());
            if (!parsed.isEmpty()) {
                return parsed;
            }
        }
        return getDefaultBenefits();
    }

    public List<Map<String, String>> getCultureHighlights(Map<String, Object> employer, Map<String, Object> stats) {
        String companyName = Objects.toString(employer.getOrDefault("company_name", "Doanh nghiệp"));
        int totalJobs = ((Number) stats.getOrDefault("totalJobs", 0)).intValue();
        int recentJobs = ((Number) stats.getOrDefault("recentJobs", 0)).intValue();
        int locations = ((Number) stats.getOrDefault("uniqueLocations", 0)).intValue();
        long totalViews = ((Number) stats.getOrDefault("totalViews", 0L)).longValue();

        List<Map<String, String>> highlights = new ArrayList<>();
        if (totalJobs > 0) {
            highlights.add(createHighlight(
                    "fa-people-group",
                    "Đội ngũ đang mở rộng",
                    companyName + " đang mở " + totalJobs + " vị trí và tích cực tìm kiếm nhân sự nổi bật."
            ));
        } else {
            highlights.add(createHighlight(
                    "fa-lightbulb",
                    "Sẵn sàng cho chiến dịch mới",
                    companyName + " đang chuẩn bị cho những chiến dịch tuyển dụng tiếp theo."
            ));
        }

        if (recentJobs > 0) {
            highlights.add(createHighlight(
                    "fa-rocket",
                    "Tuyển dụng sôi động",
                    recentJobs + " tin đăng mới trong 30 ngày thể hiện tốc độ tăng trưởng thực tế."
            ));
        }

        if (locations > 0) {
            highlights.add(createHighlight(
                    "fa-map-location-dot",
                    "Môi trường đa địa điểm",
                    "Cơ hội làm việc tại " + locations + " địa điểm linh hoạt."
            ));
        }

        if (totalViews > 0) {
            highlights.add(createHighlight(
                    "fa-eye",
                    "Được ứng viên quan tâm",
                    String.format("%,d", totalViews) + " lượt xem việc làm phản ánh sức hút thương hiệu tuyển dụng."
            ));
        }

        return highlights;
    }

    public List<Map<String, Object>> getHiringTimeline(Long employerId) {
        if (employerId == null) {
            return Collections.emptyList();
        }
        List<Job> jobs = jobRepository.findPublishedByEmployerId(employerId.intValue());
        return jobs.stream()
                .filter(job -> job.getCreatedAt() != null)
                .sorted(Comparator.comparing(Job::getCreatedAt).reversed())
                .limit(6)
                .map(job -> {
                    Map<String, Object> timelineRow = new HashMap<>();
                    timelineRow.put("title", job.getTitle());
                    timelineRow.put("date", job.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                    timelineRow.put("view_count", jobViewRepository.countByJobId(job.getId()));
                    return timelineRow;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getRelatedJobs(Long employerId, int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }

        Integer employerIdInt = employerId != null ? employerId.intValue() : null;

        List<Job> employerJobs = employerIdInt != null
            ? jobRepository.findPublishedByEmployerId(employerIdInt)
                : Collections.emptyList();

        Set<Integer> categoryIds = employerJobs.stream()
                .flatMap(job -> job.getCategories().stream())
                .map(Category::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Job> related;
        if (categoryIds.isEmpty() || employerIdInt == null) {
            related = jobRepository.findTopJobsByViewCount(PageRequest.of(0, limit));
        } else {
            related = jobRepository.findRelatedJobs(
                    employerIdInt,
                    new ArrayList<>(categoryIds),
                    PageRequest.of(0, limit)
            );
        }

        return related.stream()
                .map(this::jobCardSummary)
                .collect(Collectors.toList());
    }

    public String pickPrimaryMapAddress(Map<String, Object> employer, Map<String, Object> stats) {
        String companyAddress = Objects.toString(employer.getOrDefault("address", ""), "").trim();
        if (!companyAddress.isEmpty()) {
            return companyAddress;
        }
        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) stats.getOrDefault("locationTags", Collections.emptyList());
        return tags.isEmpty() ? null : tags.get(0);
    }

    public String buildMapEmbedUrl(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }
        String query = URLEncoder.encode(address, StandardCharsets.UTF_8);
        return "https://www.google.com/maps?q=" + query + "&output=embed";
    }

    private Map<String, Object> employerToMap(Employer employer, boolean includeJobCount) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", employer.getId());
        map.put("company_name", employer.getCompanyName());
        map.put("about", employer.getAbout());
        map.put("address", employer.getAddress());
        map.put("website", employer.getWebsite());
        map.put("logo_path", employer.getLogoPath());
        map.put("job_count", includeJobCount ? jobRepository.countPublishedByEmployerId(employer.getId()) : 0);

        String companyName = employer.getCompanyName();
        if (companyName != null && !companyName.isEmpty()) {
            String[] words = companyName.trim().split("\\s+");
            String initial;
            if (words.length >= 2) {
                initial = words[0].substring(0, 1).toUpperCase() + words[1].substring(0, 1).toUpperCase();
            } else {
                initial = companyName.substring(0, Math.min(2, companyName.length())).toUpperCase();
            }
            map.put("company_initial", initial);
        } else {
            map.put("company_initial", "JF");
        }

        return map;
    }

    private Map<String, Object> employerToMap(Employer employer) {
        return employerToMap(employer, false);
    }

    private Sort getSort(String sortOrder) {
        if ("alphabet".equals(sortOrder)) {
            return Sort.by(Sort.Direction.ASC, "companyName");
        }
        // Default to featured (by id DESC) since we don't have createdAt field
        return Sort.by(Sort.Direction.DESC, "id");
    }

    private List<String> getDefaultBenefits() {
        return Arrays.asList(
                "Bảo hiểm đầy đủ theo quy định",
                "Thưởng hiệu suất định kỳ",
                "Chương trình đào tạo chuyên môn",
                "Du lịch và team building hàng năm",
                "Môi trường làm việc chuyên nghiệp",
                "Cơ hội thăng tiến rõ ràng"
        );
    }

    private Map<String, String> createHighlight(String icon, String title, String description) {
        Map<String, String> highlight = new HashMap<>();
        highlight.put("icon", icon);
        highlight.put("title", title);
        highlight.put("description", description);
        return highlight;
    }

    private Map<String, Object> jobToMap(Job job) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", job.getId());
        map.put("title", job.getTitle());
        map.put("location", job.getLocation());
        map.put("salary", job.getSalary());
        map.put("employmentType", job.getEmploymentType());
        map.put("viewCount", jobViewRepository.countByJobId(job.getId()));
        map.put("status", job.getStatus());
        map.put("createdAt", job.getCreatedAt());
        return map;
    }

    private Map<String, Object> jobCardSummary(Job job) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", job.getId());
        map.put("title", job.getTitle());
        map.put("companyName", job.getEmployer() != null ? job.getEmployer().getCompanyName() : "Nhà tuyển dụng");
        map.put("employmentType", job.getEmploymentType());
        map.put("location", job.getLocation());
        map.put("salary", job.getSalary());
        return map;
    }

    private Optional<String> fetchTextColumn(Integer employerId, String columnName) {
        if (!"benefits".equals(columnName)) {
            return Optional.empty();
        }
        try {
            String sql = "SELECT " + columnName + " FROM employers WHERE id = ?";
            String value = jdbcTemplate.queryForObject(sql, String.class, employerId);
            if (value == null || value.trim().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(value);
        } catch (BadSqlGrammarException ex) {
            return Optional.empty();
        } catch (DataAccessException ex) {
            return Optional.empty();
        }
    }
}
