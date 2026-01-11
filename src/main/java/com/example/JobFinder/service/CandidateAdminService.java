package com.example.JobFinder.service;

import com.example.JobFinder.model.Candidate;
import com.example.JobFinder.model.User;
import com.example.JobFinder.repository.CandidateRepository;
import com.example.JobFinder.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidateAdminService {

    private final CandidateRepository candidateRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public Map<String, Object> getCandidatesList(String keyword, String location, String skill, String cvStatus) {
        // Get all users with role_id = 3 (candidates)
        List<User> candidateUsers = userRepository.findByRoleId(Integer.valueOf(3));
        
        List<Map<String, Object>> candidates = new ArrayList<>();
        
        for (User user : candidateUsers) {
            Optional<Candidate> candidateOpt = candidateRepository.findByUserId(user.getId());
            
            // Apply filters
            if (!matchesFilters(user, candidateOpt.orElse(null), keyword, location, skill, cvStatus)) {
                continue;
            }
            
            Map<String, Object> candidateData = new HashMap<>();
            candidateData.put("user_id", user.getId());
            candidateData.put("email", user.getEmail());
            candidateData.put("full_name", user.getName() != null ? user.getName() : "Ứng viên JobFind");
            candidateData.put("user_created_at", user.getCreatedAt());
            
            if (candidateOpt.isPresent()) {
                Candidate candidate = candidateOpt.get();
                candidateData.put("candidate_id", candidate.getId());
                candidateData.put("headline", candidate.getHeadline() != null ? candidate.getHeadline() : "Chưa cập nhật");
                candidateData.put("location", candidate.getLocation() != null ? candidate.getLocation() : "Chưa cập nhật");
                candidateData.put("cv_path", candidate.getCvPath());
                candidateData.put("cv_updated_at", candidate.getUpdatedAt());
                candidateData.put("profile_picture", candidate.getProfilePicture());
                
                // Parse skills JSON to get first few skills
                List<String> skillList = parseSkills(candidate.getSkills());
                candidateData.put("skills_list", skillList);
                candidateData.put("skills_display_list", getTopSkills(skillList, 4));
                
                List<Map<String, Object>> experienceList = parseExperience(candidate.getExperience());
                candidateData.put("experience_list", experienceList);
                candidateData.put("experience_count", experienceList.size());
                candidateData.put("experience_summary", experienceList.isEmpty()
                    ? "Chưa cập nhật"
                    : experienceList.size() + " vị trí");
            } else {
                candidateData.put("candidate_id", null);
                candidateData.put("headline", "Chưa cập nhật");
                candidateData.put("location", "Chưa cập nhật");
                candidateData.put("cv_path", null);
                candidateData.put("cv_updated_at", null);
                candidateData.put("skills_list", Collections.emptyList());
                candidateData.put("skills_display_list", Collections.emptyList());
                candidateData.put("experience_list", Collections.emptyList());
                candidateData.put("experience_count", 0);
                candidateData.put("experience_summary", "Chưa cập nhật");
            }
            
            candidates.add(candidateData);
        }
        
        // Calculate stats
        Map<String, Object> stats = calculateStats(candidates);
        
        Map<String, Object> result = new HashMap<>();
        result.put("candidates", candidates);
        result.put("stats", stats);
        
        return result;
    }

    private boolean matchesFilters(User user, Candidate candidate, String keyword, String location, String skill, String cvStatus) {
        // Keyword filter (name or email)
        if (!keyword.isEmpty()) {
            String lowerKeyword = keyword.toLowerCase();
            boolean matchesName = user.getName() != null && user.getName().toLowerCase().contains(lowerKeyword);
            boolean matchesEmail = user.getEmail().toLowerCase().contains(lowerKeyword);
            boolean matchesHeadline = candidate != null && candidate.getHeadline() != null && 
                candidate.getHeadline().toLowerCase().contains(lowerKeyword);
            
            if (!matchesName && !matchesEmail && !matchesHeadline) {
                return false;
            }
        }
        
        // Location filter
        if (!location.isEmpty() && candidate != null) {
            if (candidate.getLocation() == null || !candidate.getLocation().toLowerCase().contains(location.toLowerCase())) {
                return false;
            }
        }
        
        // Skill filter
        if (!skill.isEmpty() && candidate != null) {
            List<String> skills = parseSkills(candidate.getSkills());
            boolean hasSkill = skills.stream().anyMatch(s -> s.toLowerCase().contains(skill.toLowerCase()));
            if (!hasSkill) {
                return false;
            }
        }
        
        // CV status filter
        if (!cvStatus.isEmpty()) {
            boolean hasCv = candidate != null && candidate.getCvPath() != null && !candidate.getCvPath().isEmpty();
            if ("has".equals(cvStatus) && !hasCv) {
                return false;
            }
            if ("missing".equals(cvStatus) && hasCv) {
                return false;
            }
        }
        
        return true;
    }

    private List<String> parseSkills(String skillsJson) {
        if (skillsJson == null || skillsJson.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            List<String> skills = objectMapper.readValue(skillsJson, new TypeReference<List<String>>() {});
            return skills.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<String> getTopSkills(List<String> skills, int limit) {
        if (skills.isEmpty()) {
            return Collections.emptyList();
        }
        int actualLimit = Math.min(limit, skills.size());
        return new ArrayList<>(skills.subList(0, actualLimit));
    }

    private List<Map<String, Object>> parseExperience(String experienceJson) {
        if (experienceJson == null || experienceJson.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(experienceJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private Map<String, Object> calculateStats(List<Map<String, Object>> candidates) {
        Map<String, Object> stats = new HashMap<>();
        
        int totalCandidates = candidates.size();
        int withCv = 0;
        int recentCv = 0;
        
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        
        for (Map<String, Object> candidate : candidates) {
            String cvPath = (String) candidate.get("cv_path");
            LocalDateTime cvUpdatedAt = (LocalDateTime) candidate.get("cv_updated_at");
            
            if (cvPath != null && !cvPath.isEmpty()) {
                withCv++;
                
                if (cvUpdatedAt != null && cvUpdatedAt.isAfter(thirtyDaysAgo)) {
                    recentCv++;
                }
            }
        }
        
        stats.put("totalCandidates", totalCandidates);
        stats.put("withCv", withCv);
        stats.put("recentCv", recentCv);
        
        return stats;
    }

    public Map<String, Object> prepareCandidateData(User user, Candidate candidate) {
        Map<String, Object> data = new HashMap<>();
        
        data.put("user_id", user.getId());
        data.put("email", user.getEmail());
        data.put("full_name", user.getName() != null ? user.getName() : "Ứng viên JobFind");
        data.put("user_created_at", user.getCreatedAt());
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        data.put("created_label", user.getCreatedAt() != null ? user.getCreatedAt().format(formatter) : "Không rõ");
        
        if (candidate != null) {
            data.put("candidate_id", candidate.getId());
            data.put("headline", candidate.getHeadline() != null ? candidate.getHeadline() : "Chưa cập nhật");
            data.put("location", candidate.getLocation() != null ? candidate.getLocation() : "Chưa cập nhật");
            data.put("cv_path", candidate.getCvPath());
            data.put("cv_status", candidate.getCvPath() != null && !candidate.getCvPath().isEmpty() ? "Đã tải lên" : "Chưa có CV");
        } else {
            data.put("candidate_id", null);
            data.put("headline", "Chưa cập nhật");
            data.put("location", "Chưa cập nhật");
            data.put("cv_path", null);
            data.put("cv_status", "Chưa có CV");
        }
        
        return data;
    }
}
