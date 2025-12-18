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
import java.util.*;

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
        try {
            return objectMapper.readValue(experienceJson, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
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
        String[] lines = experienceInput.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\|");
            if (parts.length >= 1) {
                Map<String, String> exp = new HashMap<>();
                exp.put("title", parts.length > 0 ? parts[0].trim() : "");
                exp.put("company", parts.length > 1 ? parts[1].trim() : "");
                exp.put("start", parts.length > 2 ? parts[2].trim() : "");
                exp.put("end", parts.length > 3 ? parts[3].trim() : "");
                exp.put("description", parts.length > 4 ? parts[4].trim() : "");
                
                if (!exp.get("title").isEmpty()) {
                    experienceList.add(exp);
                }
            }
        }

        return experienceList;
    }
}
