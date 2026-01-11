package com.example.JobFinder.controller;

import com.example.JobFinder.service.HeaderContextService;
import com.example.JobFinder.service.HeaderContextService.HeaderUserContext;
import com.example.JobFinder.service.HomeService;
import com.example.JobFinder.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;
import java.util.Map;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalLayoutAdvice {

    private final HomeService homeService;
    private final HeaderContextService headerContextService;
    private final NotificationService notificationService;

    @ModelAttribute("globalStats")
    public Map<String, Object> populateGlobalStats() {
        return homeService.getStatistics();
    }

    @ModelAttribute("navCategories")
    public List<Map<String, Object>> populateNavCategories() {
        return homeService.getTopCategories(6);
    }

    @ModelAttribute("headerUser")
    public HeaderUserContext populateHeaderUser(Authentication authentication) {
        return headerContextService.buildContext(authentication).orElse(null);
    }

    @ModelAttribute("headerNotifications")
    public List<NotificationService.NotificationView> populateHeaderNotifications(
            @ModelAttribute("headerUser") HeaderUserContext headerUser) {
        if (headerUser == null) {
            return List.of();
        }
        return notificationService.getRecentNotifications(headerUser.userId(), 5);
    }
}
