package com.example.JobFinder.controller;

import com.example.JobFinder.dto.RegistrationRequest;
import com.example.JobFinder.exception.BadRequestException;
import com.example.JobFinder.exception.ResourceAlreadyExistsException;
import com.example.JobFinder.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

	private final UserService userService;
	private final AuthenticationManager authenticationManager;

	@ModelAttribute("registrationRequest")
	public RegistrationRequest registrationRequest() {
		return new RegistrationRequest("", "", "", 3);
	}

	@ModelAttribute("availableRoles")
	public List<RoleDisplay> availableRoles() {
		try {
			return userService.findSelfRegisterRoles().stream()
				.map(role -> new RoleDisplay(
					role.getId(),
					role.getName(),
					getRoleDisplayName(role.getName())
				))
				.toList();
		} catch (Exception ex) {
			log.error("Không tải được danh sách vai trò tự đăng ký", ex);
			return List.of();
		}
	}

	private String getRoleDisplayName(String roleName) {
		if (roleName == null) return "";
		return switch (roleName.toLowerCase()) {
			case "employer" -> "Nhà tuyển dụng";
			case "admin" -> "Quản trị viên";
			case "candidate" -> "Ứng viên tìm việc";
			default -> roleName;
		};
	}

	public record RoleDisplay(Integer id, String name, String displayName) {}

	@GetMapping("/login")
	public String showLoginPage(@RequestParam(value = "error", required = false) String hasError,
								@RequestParam(value = "logout", required = false) String logout,
								HttpServletRequest request,
								Model model) {
		if (hasError != null) {
			Object exception = request.getSession().getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
			String errorMessage = exception instanceof Exception ex
				? ex.getMessage()
				: "Email hoặc mật khẩu không đúng.";
			model.addAttribute("authError", errorMessage);
		}
		if (logout != null) {
			model.addAttribute("logoutMessage", "Bạn đã đăng xuất thành công.");
		}
		return "frontend/auth/login";
	}

	@GetMapping("/register")
	public String showRegisterPage(Model model) {
		if (availableRoles().isEmpty()) {
			model.addAttribute("formError", "Chưa có dữ liệu vai trò. Vui lòng import roles/permissions trước khi đăng ký.");
		}
		return "frontend/auth/register";
	}

	@PostMapping("/register")
	public String handleRegister(@Valid @ModelAttribute("registrationRequest") RegistrationRequest registrationRequest,
								 BindingResult bindingResult,
								 Model model) {
		if (bindingResult.hasErrors()) {
			return "frontend/auth/register";
		}

		try {
			var user = userService.registerUser(registrationRequest);
			authenticateUser(registrationRequest.email(), registrationRequest.password());
			
			// Redirect based on role
			String roleName = user.getRole().getName().toLowerCase();
			String redirectUrl = switch (roleName) {
				case "admin" -> "redirect:/admin/dashboard";
				case "employer" -> "redirect:/employer/dashboard";
				case "candidate" -> "redirect:/candidate/dashboard";
				default -> "redirect:/dashboard";
			};
			return redirectUrl;
		} catch (ResourceAlreadyExistsException | BadRequestException ex) {
			model.addAttribute("formError", ex.getMessage());
			return "frontend/auth/register";
		}
	}

	private void authenticateUser(String email, String rawPassword) {
		Authentication authentication = authenticationManager.authenticate(
			new UsernamePasswordAuthenticationToken(email, rawPassword)
		);
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}
}
