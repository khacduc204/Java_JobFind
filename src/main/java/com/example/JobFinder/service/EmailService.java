package com.example.JobFinder.service;

import com.example.JobFinder.model.Application;
import com.example.JobFinder.model.Candidate;
import com.example.JobFinder.model.Job;
import com.example.JobFinder.model.User;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public void sendNewApplicationToEmployer(Application application) {
        if (application == null) return;
        Job job = application.getJob();
        Candidate candidate = application.getCandidate();
        User employerUser = job != null && job.getEmployer() != null ? job.getEmployer().getUser() : null;
        if (employerUser == null || !StringUtils.hasText(employerUser.getEmail())) {
            return;
        }

        String subject = "[JobFinder] Ứng viên mới cho vị trí " + safe(job != null ? job.getTitle() : "");
        StringBuilder body = new StringBuilder();
        body.append("Chào bạn,\n\n")
            .append("Bạn vừa nhận được một hồ sơ ứng tuyển mới.\n")
            .append("Vị trí: ").append(safe(job != null ? job.getTitle() : "")).append("\n");
        if (candidate != null && candidate.getUser() != null) {
            body.append("Ứng viên: ").append(safe(candidate.getUser().getName())).append("\n")
                .append("Email: ").append(safe(candidate.getUser().getEmail())).append("\n")
                .append("SĐT: ").append(safe(candidate.getUser().getPhone())).append("\n");
        }
        Optional.ofNullable(application.getCoverLetter())
            .filter(StringUtils::hasText)
            .ifPresent(letter -> body.append("\nThư giới thiệu:\n").append(letter).append("\n"));
        body.append("\nHãy đăng nhập JobFinder để xem chi tiết và phản hồi ứng viên.\n\nJobFinder Team");

        send(employerUser.getEmail(), subject, body.toString());
    }

    public void sendStatusUpdateToCandidate(Application application, @Nullable String note) {
        if (application == null || application.getCandidate() == null || application.getCandidate().getUser() == null) {
            return;
        }
        User candidateUser = application.getCandidate().getUser();
        if (!StringUtils.hasText(candidateUser.getEmail())) {
            return;
        }

        Job job = application.getJob();
        String status = safe(application.getStatus());
        String subject = "[JobFinder] Trạng thái hồ sơ của bạn: " + status;

        StringBuilder body = new StringBuilder();
        body.append("Chào ").append(safe(candidateUser.getName())).append(",\n\n")
            .append("Nhà tuyển dụng đã cập nhật hồ sơ của bạn.\n")
            .append("Vị trí: ").append(safe(job != null ? job.getTitle() : "")).append("\n")
            .append("Trạng thái mới: ").append(status).append("\n");

        if (StringUtils.hasText(note)) {
            body.append("\nGhi chú từ nhà tuyển dụng:\n").append(note).append("\n");
        }

        body.append("\nVui lòng đăng nhập JobFinder để xem chi tiết.\n\nJobFinder Team");

        send(candidateUser.getEmail(), subject, body.toString());
    }

    private void send(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (MailException ignored) {
            log.warn("Gửi email thất bại tới {} với tiêu đề '{}': {}", to, subject, ignored.getMessage());
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
