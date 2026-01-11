package com.example.JobFinder.service;

import com.example.JobFinder.model.Candidate;
import com.example.JobFinder.model.User;
import com.example.JobFinder.repository.CandidateRepository;
import com.example.JobFinder.repository.UserRepository;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class HeaderContextService {

    private final UserRepository userRepository;
    private final CandidateRepository candidateRepository;
    private final JobService jobService;
    private final NotificationService notificationService;

    public Optional<HeaderUserContext> buildContext(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() ||
            authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        String email = authentication.getName();
        return userRepository.findByEmailIgnoreCase(email).map(this::mapToContext);
    }

    private HeaderUserContext mapToContext(User user) {
        String displayName = StringUtils.hasText(user.getName()) ? user.getName() : user.getEmail();
        String roleKey = user.getRole() != null && user.getRole().getName() != null
            ? user.getRole().getName().toLowerCase(Locale.ROOT)
            : "";

        long savedJobsCount = 0;
        if ("candidate".equals(roleKey)) {
            Integer candidateId = resolveCandidateId(user);
            if (candidateId != null) {
                savedJobsCount = jobService.countSavedJobsByCandidate(candidateId);
            }
        }

        long unreadNotifications = notificationService.countUnread(user.getId());

        return new HeaderUserContext(
            user.getId(),
            displayName,
            user.getEmail(),
            normalizeAvatarPath(user.getAvatarPath()),
            roleKey,
            resolveDashboardUrl(roleKey),
            savedJobsCount,
            unreadNotifications
        );
    }

    private Integer resolveCandidateId(User user) {
        Candidate candidate = user.getCandidateProfile();
        if (candidate != null) {
            return candidate.getId();
        }
        return candidateRepository.findByUserId(user.getId())
            .map(Candidate::getId)
            .orElse(null);
    }

    private String normalizeAvatarPath(String avatarPath) {
        if (!StringUtils.hasText(avatarPath)) {
            return null;
        }
        String trimmed = avatarPath.trim();
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    private String resolveDashboardUrl(String roleKey) {
        return switch (roleKey) {
            case "admin" -> "/admin/dashboard";
            case "employer" -> "/employer/dashboard";
            case "candidate" -> "/candidate/dashboard";
            default -> "/dashboard";
        };
    }

    public record HeaderUserContext(
        Integer userId,
        String displayName,
        String email,
        String avatarUrl,
        String roleKey,
        String dashboardUrl,
        long savedJobsCount,
        long unreadNotifications
    ) {
        public boolean isCandidate() {
            return "candidate".equals(roleKey);
        }

        public boolean isEmployer() {
            return "employer".equals(roleKey);
        }

        public boolean isAdmin() {
            return "admin".equals(roleKey);
        }

        public String initials() {
            if (!StringUtils.hasText(displayName)) {
                return "U";
            }
            return displayName.trim().substring(0, 1).toUpperCase(Locale.ROOT);
        }

        public boolean hasUnreadNotifications() {
            return unreadNotifications > 0;
        }
    }
}
