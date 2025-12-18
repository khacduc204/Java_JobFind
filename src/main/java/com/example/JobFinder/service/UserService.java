package com.example.JobFinder.service;

import com.example.JobFinder.dto.RegistrationRequest;
import com.example.JobFinder.exception.BadRequestException;
import com.example.JobFinder.exception.ResourceAlreadyExistsException;
import com.example.JobFinder.model.Candidate;
import com.example.JobFinder.model.Employer;
import com.example.JobFinder.model.Role;
import com.example.JobFinder.model.User;
import com.example.JobFinder.repository.CandidateRepository;
import com.example.JobFinder.repository.EmployerRepository;
import com.example.JobFinder.repository.RoleRepository;
import com.example.JobFinder.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Set<String> SELF_REGISTERABLE_ROLES = Set.of("candidate", "employer");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CandidateRepository candidateRepository;
    private final EmployerRepository employerRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<Role> findSelfRegisterRoles() {
        return roleRepository.findAll().stream()
            .filter(role -> role.getName() != null)
            .filter(role -> SELF_REGISTERABLE_ROLES.contains(role.getName().toLowerCase(Locale.ROOT)))
            .toList();
    }

    @Transactional
    public User registerUser(RegistrationRequest request) {
        validateRegistrationRequest(request);

        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResourceAlreadyExistsException("Email đã được đăng ký. Vui lòng sử dụng email khác hoặc đăng nhập.");
        }

        Role role = roleRepository.findById(request.roleId())
            .orElseThrow(() -> new BadRequestException("Vai trò không hợp lệ"));
        if (!SELF_REGISTERABLE_ROLES.contains(role.getName().toLowerCase(Locale.ROOT))) {
            throw new BadRequestException("Vai trò không được phép tự đăng ký");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setName(request.name().trim());
        String encodedPassword = passwordEncoder.encode(request.password());
        user.setPasswordHash(encodedPassword);
        user.setRole(role);

        User savedUser = userRepository.save(user);
        createProfileForRole(savedUser, role.getName());
        return savedUser;
    }

    private void validateRegistrationRequest(RegistrationRequest request) {
        if (!StringUtils.hasText(request.email()) || !StringUtils.hasText(request.password())) {
            throw new BadRequestException("Email và mật khẩu là bắt buộc");
        }
        if (request.password().length() < 6) {
            throw new BadRequestException("Mật khẩu phải có tối thiểu 6 ký tự");
        }
        if (!StringUtils.hasText(request.name())) {
            throw new BadRequestException("Họ tên không được để trống");
        }
        if (request.roleId() == null) {
            throw new BadRequestException("Vui lòng chọn vai trò");
        }
    }

    private void createProfileForRole(User user, String roleName) {
        if (roleName == null) {
            return;
        }
        String normalizedRole = roleName.toLowerCase(Locale.ROOT);
        if ("candidate".equals(normalizedRole)) {
            Candidate candidate = new Candidate();
            candidate.setUser(user);
            candidateRepository.save(candidate);
        } else if ("employer".equals(normalizedRole)) {
            Employer employer = new Employer();
            employer.setUser(user);
            employer.setCompanyName(user.getName() != null ? user.getName() : user.getEmail());
            employerRepository.save(employer);
        }
    }
}
