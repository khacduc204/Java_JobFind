package com.example.JobFinder.admin;

import com.example.JobFinder.model.Candidate;
import com.example.JobFinder.model.User;
import com.example.JobFinder.repository.CandidateRepository;
import com.example.JobFinder.repository.UserRepository;
import com.example.JobFinder.service.CandidateAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/admin/candidates")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('manage_users')")
public class CandidateAdminController {

    private final CandidateAdminService candidateAdminService;
    private final CandidateRepository candidateRepository;
    private final UserRepository userRepository;

    @GetMapping
    public String index(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "") String location,
            @RequestParam(required = false, defaultValue = "") String skill,
            @RequestParam(required = false, defaultValue = "") String cvStatus,
            Model model
    ) {
        Map<String, Object> result = candidateAdminService.getCandidatesList(keyword, location, skill, cvStatus);
        
        model.addAttribute("pageTitle", "Quản lý ứng viên");
        model.addAttribute("candidates", result.get("candidates"));
        model.addAttribute("stats", result.get("stats"));
        model.addAttribute("filters", Map.of(
            "keyword", keyword,
            "location", location,
            "skill", skill,
            "cvStatus", cvStatus
        ));
        
        return "admin/candidates/index";
    }

    @GetMapping("/delete/{userId}")
    public String showDeleteForm(@PathVariable Integer userId, Model model, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(userId).orElse(null);
        
        if (user == null || user.getRole() == null || user.getRole().getId() != 3) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy ứng viên.");
            return "redirect:/admin/candidates";
        }

        Candidate candidate = candidateRepository.findByUserId(userId).orElse(null);
        
        Map<String, Object> candidateData = candidateAdminService.prepareCandidateData(user, candidate);
        
        model.addAttribute("pageTitle", "Xóa ứng viên");
        model.addAttribute("candidate", candidateData);
        
        return "admin/candidates/delete";
    }

    @PostMapping("/delete/{userId}")
    @Transactional
    public String deleteCandidate(@PathVariable Integer userId, RedirectAttributes redirectAttributes) {
        User user = userRepository.findById(userId).orElse(null);
        
        if (user == null || user.getRole() == null || user.getRole().getId() != 3) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy ứng viên.");
            return "redirect:/admin/candidates";
        }

        try {
            // Delete candidate profile if exists
            candidateRepository.findByUserId(userId).ifPresent(candidate -> {
                // TODO: Delete associated files (CV, profile picture)
                candidateRepository.delete(candidate);
            });
            
            // Delete user account
            userRepository.delete(user);
            
            redirectAttributes.addFlashAttribute("success", "Đã xóa ứng viên thành công.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa ứng viên. Vui lòng thử lại sau.");
        }
        
        return "redirect:/admin/candidates";
    }
}
