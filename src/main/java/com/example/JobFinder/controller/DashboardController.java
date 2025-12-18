package com.example.JobFinder.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        model.addAttribute("currentUser", authentication != null ? authentication.getName() : "");
        return "frontend/dashboard";
    }

        private List<String> buildSearchKeywords() {
        return List.of("Product Manager", "Frontend", "Marketing", "UI/UX", "Data Analyst", "Sales");
        }

        private List<Map<String, Object>> buildHeroMetrics() {
        return List.of(
            buildMetric("Ứng viên tin dùng JobFind", 2_500_000),
            buildMetric("Nhà tuyển dụng đang tuyển", 5_200),
            buildMetric("Việc làm đang mở", 48_000)
        );
        }

        private Map<String, Object> buildMetric(String label, int value) {
        return Map.of(
            "label", label,
            "value", value,
            "formatted", formatMetric(value)
        );
        }

        private List<Map<String, String>> buildHeroCategories() {
        return List.of(
            Map.of("name", "Công nghệ thông tin", "jobCount", "1.2K"),
            Map.of("name", "Marketing & Growth", "jobCount", "860"),
            Map.of("name", "Tài chính & Kế toán", "jobCount", "540")
        );
        }

        private List<Map<String, String>> buildHighlightCards() {
        return List.of(
            Map.of(
                "icon", "fa-solid fa-wand-magic-sparkles",
                "background", "rgba(0,177,79,0.12)",
                "color", "#00b14f",
                "title", "Gợi ý việc làm thông minh",
                "description", "Thuật toán AI phân tích hồ sơ và đề xuất cơ hội phù hợp"
            ),
            Map.of(
                "icon", "fa-solid fa-file-signature",
                "background", "rgba(13,110,253,0.12)",
                "color", "#0d6efd",
                "title", "CV chuẩn ATS miễn phí",
                "description", "Kho mẫu CV chính hãng TopCV với phong cách hiện đại"
            ),
            Map.of(
                "icon", "fa-solid fa-building",
                "background", "rgba(255,193,7,0.15)",
                "color", "#f59f11",
                "title", "Doanh nghiệp uy tín",
                "description", "Hơn 5.000 thương hiệu, startup và tập đoàn lớn"
            )
        );
        }

        private List<Map<String, String>> buildTopCategories() {
        return List.of(
            Map.of("name", "Công nghệ thông tin", "jobCount", "1.240", "slug", "it", "icon", "fa-code"),
            Map.of("name", "Marketing & Truyền thông", "jobCount", "980", "slug", "marketing", "icon", "fa-bullhorn"),
            Map.of("name", "Tài chính / Ngân hàng", "jobCount", "610", "slug", "finance", "icon", "fa-coins"),
            Map.of("name", "Thiết kế / Sáng tạo", "jobCount", "540", "slug", "design", "icon", "fa-palette"),
            Map.of("name", "Nhân sự / Hành chính", "jobCount", "420", "slug", "hr", "icon", "fa-people-group"),
            Map.of("name", "Chuỗi cung ứng / Logistics", "jobCount", "370", "slug", "logistics", "icon", "fa-truck-fast")
        );
        }

        private List<Map<String, Object>> buildHotJobs() {
        return List.of(
            Map.of(
                "title", "Senior Product Designer",
                "company", "Lumin Tech",
                "location", "Hà Nội",
                "salary", "35 - 45 triệu",
                "employmentType", "Toàn thời gian",
                "viewCount", 3_200,
                "viewsFormatted", formatMetric(3_200),
                "tag", "Design",
                "badgeColor", "linear-gradient(135deg, rgba(255,193,7,0.12), rgba(255,193,7,0.2))",
                "link", "/job/share/view/1"
            ),
            Map.of(
                "title", "Engineering Manager",
                "company", "Nova Fintech",
                "location", "TP. Hồ Chí Minh",
                "salary", "Thỏa thuận",
                "employmentType", "Toàn thời gian",
                "viewCount", 5_800,
                "viewsFormatted", formatMetric(5_800),
                "tag", "Tech Lead",
                "badgeColor", "linear-gradient(135deg, rgba(0,177,79,0.12), rgba(0,177,79,0.22))",
                "link", "/job/share/view/2"
            ),
            Map.of(
                "title", "Performance Marketing Lead",
                "company", "Skylark Digital",
                "location", "Remote",
                "salary", "30 - 40 triệu",
                "employmentType", "Làm việc linh hoạt",
                "viewCount", 2_450,
                "viewsFormatted", formatMetric(2_450),
                "tag", "Marketing",
                "badgeColor", "linear-gradient(135deg, rgba(13,110,253,0.12), rgba(13,110,253,0.2))",
                "link", "/job/share/view/3"
            ),
            Map.of(
                "title", "Data Analyst",
                "company", "NextWave Retail",
                "location", "Đà Nẵng",
                "salary", "22 - 28 triệu",
                "employmentType", "Toàn thời gian",
                "viewCount", 4_120,
                "viewsFormatted", formatMetric(4_120),
                "tag", "Data",
                "badgeColor", "linear-gradient(135deg, rgba(111,66,193,0.12), rgba(111,66,193,0.2))",
                "link", "/job/share/view/4"
            )
        );
        }

        private List<Map<String, String>> buildTopEmployers() {
        return List.of(
            Map.of("name", "VNPay", "jobs", "124"),
            Map.of("name", "Momo", "jobs", "88"),
            Map.of("name", "Viettel", "jobs", "210"),
            Map.of("name", "FPT Software", "jobs", "176"),
            Map.of("name", "Be Group", "jobs", "64"),
            Map.of("name", "Base.vn", "jobs", "42")
        );
        }

        private List<Map<String, String>> buildBlogArticles() {
        return List.of(
            Map.of(
                "title", "7 bước xây dựng Portfolio chuẩn nhà tuyển dụng",
                "category", "Career Guide",
                "time", "12 phút đọc",
                "link", "/blog/portfolio"
            ),
            Map.of(
                "title", "Lộ trình trưởng thành cho Product Manager 2025",
                "category", "Product",
                "time", "9 phút đọc",
                "link", "/blog/product-roadmap"
            ),
            Map.of(
                "title", "Kinh nghiệm phỏng vấn Senior Data Analyst",
                "category", "Data",
                "time", "8 phút đọc",
                "link", "/blog/data-interview"
            )
        );
        }

        private String formatMetric(int value) {
        if (value >= 1_000_000) {
            double million = value / 1_000_000d;
            return String.format("%sM+", trimTrailingZeros(million));
        }
        if (value >= 1_000) {
            double thousand = value / 1_000d;
            return String.format("%sK+", trimTrailingZeros(thousand));
        }
        return value + "+";
        }

        private String trimTrailingZeros(double number) {
        String formatted = String.format("%.1f", number);
        if (formatted.endsWith(".0")) {
            return formatted.substring(0, formatted.length() - 2);
        }
        return formatted;
        }
}
