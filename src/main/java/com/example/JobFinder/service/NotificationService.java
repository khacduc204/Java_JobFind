package com.example.JobFinder.service;

import com.example.JobFinder.model.Application;
import com.example.JobFinder.model.Job;
import com.example.JobFinder.model.Notification;
import com.example.JobFinder.model.User;
import com.example.JobFinder.repository.NotificationRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final NotificationRepository notificationRepository;

    @Transactional
    public void notifyApplicationViewed(Application application) {
        User candidateUser = getCandidateUser(application);
        if (candidateUser == null) {
            return;
        }

        String employer = resolveEmployer(application);
        String jobTitle = resolveJobTitle(application);

        Notification notification = Notification.builder()
            .user(candidateUser)
            .title("Hồ sơ của bạn đã được xem")
            .message(employer + " vừa xem hồ sơ của bạn cho vị trí " + jobTitle + ".")
            .iconPath("fa-eye")
            .read(false)
            .build();

        notificationRepository.save(notification);
    }

    @Transactional
    public void notifyApplicationStatusChanged(Application application, String note) {
        User candidateUser = getCandidateUser(application);
        if (candidateUser == null) {
            return;
        }

        StatusPayload payload = buildStatusPayload(application, note);

        Notification notification = Notification.builder()
            .user(candidateUser)
            .title(payload.title())
            .message(payload.message())
            .iconPath(payload.icon())
            .read(false)
            .build();

        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public long countUnread(Integer userId) {
        if (userId == null) {
            return 0;
        }
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional(readOnly = true)
    public List<NotificationView> getRecentNotifications(Integer userId, int limit) {
        if (userId == null) {
            return Collections.emptyList();
        }
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(
            userId,
            PageRequest.of(0, Math.max(limit, 1))
        );
        return notifications.stream()
            .map(this::toView)
            .collect(Collectors.toList());
    }

    @Transactional
    public void markAllAsRead(Integer userId) {
        if (userId == null) {
            return;
        }
        notificationRepository.markAllAsRead(userId);
    }

    private NotificationView toView(Notification notification) {
        String message = StringUtils.hasText(notification.getMessage()) ? notification.getMessage() : "";
        String title = StringUtils.hasText(notification.getTitle()) ? notification.getTitle() : "Thông báo";
        String icon = StringUtils.hasText(notification.getIconPath()) ? notification.getIconPath() : "fa-bell";
        String relativeTime = formatRelativeTime(notification.getCreatedAt());

        return new NotificationView(title, message, relativeTime, notification.isRead(), icon,
            notification.getCreatedAt() != null ? notification.getCreatedAt().format(DATE_FORMAT) : "");
    }

    private String formatRelativeTime(LocalDateTime createdAt) {
        if (createdAt == null) {
            return "";
        }
        Duration duration = Duration.between(createdAt, LocalDateTime.now());
        if (duration.toMinutes() < 1) {
            return "Vừa xong";
        }
        if (duration.toMinutes() < 60) {
            return duration.toMinutes() + " phút trước";
        }
        if (duration.toHours() < 24) {
            return duration.toHours() + " giờ trước";
        }
        if (duration.toDays() < 7) {
            return duration.toDays() + " ngày trước";
        }
        return createdAt.format(DATE_FORMAT);
    }

    public record NotificationView(
        String title,
        String message,
        String relativeTime,
        boolean read,
        String icon,
        String exactTime
    ) {
    }

    private User getCandidateUser(Application application) {
        if (application == null || application.getCandidate() == null) {
            return null;
        }
        return application.getCandidate().getUser();
    }

    private String resolveEmployer(Application application) {
        if (application == null) {
            return "Nhà tuyển dụng";
        }
        Job job = application.getJob();
        if (job == null || job.getEmployer() == null || !StringUtils.hasText(job.getEmployer().getCompanyName())) {
            return "Nhà tuyển dụng";
        }
        return job.getEmployer().getCompanyName();
    }

    private String resolveJobTitle(Application application) {
        if (application == null || application.getJob() == null || !StringUtils.hasText(application.getJob().getTitle())) {
            return "một vị trí đang tuyển";
        }
        return application.getJob().getTitle();
    }

    private StatusPayload buildStatusPayload(Application application, String note) {
        String status = application != null && StringUtils.hasText(application.getStatus())
            ? application.getStatus().toLowerCase()
            : "applied";
        String employer = resolveEmployer(application);
        String jobTitle = resolveJobTitle(application);
        String sanitizedNote = StringUtils.hasText(note) ? note.trim() : null;

        String title;
        String message;
        String icon;

        switch (status) {
            case "shortlisted" -> {
                title = "Bạn được mời phỏng vấn";
                message = employer + " đã đưa hồ sơ của bạn vào vòng phỏng vấn cho vị trí " + jobTitle + ".";
                icon = "fa-calendar-check";
            }
            case "rejected" -> {
                title = "Hồ sơ chưa phù hợp";
                message = employer + " rất tiếc chưa thể tiếp tục với ứng tuyển " + jobTitle + ".";
                icon = "fa-xmark";
            }
            case "hired" -> {
                title = "Chúc mừng! Bạn đã được nhận";
                message = employer + " muốn chào đón bạn cho vị trí " + jobTitle + ".";
                icon = "fa-trophy";
            }
            case "withdrawn" -> {
                title = "Ứng tuyển đã bị hủy";
                message = "Hồ sơ cho vị trí " + jobTitle + " đã bị hủy trạng thái.";
                icon = "fa-circle-minus";
            }
            case "viewed" -> {
                title = "Hồ sơ đã được xem";
                message = employer + " tiếp tục xử lý hồ sơ của bạn cho vị trí " + jobTitle + ".";
                icon = "fa-eye";
            }
            default -> {
                title = "Hồ sơ được cập nhật";
                message = employer + " đã cập nhật trạng thái hồ sơ của bạn cho vị trí " + jobTitle + ".";
                icon = "fa-briefcase";
            }
        }

        if (sanitizedNote != null) {
            message += " Ghi chú: " + sanitizedNote;
        }

        return new StatusPayload(title, message, icon);
    }

    private record StatusPayload(String title, String message, String icon) {
    }
}
