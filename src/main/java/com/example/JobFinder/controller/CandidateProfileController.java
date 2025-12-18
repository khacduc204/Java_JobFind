package com.example.JobFinder.controller;

import com.example.JobFinder.model.Candidate;
import com.example.JobFinder.model.User;
import com.example.JobFinder.repository.CandidateRepository;
import com.example.JobFinder.repository.UserRepository;
import com.example.JobFinder.service.CandidateProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/candidate")
@RequiredArgsConstructor
public class CandidateProfileController {

    private final CandidateProfileService candidateProfileService;
    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;

    /**
     * Hiển thị trang hồ sơ ứng viên
     */
    @GetMapping("/profile")
    public String showProfile(@RequestParam(required = false) Integer user,
                             @RequestParam(required = false) Integer candidate,
                             Authentication authentication,
                             Model model) {
        
        User currentUser = null;
        if (authentication != null && authentication.isAuthenticated()) {
            String email = authentication.getName();
            currentUser = userRepository.findByEmailIgnoreCase(email).orElse(null);
        }

        // Lấy thông tin ứng viên theo thứ tự: user param -> candidate param -> current user
        Candidate profileCandidate = candidateProfileService.getProfile(user, candidate, currentUser);
        
        if (profileCandidate == null) {
            // Nếu không tìm thấy, tạo profile mẫu
            model.addAttribute("candidate", null);
            model.addAttribute("user", null);
            model.addAttribute("isDemo", true);
            model.addAttribute("fullName", "Ứng viên JobFind");
            model.addAttribute("email", "candidate@example.com");
            model.addAttribute("headline", "Chuyên viên Marketing Digital");
            model.addAttribute("skillsList", new java.util.ArrayList<>());
            model.addAttribute("experienceList", new java.util.ArrayList<>());
            model.addAttribute("isOwnProfile", false);
            return "frontend/candidate/profile";
        }

        User profileUser = profileCandidate.getUser();
        model.addAttribute("candidate", profileCandidate);
        model.addAttribute("user", profileUser);
        model.addAttribute("fullName", profileUser.getName() != null ? profileUser.getName() : profileUser.getEmail());
        model.addAttribute("email", profileUser.getEmail());
        model.addAttribute("phone", profileUser.getPhone());
        model.addAttribute("headline", profileCandidate.getHeadline());
        model.addAttribute("summary", profileCandidate.getSummary());
        model.addAttribute("location", profileCandidate.getLocation());
        model.addAttribute("cvPath", profileCandidate.getCvPath());
        model.addAttribute("profilePicture", profileCandidate.getProfilePicture());
        model.addAttribute("avatarPath", profileUser.getAvatarPath());
        
        // Parse skills và experience từ JSON
        model.addAttribute("skillsList", candidateProfileService.parseSkills(profileCandidate.getSkills()));
        model.addAttribute("experienceList", candidateProfileService.parseExperience(profileCandidate.getExperience()));
        
        // Check if current user is viewing their own profile
        boolean isOwnProfile = currentUser != null && 
                              profileUser.getId().equals(currentUser.getId());
        model.addAttribute("isOwnProfile", isOwnProfile);
        model.addAttribute("currentUser", currentUser);
        
        return "frontend/candidate/profile";
    }

    /**
     * Hiển thị form chỉnh sửa hồ sơ
     */
    @GetMapping("/edit-profile")
    public String showEditForm(Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập để chỉnh sửa hồ sơ");
            return "redirect:/login";
        }

        String email = authentication.getName();
        User currentUser = userRepository.findByEmailIgnoreCase(email).orElse(null);
        
        if (currentUser == null || currentUser.getRole().getId() != 3) {
            redirectAttributes.addFlashAttribute("error", "Chỉ ứng viên mới có thể chỉnh sửa hồ sơ");
            return "redirect:/";
        }

        Candidate candidate = candidateRepository.findByUserId(currentUser.getId()).orElse(null);
        if (candidate == null) {
            // Tạo candidate mới nếu chưa có
            candidate = new Candidate();
            candidate.setUser(currentUser);
            candidateRepository.save(candidate);
        }

        model.addAttribute("user", currentUser);
        model.addAttribute("candidate", candidate);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("email", currentUser.getEmail());
        model.addAttribute("headline", candidate.getHeadline());
        model.addAttribute("summary", candidate.getSummary());
        model.addAttribute("location", candidate.getLocation());
        model.addAttribute("skillsList", candidateProfileService.parseSkills(candidate.getSkills()));
        model.addAttribute("experienceList", candidateProfileService.parseExperience(candidate.getExperience()));
        model.addAttribute("experienceText", candidate.getExperience());
        
        return "frontend/candidate/edit-profile";
    }

    /**
     * Xử lý cập nhật hồ sơ
     */
    @PostMapping("/edit-profile")
    public String updateProfile(@RequestParam String fullName,
                               @RequestParam(required = false) String headline,
                               @RequestParam(required = false) String summary,
                               @RequestParam(required = false) String location,
                               @RequestParam(required = false) String phone,
                               @RequestParam(required = false) String skills,
                               @RequestParam(required = false) String experience,
                               @RequestParam(required = false) MultipartFile avatar,
                               @RequestParam(required = false) boolean removeAvatar,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            String email = authentication.getName();
            User currentUser = userRepository.findByEmailIgnoreCase(email).orElse(null);
            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy thông tin người dùng");
                return "redirect:/candidate/edit-profile";
            }

            candidateProfileService.updateProfile(currentUser, fullName, headline, summary, 
                                                 location, phone, skills, experience, 
                                                 avatar, removeAvatar);

            redirectAttributes.addFlashAttribute("success", "Cập nhật hồ sơ thành công");
            return "redirect:/candidate/profile?updated=1";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage());
            return "redirect:/candidate/edit-profile";
        }
    }

    /**
     * Hiển thị trang upload CV
     */
    @GetMapping("/upload-cv")
    public String showUploadCvPage(Authentication authentication, Model model, RedirectAttributes redirectAttributes) {
        if (authentication == null || !authentication.isAuthenticated()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng đăng nhập");
            return "redirect:/login";
        }

        String email = authentication.getName();
        User currentUser = userRepository.findByEmailIgnoreCase(email).orElse(null);
        
        if (currentUser == null || currentUser.getRole().getId() != 3) {
            redirectAttributes.addFlashAttribute("error", "Chỉ ứng viên mới có thể tải CV");
            return "redirect:/";
        }

        Candidate candidate = candidateRepository.findByUserId(currentUser.getId()).orElse(null);
        if (candidate == null) {
            candidate = new Candidate();
            candidate.setUser(currentUser);
            candidateRepository.save(candidate);
        }

        model.addAttribute("candidate", candidate);
        model.addAttribute("currentCvPath", candidate.getCvPath());
        model.addAttribute("cvUpdatedAt", candidate.getUpdatedAt());        model.addAttribute("currentUser", currentUser);        
        return "frontend/candidate/upload-cv";
    }

    /**
     * Xử lý upload CV
     */
    @PostMapping("/upload-cv")
    public String uploadCv(@RequestParam("cvFile") MultipartFile cvFile,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            String email = authentication.getName();
            User currentUser = userRepository.findByEmailIgnoreCase(email).orElse(null);
            if (currentUser == null) {
                redirectAttributes.addFlashAttribute("error", "Không tìm thấy thông tin người dùng");
                return "redirect:/candidate/upload-cv";
            }

            candidateProfileService.uploadCv(currentUser, cvFile);

            redirectAttributes.addFlashAttribute("success", "CV đã được tải lên thành công");
            return "redirect:/candidate/upload-cv?uploaded=1";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/candidate/upload-cv";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể tải lên CV: " + e.getMessage());
            return "redirect:/candidate/upload-cv";
        }
    }
}
