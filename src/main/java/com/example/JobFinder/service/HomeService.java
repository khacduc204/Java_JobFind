package com.example.JobFinder.service;

import com.example.JobFinder.repository.UserRepository;
import com.example.JobFinder.repository.EmployerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.DecimalFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final UserRepository userRepository;
    private final EmployerRepository employerRepository;
    private final JobService jobService;

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // Count users by role
        long candidates = userRepository.countByRoleId(3); // candidate role
        long employersCount = employerRepository.count();  // actual employers from employer table
        long jobs = 0; // TODO: implement job counting when Job entity is ready
        
        stats.put("candidates", candidates);
        stats.put("employers", employersCount);
        stats.put("jobs", jobs);
        
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
        // TODO: implement when Category and Job entities are ready
        List<Map<String, Object>> categories = new ArrayList<>();
        
        // Sample data for now
        if (limit >= 1) {
            Map<String, Object> cat1 = new HashMap<>();
            cat1.put("name", "Công nghệ thông tin");
            cat1.put("jobCount", 120);
            cat1.put("job_count", 120);
            cat1.put("icon", "fa-code");
            categories.add(cat1);
        }
        
        if (limit >= 2) {
            Map<String, Object> cat2 = new HashMap<>();
            cat2.put("name", "Kinh doanh");
            cat2.put("jobCount", 85);
            cat2.put("job_count", 85);
            cat2.put("icon", "fa-chart-line");
            categories.add(cat2);
        }
        
        if (limit >= 3) {
            Map<String, Object> cat3 = new HashMap<>();
            cat3.put("name", "Marketing");
            cat3.put("jobCount", 60);
            cat3.put("job_count", 60);
            cat3.put("icon", "fa-bullhorn");
            categories.add(cat3);
        }
        
        if (limit >= 4) {
            Map<String, Object> cat4 = new HashMap<>();
            cat4.put("name", "Thiết kế");
            cat4.put("jobCount", 45);
            cat4.put("job_count", 45);
            cat4.put("icon", "fa-palette");
            categories.add(cat4);
        }
        
        if (limit >= 5) {
            Map<String, Object> cat5 = new HashMap<>();
            cat5.put("name", "Nhân sự");
            cat5.put("jobCount", 38);
            cat5.put("job_count", 38);
            cat5.put("icon", "fa-users");
            categories.add(cat5);
        }
        
        if (limit >= 6) {
            Map<String, Object> cat6 = new HashMap<>();
            cat6.put("name", "Kế toán");
            cat6.put("jobCount", 30);
            cat6.put("job_count", 30);
            cat6.put("icon", "fa-calculator");
            categories.add(cat6);
        }
        
        return categories;
    }

    public List<String> getPopularKeywords(int limit) {
        // TODO: implement when Job entity is ready
        return Arrays.asList("Product Manager", "Frontend", "Marketing", "UI/UX", "Data Analyst", "Sales");
    }

    public List<Map<String, Object>> getHotJobs(int limit) {
        // Get top viewed jobs from database
        return jobService.getTopViewedJobs(limit);
    }

    public List<Map<String, Object>> getTopEmployers(int limit) {
        // Get real employers from database
        List<Map<String, Object>> employers = new ArrayList<>();
        
        var allEmployers = employerRepository.findAll();
        for (var employer : allEmployers) {
            if (employers.size() >= limit) break;
            
            Map<String, Object> emp = new HashMap<>();
            emp.put("id", employer.getId());
            emp.put("company_name", employer.getCompanyName());
            emp.put("logo_path", employer.getLogoPath());
            emp.put("address", employer.getAddress());
            emp.put("job_count", 0); // TODO: count actual jobs when Job entity ready
            employers.add(emp);
        }
        
        // If not enough real employers, add sample data
        if (employers.size() < limit) {
            while (employers.size() < limit) {
                Map<String, Object> emp = new HashMap<>();
                emp.put("company_name", "Công ty " + (employers.size() + 1));
                emp.put("job_count", 0);
                employers.add(emp);
            }
        }
        
        return employers;
    }

    public List<Map<String, Object>> getBlogArticles() {
        List<Map<String, Object>> articles = new ArrayList<>();
        
        Map<String, Object> article1 = new HashMap<>();
        article1.put("title", "5 bí kíp nâng cấp CV khiến nhà tuyển dụng ấn tượng ngay lập tức");
        article1.put("category", "CV & Hồ sơ");
        article1.put("readTime", "5 phút đọc");
        articles.add(article1);
        
        Map<String, Object> article2 = new HashMap<>();
        article2.put("title", "Checklist phỏng vấn: Chuẩn bị gì để không bị hỏi khó?");
        article2.put("category", "Phỏng vấn");
        article2.put("readTime", "7 phút đọc");
        articles.add(article2);
        
        Map<String, Object> article3 = new HashMap<>();
        article3.put("title", "Kỹ năng phân tích dữ liệu cho marketer thời 4.0");
        article3.put("category", "Kỹ năng");
        article3.put("readTime", "6 phút đọc");
        articles.add(article3);
        
        return articles;
    }
}
