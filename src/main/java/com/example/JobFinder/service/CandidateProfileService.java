package com.example.JobFinder.service;

import com.example.JobFinder.model.Candidate;
import com.example.JobFinder.model.User;
import com.example.JobFinder.repository.CandidateRepository;
import com.example.JobFinder.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CandidateProfileService {

    private final CandidateRepository candidateRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    private static final String UPLOAD_DIR = "uploads/";
    private static final String CV_DIR = UPLOAD_DIR + "cv/";
    private static final String AVATAR_DIR = UPLOAD_DIR + "avatars/";
    private static final long MAX_CV_SIZE = 5 * 1024 * 1024; // 5MB
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024; // 2MB
    private static final List<String> ALLOWED_CV_TYPES = Arrays.asList(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/gif",
        "image/webp"
    );

    /**
     * Lấy profile ứng viên theo thứ tự ưu tiên
     */
    public Candidate getProfile(Integer userId, Integer candidateId, User currentUser) {
        // Tìm theo user ID
        if (userId != null) {
            Optional<Candidate> candidate = candidateRepository.findByUserId(userId);
            if (candidate.isPresent()) {
                return candidate.get();
            }
        }

        // Tìm theo candidate ID
        if (candidateId != null) {
            Optional<Candidate> candidate = candidateRepository.findById(candidateId);
            if (candidate.isPresent()) {
                return candidate.get();
            }
        }

        // Tìm theo current user
        if (currentUser != null && currentUser.getRole().getId() == 3) {
            Optional<Candidate> candidate = candidateRepository.findByUserId(currentUser.getId());
            if (candidate.isPresent()) {
                return candidate.get();
            }
        }

        return null;
    }

    /**
     * Parse skills từ JSON string
     */
    public List<String> parseSkills(String skillsJson) {
        if (skillsJson == null || skillsJson.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<String> skills = objectMapper.readValue(skillsJson, new TypeReference<List<String>>() {});
            return skills.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .toList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Parse experience từ JSON string
     */
    public List<Map<String, Object>> parseExperience(String experienceJson) {
        if (experienceJson == null || experienceJson.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> parsed = tryParseExperienceArray(experienceJson.trim());
        if (parsed.isEmpty()) {
            return Collections.emptyList();
        }

        return normalizeExperienceList(parsed);
    }

    /**
     * Convert stored experience JSON into textarea-friendly lines.
     */
    public String formatExperienceTextarea(String experienceJson) {
        List<Map<String, Object>> experiences = parseExperience(experienceJson);
        if (experiences.isEmpty()) {
            return "";
        }

        return experiences.stream()
            .map(this::buildExperienceLine)
            .filter(line -> !line.isBlank())
            .collect(Collectors.joining("\n"));
    }

    /**
     * Cập nhật hồ sơ ứng viên
     */
    @Transactional
    public void updateProfile(User user, String fullName, String headline, String summary,
                             String location, String phone, String skills, String experience,
                             MultipartFile avatar, boolean removeAvatar) throws IOException {
        
        // Cập nhật thông tin user
        user.setName(fullName);
        user.setPhone(phone);

        // Xử lý avatar
        if (removeAvatar && user.getAvatarPath() != null) {
            deleteFile(user.getAvatarPath());
            user.setAvatarPath(null);
        } else if (avatar != null && !avatar.isEmpty()) {
            validateImageFile(avatar);
            String avatarPath = saveFile(avatar, AVATAR_DIR, "avatar");
            if (user.getAvatarPath() != null) {
                deleteFile(user.getAvatarPath());
            }
            user.setAvatarPath(avatarPath);
        }

        userRepository.save(user);

        // Cập nhật hoặc tạo candidate
        Candidate candidate = candidateRepository.findByUserId(user.getId())
            .orElse(new Candidate());
        
        if (candidate.getId() == null) {
            candidate.setUser(user);
        }

        // Đồng bộ avatar từ user sang candidate.profilePicture
        // CHỈ LƯU FILENAME, không lưu full path để tránh duplicate prefix trong template
        if (removeAvatar) {
            candidate.setProfilePicture(null);
        } else if (user.getAvatarPath() != null) {
            // Extract filename từ path: "uploads/avatars/avatar_xxx.webp" -> "avatar_xxx.webp"
            String avatarFilename = user.getAvatarPath();
            if (avatarFilename.contains("/")) {
                avatarFilename = avatarFilename.substring(avatarFilename.lastIndexOf("/") + 1);
            }
            candidate.setProfilePicture(avatarFilename);
        }

        candidate.setHeadline(headline);
        candidate.setSummary(summary);
        candidate.setLocation(location);

        // Parse và lưu skills
        if (skills != null && !skills.trim().isEmpty()) {
            List<String> skillsList = Arrays.stream(skills.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
            candidate.setSkills(objectMapper.writeValueAsString(skillsList));
        }

        // Parse và lưu experience
        if (experience != null && !experience.trim().isEmpty()) {
            List<Map<String, String>> experienceList = parseExperienceInput(experience);
            candidate.setExperience(objectMapper.writeValueAsString(experienceList));
        } else {
            candidate.setExperience(null);
        }

        candidateRepository.save(candidate);
    }

    /**
     * Upload CV
     */
    @Transactional
    public void uploadCv(User user, MultipartFile cvFile) throws IOException {
        validateCvFile(cvFile);

        Candidate candidate = candidateRepository.findByUserId(user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ ứng viên"));

        String cvPath = saveFile(cvFile, CV_DIR, "cv");
        
        // Xóa CV cũ nếu có
        if (candidate.getCvPath() != null) {
            deleteFile(candidate.getCvPath());
        }

        candidate.setCvPath(cvPath);
        candidateRepository.save(candidate);
    }

    /**
     * Validate CV file
     */
    private void validateCvFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn tệp CV");
        }

        if (file.getSize() > MAX_CV_SIZE) {
            throw new IllegalArgumentException("Kích thước tệp CV vượt quá 5MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CV_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Chỉ chấp nhận định dạng PDF, DOC hoặc DOCX");
        }
    }

    /**
     * Validate image file
     */
    private void validateImageFile(MultipartFile file) throws IOException {
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new IOException("Kích thước ảnh vượt quá 2MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new IOException("Chỉ chấp nhận định dạng JPG, PNG, GIF hoặc WEBP");
        }
    }

    /**
     * Lưu file
     */
    private String saveFile(MultipartFile file, String directory, String prefix) throws IOException {
        // Tạo thư mục nếu chưa có
        Path uploadPath = Paths.get(directory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Tạo tên file unique
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = prefix + "_" + System.currentTimeMillis() + "_" + 
                         UUID.randomUUID().toString().substring(0, 8) + extension;

        // Lưu file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return directory + filename;
    }

    /**
     * Xóa file
     */
    private void deleteFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // Log error but don't throw
            System.err.println("Could not delete file: " + filePath);
        }
    }

    /**
     * Parse experience input từ textarea
     * Format: "Title | Company | YYYY-MM | YYYY-MM | Description"
     */
    private List<Map<String, String>> parseExperienceInput(String experienceInput) {
        List<Map<String, String>> experienceList = new ArrayList<>();
        if (experienceInput == null) {
            return experienceList;
        }

        String trimmedInput = experienceInput.trim();
        if (trimmedInput.isEmpty()) {
            return experienceList;
        }

        // Allow users to paste raw JSON
        if (looksLikeJson(trimmedInput)) {
            List<Map<String, Object>> parsed = tryParseExperienceArray(trimmedInput);
            if (!parsed.isEmpty()) {
                return parsed.stream()
                    .map(this::mapToStringMap)
                    .collect(Collectors.toList());
            }
        }

        String[] lines = trimmedInput.split("\r?\n");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (looksLikeJson(line)) {
                List<Map<String, Object>> parsedLine = tryParseExperienceArray(line);
                if (!parsedLine.isEmpty()) {
                    parsedLine.stream()
                        .map(this::mapToStringMap)
                        .filter(entry -> !entry.values().stream().allMatch(String::isBlank))
                        .forEach(experienceList::add);
                    continue;
                }
            }

            String[] parts = line.split("\\|");
            Map<String, String> exp = new HashMap<>();
            exp.put("title", parts.length > 0 ? parts[0].trim() : "");
            exp.put("company", parts.length > 1 ? parts[1].trim() : "");
            exp.put("start", parts.length > 2 ? parts[2].trim() : "");
            exp.put("end", parts.length > 3 ? parts[3].trim() : "");
            exp.put("description", parts.length > 4 ? parts[4].trim() : "");

            boolean hasContent = exp.values().stream().anyMatch(value -> value != null && !value.isBlank());
            if (hasContent) {
                experienceList.add(exp);
            }
        }

        return experienceList;
    }

    private List<Map<String, Object>> tryParseExperienceArray(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }

        String candidateJson = json.trim();
        try {
            return objectMapper.readValue(candidateJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception primary) {
            // Attempt to unescape nested JSON strings
            String normalized = candidateJson;
            if ((normalized.startsWith("\"") && normalized.endsWith("\"")) ||
                (normalized.startsWith("'") && normalized.endsWith("'"))) {
                normalized = normalized.substring(1, normalized.length() - 1);
            }
            normalized = normalized.replace("\\\"", "\"");
            try {
                return objectMapper.readValue(normalized, new TypeReference<List<Map<String, Object>>>() {});
            } catch (Exception secondary) {
                return Collections.emptyList();
            }
        }
    }

    private List<Map<String, Object>> normalizeExperienceList(List<Map<String, Object>> rawList) {
        return rawList.stream()
            .map(this::normalizeExperienceEntry)
            .filter(entry -> entry.values().stream()
                .anyMatch(value -> value instanceof String str ? !str.isBlank() : value != null))
            .collect(Collectors.toList());
    }

    private Map<String, Object> normalizeExperienceEntry(Map<String, Object> entry) {
        if (entry == null || entry.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> working = unwrapNestedExperience(entry);

        String title = safeValue(working.get("title"));
        String company = safeValue(working.get("company"));
        String start = safeValue(working.get("start"));
        String end = safeValue(working.get("end"));
        String description = safeValue(working.get("description"));

        Map<String, Object> normalized = new LinkedHashMap<>();
        normalized.put("title", title);
        normalized.put("company", company);
        normalized.put("start", start);
        normalized.put("end", end);
        normalized.put("description", description);
        normalized.put("startDisplay", formatExperienceDate(start));
        normalized.put("endDisplay", end.isBlank() ? "Hiện tại" : formatExperienceDate(end));
        normalized.put("isCurrent", end.isBlank());

        return normalized;
    }

    private Map<String, Object> unwrapNestedExperience(Map<String, Object> entry) {
        if (entry == null || entry.isEmpty()) {
            return new LinkedHashMap<>();
        }

        Map<String, Object> working = new LinkedHashMap<>(entry);

        if (working.size() == 1) {
            Object firstValue = working.values().iterator().next();
            if (firstValue instanceof String nested && looksLikeJson(nested)) {
                List<Map<String, Object>> nestedList = tryParseExperienceArray(nested);
                if (!nestedList.isEmpty()) {
                    return new LinkedHashMap<>(nestedList.get(0));
                }
            }
        }

        Object titleValue = working.get("title");
        if (titleValue instanceof String nestedTitle && looksLikeJson(nestedTitle)) {
            List<Map<String, Object>> nestedList = tryParseExperienceArray(nestedTitle);
            if (!nestedList.isEmpty()) {
                return new LinkedHashMap<>(nestedList.get(0));
            }
        }

        return working;
    }

    private Map<String, String> mapToStringMap(Map<String, Object> source) {
        Map<String, String> target = new LinkedHashMap<>();
        target.put("title", safeValue(source != null ? source.get("title") : null));
        target.put("company", safeValue(source != null ? source.get("company") : null));
        target.put("start", safeValue(source != null ? source.get("start") : null));
        target.put("end", safeValue(source != null ? source.get("end") : null));
        target.put("description", safeValue(source != null ? source.get("description") : null));
        return target;
    }

    private String safeValue(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private boolean looksLikeJson(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        return !trimmed.isEmpty() && (trimmed.startsWith("{") || trimmed.startsWith("["));
    }

    private String formatExperienceDate(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String trimmed = value.trim();
        DateTimeFormatter monthYearFormatter = DateTimeFormatter.ofPattern("MM/yyyy");

        try {
            LocalDate date = LocalDate.parse(trimmed);
            return date.format(monthYearFormatter);
        } catch (DateTimeParseException ignored) { }

        try {
            YearMonth ym = YearMonth.parse(trimmed);
            return ym.format(monthYearFormatter);
        } catch (DateTimeParseException ignored) { }

        try {
            LocalDate date = LocalDate.parse(trimmed, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            return date.format(monthYearFormatter);
        } catch (DateTimeParseException ignored) { }

        try {
            YearMonth ym = YearMonth.parse(trimmed, DateTimeFormatter.ofPattern("MM/yyyy"));
            return ym.format(monthYearFormatter);
        } catch (DateTimeParseException ignored) { }

        return trimmed;
    }

    private String buildExperienceLine(Map<String, Object> experience) {
        if (experience == null) {
            return "";
        }

        List<String> segments = Arrays.asList(
            safeValue(experience.get("title")),
            safeValue(experience.get("company")),
            safeValue(experience.get("start")),
            safeValue(experience.get("end")),
            safeValue(experience.get("description"))
        );

        int lastIndex = -1;
        for (int i = segments.size() - 1; i >= 0; i--) {
            if (!segments.get(i).isBlank()) {
                lastIndex = i;
                break;
            }
        }

        if (lastIndex == -1) {
            return "";
        }

        return segments.subList(0, lastIndex + 1)
            .stream()
            .map(segment -> segment == null ? "" : segment)
            .collect(Collectors.joining(" | "));
    }
}
