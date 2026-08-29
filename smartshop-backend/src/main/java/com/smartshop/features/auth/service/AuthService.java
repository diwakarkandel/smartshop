package com.smartshop.features.auth.service;

import com.smartshop.features.auth.dto.AuthResponse;
import com.smartshop.features.auth.dto.AuthResponse.BranchRoleResponse;
import com.smartshop.features.auth.dto.AuthResult;
import com.smartshop.features.auth.dto.LoginRequest;
import com.smartshop.features.auth.dto.RegisterRequest;
import com.smartshop.features.user.entity.User;
import com.smartshop.features.user.repository.UserRepository;
import com.smartshop.features.userBranchRole.entity.UserBranchRole;
import com.smartshop.features.userBranchRole.repository.UserBranchRoleRepository;
import com.smartshop.security.BranchRoleGrant;
import com.smartshop.security.JwtTokenProvider;
import com.smartshop.security.RateLimiterService;
import com.smartshop.security.RefreshTokenBlacklist;
import com.smartshop.shared.email.EmailService;
import com.smartshop.shared.exception.DuplicateResourceException;
import com.smartshop.shared.exception.UnauthorizedException;
import com.smartshop.shared.enumeration.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final UserBranchRoleRepository userBranchRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RateLimiterService rateLimiterService;
    private final RefreshTokenBlacklist refreshTokenBlacklist;
    private final EmailService emailService;

    @Transactional
    public AuthResult register(RegisterRequest request) {
        log.info("Registering user with email: {}", request.getEmail());
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("An account with email " + request.getEmail() + " already exists");
        }

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setStatus(UserStatus.ACTIVE);
        User saved = userRepository.save(user);
        
        // Trigger welcome email asynchronously so it doesn't block the API response
        try {
            emailService.sendWelcomeEmail(saved.getEmail(), saved.getFirstName());
        } catch (Exception e) {
            log.warn("Failed to send welcome email to {}: {}", saved.getEmail(), e.getMessage());
        }

        log.info("User registered successfully with id: {}", saved.getId());

        return buildAuthResult(saved);
    }

    @Transactional(readOnly = true)
    public AuthResult login(LoginRequest request, String clientIp) {
        log.info("Attempting login for user: {} from ip: {}", request.getEmail(), clientIp);
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (user.getStatus() == UserStatus.BLOCKED) {
            log.warn("Blocked user {} tried to login", request.getEmail());
            throw new UnauthorizedException("Account blocked. Contact your administrator.");
        }
        if (user.getStatus() == UserStatus.SUSPENDED) {
            log.warn("Suspended user {} tried to login", request.getEmail());
            throw new UnauthorizedException("Account suspended. Contact your administrator.");
        }

        if (clientIp != null) {
            rateLimiterService.clear(clientIp);
        }
        log.info("User {} logged in successfully", request.getEmail());
        return buildAuthResult(user);
    }

    @Transactional(readOnly = true)
    public AuthResult refresh(String refreshToken) {
        log.info("Refreshing auth token");
        if (refreshToken == null || refreshToken.isBlank()
                || !jwtTokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
        if (refreshTokenBlacklist.isBlacklisted(refreshToken)) {
            log.warn("Attempt to use blacklisted refresh token");
            throw new UnauthorizedException("Refresh token has been revoked");
        }
        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        log.info("Token refreshed for user id: {}", userId);
        return buildAuthResult(user);
    }

    public void logout(String refreshToken) {
        log.info("Logging out user, blacklisting token");
        refreshTokenBlacklist.blacklist(refreshToken);
    }

    private AuthResult buildAuthResult(User user) {
        List<UserBranchRole> assignments = userBranchRoleRepository.findByUserIdAndIsActiveTrue(user.getId());
        List<String> roleNames = assignments.stream()
                .map(a -> a.getRole().getName())
                .distinct()
                .toList();
        List<BranchRoleGrant> grants = assignments.stream()
                .map(a -> new BranchRoleGrant(
                        a.getBranch() == null ? null : a.getBranch().getId(),
                        a.getShop() == null ? null : a.getShop().getId(),
                        a.getRole().getName()))
                .toList();
        List<BranchRoleResponse> branchRoles = assignments.stream()
                .map(a -> BranchRoleResponse.builder()
                        .branchId(a.getBranch() == null ? null : a.getBranch().getId())
                        .branchName(a.getBranch() == null ? null : a.getBranch().getName())
                        .shopId(a.getShop() == null ? null : a.getShop().getId())
                        .role(a.getRole().getName())
                        .build())
                .toList();

        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), roleNames, grants);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        AuthResponse response = AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roles(roleNames)
                .branchRoles(branchRoles)
                .profileImageUrl(user.getProfileImageUrl())
                .build();

        return new AuthResult(response, refreshToken);
    }
}