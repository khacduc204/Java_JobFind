package com.example.JobFinder.service;

import com.example.JobFinder.repository.ApplicationRepository;
import com.example.JobFinder.repository.CandidateRepository;
import com.example.JobFinder.repository.EmployerRepository;
import com.example.JobFinder.repository.JobRepository;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private static final List<String> PIPELINE_ORDER = List.of("applied", "viewed", "shortlisted", "rejected", "hired");
    private static final Map<String, String> STATUS_LABELS = Map.of(
        "applied", "Đã ứng tuyển",
        "viewed", "Nhà tuyển dụng đã xem",
        "shortlisted", "Được chọn phỏng vấn",
        "rejected", "Đã từ chối",
        "hired", "Đã trúng tuyển"
    );
    private static final Map<String, String> STATUS_COLORS = Map.of(
        "applied", "#6C63FF",
        "viewed", "#1B84FF",
        "shortlisted", "#FDBA74",
        "rejected", "#F97066",
        "hired", "#22C55E"
    );
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MM/yyyy", Locale.getDefault());

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final EmployerRepository employerRepository;
    private final CandidateRepository candidateRepository;

    public DashboardData buildDashboardData() {
        long totalJobs = jobRepository.count();
        long activeJobs = jobRepository.countByStatus("published");
        long totalApplications = applicationRepository.count();
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long applicationsLast30 = applicationRepository.countSince(thirtyDaysAgo);
        long shortlistedCount = applicationRepository.countByStatus("shortlisted");
        long hiredCount = applicationRepository.countByStatus("hired");

        double interviewRate = totalApplications == 0 ? 0 : (shortlistedCount * 100d) / totalApplications;
        double hireRate = totalApplications == 0 ? 0 : (hiredCount * 100d) / totalApplications;

        List<PipelineStatus> pipelineStatuses = buildPipelineStatuses();
        List<MonthlyActivityPoint> monthlyActivity = buildMonthlyActivity();
        List<FeaturedEmployerStats> featuredEmployers = buildFeaturedEmployers();

        long employerCount = employerRepository.count();
        long candidateCount = candidateRepository.count();

        return new DashboardData(
            totalJobs,
            activeJobs,
            totalApplications,
            applicationsLast30,
            shortlistedCount,
            hiredCount,
            interviewRate,
            hireRate,
            employerCount,
            candidateCount,
            pipelineStatuses,
            monthlyActivity,
            featuredEmployers
        );
    }

    private List<PipelineStatus> buildPipelineStatuses() {
        Map<String, Long> counts = new LinkedHashMap<>();
        long total = 0;
        for (String status : PIPELINE_ORDER) {
            long count = applicationRepository.countByStatus(status);
            counts.put(status, count);
            total += count;
        }

        List<PipelineStatus> result = new ArrayList<>();
        for (String status : PIPELINE_ORDER) {
            long count = counts.getOrDefault(status, 0L);
            double percentage = total == 0 ? 0 : (count * 100d) / total;
            result.add(new PipelineStatus(
                status,
                STATUS_LABELS.getOrDefault(status, status),
                count,
                percentage,
                STATUS_COLORS.getOrDefault(status, "#D1D5DB")
            ));
        }
        return result;
    }

    private List<MonthlyActivityPoint> buildMonthlyActivity() {
        YearMonth currentMonth = YearMonth.now();
        Map<YearMonth, MonthlyAccumulator> buckets = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth month = currentMonth.minusMonths(i);
            buckets.put(month, new MonthlyAccumulator());
        }

        LocalDateTime startDate = currentMonth.minusMonths(5).atDay(1).atStartOfDay();

        jobRepository.countJobsGroupedByMonth(startDate).forEach(row -> {
            YearMonth key = buildYearMonth(row[0], row[1]);
            MonthlyAccumulator accumulator = buckets.get(key);
            if (accumulator != null) {
                accumulator.jobs = ((Number) row[2]).longValue();
            }
        });

        applicationRepository.countApplicationsByStatusPerMonth(startDate).forEach(row -> {
            YearMonth key = buildYearMonth(row[0], row[1]);
            MonthlyAccumulator accumulator = buckets.get(key);
            if (accumulator != null) {
                long count = ((Number) row[3]).longValue();
                accumulator.applications += count;
                String status = row[2] != null ? row[2].toString() : "";
                if ("shortlisted".equals(status)) {
                    accumulator.interviews += count;
                }
                if ("hired".equals(status)) {
                    accumulator.hires += count;
                }
            }
        });

        List<MonthlyActivityPoint> points = new ArrayList<>();
        for (Map.Entry<YearMonth, MonthlyAccumulator> entry : buckets.entrySet()) {
            YearMonth month = entry.getKey();
            MonthlyAccumulator accumulator = entry.getValue();
            points.add(new MonthlyActivityPoint(
                month.format(MONTH_LABEL),
                accumulator.jobs,
                accumulator.applications,
                accumulator.interviews,
                accumulator.hires
            ));
        }
        return points;
    }

    private List<FeaturedEmployerStats> buildFeaturedEmployers() {
        return employerRepository.findTopEmployersWithStats(PageRequest.of(0, 5)).stream()
            .map(stat -> new FeaturedEmployerStats(
                stat.getId(),
                stat.getCompanyName() != null ? stat.getCompanyName() : "",
                stat.getJobCount() != null ? stat.getJobCount() : 0L,
                stat.getApplicationCount() != null ? stat.getApplicationCount() : 0L,
                stat.getHiredCount() != null ? stat.getHiredCount() : 0L
            ))
            .toList();
    }

    private YearMonth buildYearMonth(Object year, Object month) {
        if (year == null || month == null) {
            return null;
        }
        int y = ((Number) year).intValue();
        int m = ((Number) month).intValue();
        return YearMonth.of(y, m);
    }

    private static class MonthlyAccumulator {
        long jobs;
        long applications;
        long interviews;
        long hires;
    }

    public record DashboardData(
        long totalJobs,
        long activeJobs,
        long totalApplications,
        long applicationsLast30,
        long shortlistedCount,
        long hiredCount,
        double interviewRate,
        double hireRate,
        long employerCount,
        long candidateCount,
        List<PipelineStatus> pipelineStatuses,
        List<MonthlyActivityPoint> monthlyActivity,
        List<FeaturedEmployerStats> featuredEmployers
    ) {}

    public record PipelineStatus(String key, String label, long count, double percentage, String color) {}

    public record MonthlyActivityPoint(String label, long jobs, long applications, long interviews, long hires) {
        public boolean hasData() {
            return jobs > 0 || applications > 0 || interviews > 0 || hires > 0;
        }
    }

    public record FeaturedEmployerStats(Integer employerId, String companyName, long jobCount, long applicationCount, long hiredCount) {}
}
