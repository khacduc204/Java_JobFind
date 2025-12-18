package com.example.JobFinder.service;

import com.example.JobFinder.model.Employer;
import com.example.JobFinder.repository.EmployerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployerService {

    private final EmployerRepository employerRepository;

    public Map<String, Object> getDirectoryPaginated(String searchTerm, String location, String sortOrder, int page, int perPage) {
        Map<String, Object> result = new HashMap<>();
        
        Sort sort = getSort(sortOrder != null ? sortOrder : "featured");
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
        long total = employerRepository.count();
        stats.put("totalEmployers", total);
        stats.put("activeEmployers", total); // TODO: Count active employers
        stats.put("totalJobs", 0); // TODO: Count jobs when Job entity ready
        stats.put("newThisMonth", 0); // TODO: Count new employers this month
        return stats;
    }

    public Map<String, Object> getEmployerProfile(Long id) {
        Optional<Employer> employerOpt = employerRepository.findById(id);
        if (employerOpt.isEmpty()) {
            return null;
        }
        return employerToMap(employerOpt.get());
    }

    public List<Map<String, Object>> getEmployerJobs(Long employerId) {
        // TODO: Implement when Job entity is ready
        return new ArrayList<>();
    }

    public Map<String, Object> getEmployerStats(Long employerId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalJobs", 0); // TODO: Count employer jobs
        stats.put("recentJobs", 0); // TODO: Count jobs from last 30 days
        stats.put("totalViews", 0); // TODO: Sum job views
        stats.put("uniqueLocations", 0); // TODO: Count unique job locations
        return stats;
    }

    public List<String> getEmployerBenefits(Long employerId) {
        // TODO: Parse benefits from employer when benefits field is added to Employer entity
        return getDefaultBenefits();
    }

    public List<Map<String, String>> getCultureHighlights(Map<String, Object> employer) {
        List<Map<String, String>> highlights = new ArrayList<>();
        
        highlights.add(createHighlight("fa-users-line", "Đội ngũ chuyên nghiệp", "Môi trường làm việc năng động với đồng nghiệp tài năng"));
        highlights.add(createHighlight("fa-chart-line", "Cơ hội thăng tiến", "Lộ trình phát triển rõ ràng và công bằng"));
        highlights.add(createHighlight("fa-hand-holding-heart", "Phúc lợi hấp dẫn", "Chế độ đãi ngộ cạnh tranh và đầy đủ"));
        highlights.add(createHighlight("fa-laptop-code", "Công nghệ hiện đại", "Trang bị công cụ và thiết bị làm việc tốt nhất"));
        
        return highlights;
    }

    public List<Map<String, Object>> getHiringTimeline(Long employerId) {
        // TODO: Implement when Job entity is ready
        return new ArrayList<>();
    }

    public List<Map<String, Object>> getRelatedJobs(Long employerId, int limit) {
        // TODO: Implement when Job entity is ready
        List<Map<String, Object>> sampleJobs = new ArrayList<>();
        for (int i = 1; i <= limit; i++) {
            Map<String, Object> job = new HashMap<>();
            job.put("id", i);
            job.put("title", "Vị trí tuyển dụng " + i);
            job.put("companyName", "Công ty mẫu " + i);
            job.put("location", "Hà Nội");
            job.put("salary", "15 - 20 triệu");
            job.put("employmentType", "Full-time");
            sampleJobs.add(job);
        }
        return sampleJobs;
    }

    private Map<String, Object> employerToMap(Employer employer) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", employer.getId());
        map.put("company_name", employer.getCompanyName());
        map.put("about", employer.getAbout());
        map.put("address", employer.getAddress());
        map.put("website", employer.getWebsite());
        map.put("logo_path", employer.getLogoPath());
        map.put("job_count", 0); // TODO: Count when Job entity ready
        
        // Generate company initial
        String companyName = employer.getCompanyName();
        if (companyName != null && !companyName.isEmpty()) {
            String[] words = companyName.split("\\s+");
            String initial = "";
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
}
