package com.example.JobFinder.service;

import com.example.JobFinder.model.Candidate;
import com.example.JobFinder.model.Job;
import com.example.JobFinder.model.JobView;
import com.example.JobFinder.model.SavedJob;
import com.example.JobFinder.model.User;
import com.example.JobFinder.repository.ApplicationRepository;
import com.example.JobFinder.repository.CandidateRepository;
import com.example.JobFinder.repository.JobRepository;
import com.example.JobFinder.repository.JobViewRepository;
import com.example.JobFinder.repository.SavedJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobService {
    
    private final JobRepository jobRepository;
    private final JobViewRepository jobViewRepository;
    private final SavedJobRepository savedJobRepository;
    private final ApplicationRepository applicationRepository;
    private final CandidateRepository candidateRepository;
    
    /**
     * Get paginated published jobs with filters
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPublishedJobsWithFilters(
            String keyword,
            String location,
            String employmentType,
            Integer categoryId,
            String sortBy,
            int page,
            int perPage) {
        
        Sort sort = getSort(sortBy);
        Pageable pageable = PageRequest.of(page - 1, perPage, sort);
        
        Page<Job> jobPage = jobRepository.findPublishedJobsWithFilters(
            keyword, location, employmentType, categoryId, pageable
        );
        
        List<Map<String, Object>> jobs = jobPage.getContent().stream()
                .map(this::jobToMap)
                .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("jobs", jobs);
        result.put("total", jobPage.getTotalElements());
        result.put("totalPages", jobPage.getTotalPages());
        result.put("currentPage", page);
        result.put("perPage", perPage);
        
        return result;
    }
    
    /**
     * Get hot jobs (sorted by view count)
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getHotJobs(int page, int perPage) {
        Pageable pageable = PageRequest.of(page - 1, perPage);
        
        Page<Job> jobPage = jobRepository.findHotJobsByViewCount(pageable);
        
        List<Map<String, Object>> jobs = jobPage.getContent().stream()
                .map(this::jobToMap)
                .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("jobs", jobs);
        result.put("total", jobPage.getTotalElements());
        result.put("totalPages", jobPage.getTotalPages());
        result.put("currentPage", page);
        result.put("perPage", perPage);
        
        return result;
    }
    
    /**
     * Get job detail and record view
     */
    @Transactional
    public Map<String, Object> getJobDetail(Integer jobId, String viewerIp) {
        Optional<Job> jobOpt = jobRepository.findByIdWithDetails(jobId);
        
        if (jobOpt.isEmpty()) {
            return null;
        }
        
        Job job = jobOpt.get();
        
        // Record view (check if not viewed recently to avoid duplicates)
        if (viewerIp != null && !viewerIp.isEmpty()) {
            LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
            long recentViews = jobViewRepository.countRecentViewByJobIdAndIp(jobId, viewerIp, oneHourAgo);
            
            if (recentViews == 0) {
                JobView jobView = new JobView();
                jobView.setJob(job);
                jobView.setViewerIp(viewerIp);
                jobViewRepository.save(jobView);
            }
        }
        
        return jobToMap(job);
    }
    
    /**
     * Get job detail by ID with application check
     */
    @Transactional
    public Map<String, Object> getJobDetailById(Integer jobId, Integer candidateId) {
        Optional<Job> jobOpt = jobRepository.findByIdWithDetails(jobId);
        
        if (jobOpt.isEmpty()) {
            return null;
        }
        
        Job job = jobOpt.get();
        Map<String, Object> jobMap = jobToMap(job);
        
        // Check if candidate already applied
        if (candidateId != null) {
            boolean hasApplied = applicationRepository.existsByCandidateIdAndJobId(candidateId, jobId);
            jobMap.put("hasApplied", hasApplied);
        } else {
            jobMap.put("hasApplied", false);
        }
        
        return jobMap;
    }
    
    /**
     * Get saved jobs for candidate
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSavedJobsByCandidate(Integer candidateId, int page, int perPage) {
        Pageable pageable = PageRequest.of(page - 1, perPage, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<Job> jobPage = jobRepository.findSavedJobsByCandidateId(candidateId, pageable);
        
        List<Map<String, Object>> jobs = jobPage.getContent().stream()
                .map(this::jobToMap)
                .collect(Collectors.toList());
        
        Map<String, Object> result = new HashMap<>();
        result.put("jobs", jobs);
        result.put("total", jobPage.getTotalElements());
        result.put("totalPages", jobPage.getTotalPages());
        result.put("currentPage", page);
        result.put("perPage", perPage);
        
        return result;
    }
    
    /**
     * Get top N most viewed jobs for homepage
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopViewedJobs(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        
        List<Job> jobs = jobRepository.findTopJobsByViewCount(pageable);
        
        return jobs.stream()
                .map(this::jobToMap)
                .collect(Collectors.toList());
    }

    /**
     * Recommend jobs for a candidate using SQL scoring (skills + location)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecommendedJobsForCandidate(Integer candidateId, int limit) {
        if (candidateId == null || limit <= 0) {
            return Collections.emptyList();
        }

        Optional<Candidate> candidateOpt = candidateRepository.findById(candidateId);
        if (candidateOpt.isEmpty()) {
            return Collections.emptyList();
        }

        Pageable pageable = PageRequest.of(0, limit);
        List<Object[]> rows = jobRepository.findRecommendedJobIds(candidateId, candidateOpt.get().getLocation(), pageable);

        if (rows.isEmpty()) {
            return getTopViewedJobs(limit);
        }

        List<Integer> jobIds = rows.stream()
                .map(r -> ((Number) r[0]).intValue())
                .toList();

        Map<Integer, Double> scoreMap = new HashMap<>();
        double maxScore = 0;
        for (Object[] row : rows) {
            int id = ((Number) row[0]).intValue();
            double score = row[1] != null ? ((Number) row[1]).doubleValue() : 0.0;
            scoreMap.put(id, score);
            if (score > maxScore) {
                maxScore = score;
            }
        }

        List<Job> jobs = jobRepository.findByIdInWithDetails(jobIds);
        Map<Integer, Job> jobMap = jobs.stream()
                .collect(Collectors.toMap(Job::getId, j -> j));

        List<Map<String, Object>> recommendations = new ArrayList<>();

        for (Integer jobId : jobIds) {
            Job job = jobMap.get(jobId);
            if (job == null) {
                continue;
            }

            Map<String, Object> jobData = jobToMap(job);
            double rawScore = scoreMap.getOrDefault(jobId, 0.0);
            int normalizedScore = maxScore > 0 ? (int) Math.round((rawScore / maxScore) * 100) : 50;
            normalizedScore = Math.max(5, Math.min(100, normalizedScore));

            jobData.put("matchScore", normalizedScore);
            jobData.put("matchLabel", normalizedScore >= 80 ? "Rất phù hợp" : normalizedScore >= 60 ? "Khá phù hợp" : "Tham khảo");
            recommendations.add(jobData);
        }

        return recommendations;
    }
    
    /**
     * Save/unsave job for candidate
     */
    @Transactional
    public boolean toggleSaveJob(Integer candidateId, Integer jobId) {
        Optional<SavedJob> existing = savedJobRepository.findByCandidateIdAndJobId(candidateId, jobId);
        
        if (existing.isPresent()) {
            // Unsave
            savedJobRepository.delete(existing.get());
            return false;
        } else {
            // Save
            SavedJob savedJob = new SavedJob();
            savedJob.setCandidate(new com.example.JobFinder.model.Candidate());
            savedJob.getCandidate().setId(candidateId);
            savedJob.setJob(new Job());
            savedJob.getJob().setId(jobId);
            savedJobRepository.save(savedJob);
            return true;
        }
    }
    
    /**
     * Check if job is saved by candidate
     */
    public boolean isJobSavedByCandidate(Integer candidateId, Integer jobId) {
        return savedJobRepository.existsByCandidateIdAndJobId(candidateId, jobId);
    }
    
    /**
     * Get list of saved job IDs for candidate
     */
    public List<Integer> getSavedJobIds(Integer candidateId) {
        return savedJobRepository.findJobIdsByCandidateId(candidateId);
    }
    
    /**
     * Get count of saved jobs by candidate
     */
    public long countSavedJobsByCandidate(Integer candidateId) {
        return savedJobRepository.countByCandidateId(candidateId);
    }
    
    /**
     * Format job time ago
     */
    public String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "Chưa xác định";
        }
        
        LocalDateTime now = LocalDateTime.now();
        long seconds = java.time.Duration.between(dateTime, now).getSeconds();
        
        if (seconds < 60) {
            return "Vừa xong";
        }
        
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + (minutes == 1 ? " phút trước" : " phút trước");
        }
        
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + (hours == 1 ? " giờ trước" : " giờ trước");
        }
        
        long days = hours / 24;
        if (days == 1) {
            return "1 ngày trước";
        }
        if (days < 7) {
            return days + " ngày trước";
        }
        
        long weeks = days / 7;
        if (weeks == 1) {
            return "1 tuần trước";
        }
        if (weeks < 5) {
            return weeks + " tuần trước";
        }
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return dateTime.format(formatter);
    }
    
    /**
     * Convert Job entity to Map for template
     */
    private Map<String, Object> jobToMap(Job job) {
        Map<String, Object> map = new HashMap<>();
        
        map.put("id", job.getId());
        map.put("title", job.getTitle());
        map.put("description", job.getDescription());
        map.put("jobRequirements", job.getJobRequirements());
        map.put("requirements", job.getJobRequirements()); // Alias for template
        map.put("location", job.getLocation() != null ? job.getLocation() : "Toàn quốc");
        map.put("salary", job.getSalary() != null ? job.getSalary() : "Thỏa thuận");
        map.put("employmentType", job.getEmploymentType() != null ? job.getEmploymentType() : "Full-time");
        map.put("bannerImage", job.getBannerImage());
        map.put("status", job.getStatus());
        map.put("quantity", job.getQuantity());
        map.put("deadline", job.getDeadline());
        map.put("viewCount", jobViewRepository.countByJobId(job.getId()));
        map.put("createdAt", job.getCreatedAt());
        map.put("updatedAt", job.getUpdatedAt());
        
        // Format dates
        if (job.getCreatedAt() != null) {
            map.put("postedAgo", formatTimeAgo(job.getCreatedAt()));
        }
        
        if (job.getDeadline() != null) {
            map.put("deadlineFormatted", job.getDeadline().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        }
        
        // Employer info
        if (job.getEmployer() != null) {
            Map<String, Object> employerMap = new HashMap<>();
            employerMap.put("id", job.getEmployer().getId());
            employerMap.put("companyName", job.getEmployer().getCompanyName());
            employerMap.put("logoPath", job.getEmployer().getLogoPath());
            employerMap.put("address", job.getEmployer().getAddress());
            employerMap.put("website", job.getEmployer().getWebsite());
            employerMap.put("about", job.getEmployer().getAbout());
            
            // Company initial for fallback
            String companyName = job.getEmployer().getCompanyName();
            if (companyName != null && !companyName.isEmpty()) {
                String[] words = companyName.split("\\s+");
                if (words.length >= 2) {
                    employerMap.put("initial", 
                        words[0].substring(0, 1).toUpperCase() + 
                        words[1].substring(0, 1).toUpperCase());
                } else {
                    employerMap.put("initial", companyName.substring(0, Math.min(2, companyName.length())).toUpperCase());
                }
            }
            
            map.put("employer", employerMap);
            map.put("companyName", job.getEmployer().getCompanyName());
            map.put("logoPath", job.getEmployer().getLogoPath());
        }
        
        // Categories
        if (job.getCategories() != null && !job.getCategories().isEmpty()) {
            List<Map<String, Object>> categories = job.getCategories().stream()
                    .map(cat -> {
                        Map<String, Object> catMap = new HashMap<>();
                        catMap.put("id", cat.getId());
                        catMap.put("name", cat.getName());
                        return catMap;
                    })
                    .collect(Collectors.toList());
            map.put("categories", categories);
            
            // Category names as comma-separated string
            String categoryNames = job.getCategories().stream()
                    .map(cat -> cat.getName())
                    .collect(Collectors.joining(", "));
            map.put("categoryNames", categoryNames);
        } else {
            map.put("categories", Collections.emptyList());
            map.put("categoryNames", "Chưa phân loại");
        }
        
        return map;
    }
    
    /**
     * Get sort object based on sort string
     */
    private Sort getSort(String sortBy) {
        if (sortBy == null || sortBy.isEmpty()) {
            sortBy = "newest";
        }
        
        return switch (sortBy.toLowerCase()) {
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "views" -> Sort.by(Sort.Direction.DESC, "viewCount");
            case "salary" -> Sort.by(Sort.Direction.DESC, "salary");
            default -> Sort.by(Sort.Direction.DESC, "createdAt"); // newest
        };
    }
}
