package com.example.JobFinder.service;

import com.example.JobFinder.repository.ApplicationRepository;
import com.example.JobFinder.repository.CategoryRepository;
import com.example.JobFinder.repository.CategoryRepository.CategoryStatsProjection;
import com.example.JobFinder.repository.EmployerRepository;
import com.example.JobFinder.repository.EmployerRepository.EmployerStatsProjection;
import com.example.JobFinder.repository.JobRepository;
import com.example.JobFinder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final UserRepository userRepository;
    private final EmployerRepository employerRepository;
    private final CategoryRepository categoryRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final JobService jobService;

    private static final List<String> FALLBACK_KEYWORDS = List.of(
        "Product Manager",
        "Frontend Developer",
        "Backend Engineer",
        "Data Analyst",
        "UI/UX Designer",
        "Digital Marketing"
    );

    private static final List<Map<String, Object>> FALLBACK_ARTICLES = List.of(
        Map.of(
            "title", "5 bí kíp nâng cấp CV khiến nhà tuyển dụng ấn tượng ngay lập tức",
            "category", "CV & Hồ sơ",
            "readTime", "5 phút đọc"
        ),
        Map.of(
            "title", "Checklist phỏng vấn: Chuẩn bị gì để không bị hỏi khó?",
            "category", "Phỏng vấn",
            "readTime", "7 phút đọc"
        ),
        Map.of(
            "title", "Kỹ năng phân tích dữ liệu cho marketer thời 4.0",
            "category", "Kỹ năng",
            "readTime", "6 phút đọc"
        )
    );

    private static final Map<String, String> CATEGORY_ICON_LOOKUP;

    static {
    Map<String, String> icons = new HashMap<>();
    icons.put("công nghệ", "fa-code");
    icons.put("it", "fa-code");
    icons.put("marketing", "fa-bullhorn");
    icons.put("kinh doanh", "fa-chart-line");
    icons.put("bán hàng", "fa-store");
    icons.put("nhân sự", "fa-users");
    icons.put("thiết kế", "fa-pen-ruler");
    icons.put("kế toán", "fa-calculator");
    icons.put("tài chính", "fa-sack-dollar");
    icons.put("xây dựng", "fa-building");
    CATEGORY_ICON_LOOKUP = Collections.unmodifiableMap(icons);
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Count users by role
        long candidates = userRepository.countByRoleId(3); // candidate role
    long employersCount = employerRepository.count();  // actual employers from employer table
    long jobs = jobRepository.countByStatus("published");
    long applications = applicationRepository.count();
        
        stats.put("candidates", candidates);
        stats.put("employers", employersCount);
        stats.put("jobs", jobs);
    stats.put("applications", applications);
        
        return stats;
    }

    public List<Map<String, Object>> formatHeroMetrics(Map<String, Object> stats) {
        List<Map<String, Object>> metrics = new ArrayList<>();
        
        // Format candidates
        Map<String, Object> candidatesMetric = new HashMap<>();
        candidatesMetric.put("label", "Ứng viên tin dùng JobFind");
        candidatesMetric.put("value", stats.get("candidates"));
        candidatesMetric.put("formatted", formatNumber((Long) stats.get("candidates")));
        metrics.add(candidatesMetric);
        
        // Format employers
        Map<String, Object> employersMetric = new HashMap<>();
        employersMetric.put("label", "Nhà tuyển dụng đang tuyển");
        employersMetric.put("value", stats.get("employers"));
        employersMetric.put("formatted", formatNumber((Long) stats.get("employers")));
        metrics.add(employersMetric);
        
        // Format jobs
        Map<String, Object> jobsMetric = new HashMap<>();
        jobsMetric.put("label", "Việc làm đang mở");
        jobsMetric.put("value", stats.get("jobs"));
        jobsMetric.put("formatted", formatNumber((Long) stats.get("jobs")));
        metrics.add(jobsMetric);

        Map<String, Object> applicationsMetric = new HashMap<>();
        applicationsMetric.put("label", "Hồ sơ ứng tuyển đã gửi");
        Long applications = (Long) stats.getOrDefault("applications", 0L);
        applicationsMetric.put("value", applications);
        applicationsMetric.put("formatted", formatNumber(applications));
        metrics.add(applicationsMetric);
        
        return metrics;
    }

    private String formatNumber(Long number) {
        if (number == null || number == 0) return "0";
        
        if (number >= 1_000_000) {
            return new DecimalFormat("#.#M+").format(number / 1_000_000.0);
        } else if (number >= 1_000) {
            return new DecimalFormat("#.#K+").format(number / 1_000.0);
        }
        return number.toString();
    }

    public List<Map<String, Object>> getHighlightCards() {
        List<Map<String, Object>> cards = new ArrayList<>();
        
        Map<String, Object> card1 = new HashMap<>();
        card1.put("icon", "fa-solid fa-wand-magic-sparkles");
        card1.put("background", "rgba(0,177,79,0.12)");
        card1.put("color", "var(--home-primary)");
        card1.put("title", "Gợi ý việc làm thông minh");
        card1.put("description", "Thuật toán AI, chuẩn TopCV phân tích hồ sơ và đề xuất công việc phù hợp nhất với bạn.");
        cards.add(card1);
        
        Map<String, Object> card2 = new HashMap<>();
        card2.put("icon", "fa-solid fa-file-signature");
        card2.put("background", "rgba(13,110,253,0.12)");
        card2.put("color", "#0d6efd");
        card2.put("title", "Mẫu CV chuyên nghiệp");
        card2.put("description", "Thư viện CV design chuẩn ATS, tối ưu chuyển đổi và được chuyên gia TopCV kiểm chứng.");
        cards.add(card2);
        
        Map<String, Object> card3 = new HashMap<>();
        card3.put("icon", "fa-solid fa-building");
        card3.put("background", "rgba(255,193,7,0.15)");
        card3.put("color", "#f59f11");
        card3.put("title", "Doanh nghiệp uy tín");
        card3.put("description", "Kết nối với hơn 5.000 thương hiệu lớn như VNPay, Momo, Viettel, FPT cùng nhiều startup.");
        cards.add(card3);
        
        return cards;
    }

    public List<Map<String, Object>> getTopCategories(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }

        Pageable pageable = PageRequest.of(0, limit);
        List<CategoryStatsProjection> stats = categoryRepository.findTopCategoriesWithJobCounts(pageable);

        if (stats.isEmpty()) {
            return buildSampleCategories(limit);
        }

        List<Map<String, Object>> categories = new ArrayList<>();
        for (CategoryStatsProjection stat : stats) {
            Map<String, Object> category = new HashMap<>();
            category.put("id", stat.getId());
            category.put("name", stat.getName());
            category.put("jobCount", stat.getJobCount());
            category.put("job_count", stat.getJobCount());
            category.put("icon", resolveCategoryIcon(stat.getName()));
            categories.add(category);
        }
        return categories;
    }

    public List<String> getPopularKeywords(int limit) {
        int size = Math.max(limit, 1);
        Pageable pageable = PageRequest.of(0, size);
        LinkedHashSet<String> keywords = new LinkedHashSet<>(jobRepository.findRecentJobTitles(pageable));
        
        if (keywords.size() < size) {
            for (String fallback : FALLBACK_KEYWORDS) {
                if (keywords.size() >= size) break;
                keywords.add(fallback);
            }
        }
        
        return keywords.stream().limit(limit).toList();
    }

    public List<Map<String, Object>> getHotJobs(int limit) {
        // Get top viewed jobs from database
        return jobService.getTopViewedJobs(limit);
    }

    public List<Map<String, Object>> getTopEmployers(int limit) {
        if (limit <= 0) {
            return Collections.emptyList();
        }

        Pageable pageable = PageRequest.of(0, limit);
        List<EmployerStatsProjection> stats = employerRepository.findTopEmployersWithStats(pageable);
        List<Map<String, Object>> employers = new ArrayList<>();
        
        for (EmployerStatsProjection stat : stats) {
            Map<String, Object> emp = new HashMap<>();
            emp.put("id", stat.getId());
            emp.put("company_name", stat.getCompanyName());
            emp.put("logo_path", stat.getLogoPath());
            emp.put("address", stat.getAddress());
            emp.put("job_count", stat.getJobCount());
            emp.put("application_count", stat.getApplicationCount());
            employers.add(emp);
        }
        
        if (employers.size() < limit) {
            var fallbackEmployers = employerRepository.findAll();
            for (var employer : fallbackEmployers) {
                if (employers.size() >= limit) break;
                boolean alreadyAdded = employers.stream()
                        .anyMatch(existing -> Objects.equals(existing.get("id"), employer.getId()));
                if (alreadyAdded) continue;
                Map<String, Object> emp = new HashMap<>();
                emp.put("id", employer.getId());
                emp.put("company_name", employer.getCompanyName());
                emp.put("logo_path", employer.getLogoPath());
                emp.put("address", employer.getAddress());
                emp.put("job_count", 0L);
                employers.add(emp);
            }
        }
        
        return employers;
    }

    public List<Map<String, Object>> getBlogArticles() {
        List<Map<String, Object>> jobs = jobService.getTopViewedJobs(3);
        if (jobs.isEmpty()) {
            return new ArrayList<>(FALLBACK_ARTICLES);
        }
        
        List<Map<String, Object>> articles = new ArrayList<>();
        for (Map<String, Object> job : jobs) {
            Map<String, Object> article = new HashMap<>();
            article.put("title", job.getOrDefault("title", "Cơ hội nghề nghiệp nổi bật"));
            article.put("category", job.getOrDefault("categoryNames", job.getOrDefault("employmentType", "Tuyển dụng")));
            article.put("readTime", job.getOrDefault("postedAgo", "Mới"));
            articles.add(article);
        }
        return articles;
    }

    private List<Map<String, Object>> buildSampleCategories(int limit) {
        List<Map<String, Object>> samples = new ArrayList<>();
        List<Map<String, Object>> predefined = List.of(
                createCategorySample("Công nghệ thông tin", 120, "fa-code"),
                createCategorySample("Kinh doanh", 85, "fa-chart-line"),
                createCategorySample("Marketing", 60, "fa-bullhorn"),
                createCategorySample("Thiết kế", 45, "fa-pen-ruler"),
                createCategorySample("Nhân sự", 38, "fa-users"),
                createCategorySample("Kế toán", 30, "fa-calculator")
        );
        for (Map<String, Object> cat : predefined) {
            if (samples.size() >= limit) break;
            samples.add(new HashMap<>(cat));
        }
        return samples;
    }

    private Map<String, Object> createCategorySample(String name, int jobCount, String icon) {
        Map<String, Object> cat = new HashMap<>();
        cat.put("name", name);
        cat.put("jobCount", jobCount);
        cat.put("job_count", jobCount);
        cat.put("icon", icon);
        return cat;
    }

    private String resolveCategoryIcon(String name) {
        if (name == null) {
            return "fa-briefcase";
        }
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        return CATEGORY_ICON_LOOKUP.entrySet().stream()
                .filter(entry -> normalized.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("fa-briefcase");
    }
}
