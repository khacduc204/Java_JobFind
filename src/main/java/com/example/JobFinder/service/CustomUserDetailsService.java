package com.example.JobFinder.service;

import com.example.JobFinder.model.Permission;
import com.example.JobFinder.model.Role;
import com.example.JobFinder.model.User;
import com.example.JobFinder.repository.UserRepository;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Attempting to load user: {}", username);
        User user = userRepository.findByEmailIgnoreCase(username)
            .orElseThrow(() -> {
                log.error("User not found: {}", username);
                return new UsernameNotFoundException("Email hoặc mật khẩu không đúng");
            });
        
        log.debug("Found user: {}, role: {}, password hash length: {}", 
            user.getEmail(), 
            user.getRole() != null ? user.getRole().getName() : "null",
            user.getPasswordHash() != null ? user.getPasswordHash().length() : 0);

        Collection<? extends GrantedAuthority> authorities = buildAuthorities(user);
        log.debug("User {} granted authorities: {}", user.getEmail(), authorities);

        return org.springframework.security.core.userdetails.User
            .withUsername(user.getEmail())
            .password(user.getPasswordHash())
            .authorities(authorities)
            .accountExpired(false)
            .accountLocked(false)
            .credentialsExpired(false)
            .disabled(false)
            .build();
    }

    private Collection<? extends GrantedAuthority> buildAuthorities(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        Role role = user.getRole();
        if (role != null && role.getName() != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase()));
            Optional.ofNullable(role.getPermissions())
                .ifPresent(perms -> perms.stream()
                    .map(Permission::getName)
                    .filter(name -> name != null && !name.isBlank())
                    .map(SimpleGrantedAuthority::new)
                    .forEach(authorities::add));
        }
        return authorities;
    }
}
