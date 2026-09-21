package com.smartshop.features.auth.service;

import com.smartshop.features.auth.dto.AuthResponse;
import com.smartshop.features.auth.dto.AuthResponse.BranchRoleResponse;
import com.smartshop.features.auth.dto.AuthResult;
import com.smartshop.features.auth.dto.ForgotPasswordRequest;
import com.smartshop.features.auth.dto.LoginRequest;
import com.smartshop.features.auth.dto.RegisterRequest;
import com.smartshop.features.auth.dto.ResetPasswordRequest;
import com.smartshop.features.auth.entity.EmailVerificationToken;
import com.smartshop.features.auth.entity.PasswordResetToken;
import com.smartshop.features.auth.repository.EmailVerificationTokenRepository;
import com.smartshop.features.auth.repository.PasswordResetTokenRepository;
import com.smartshop.features.user.entity.User;
import com.smartshop.features.user.repository.UserRepository;
import com.smartshop.features.userBranchRole.entity.UserBranchRole;
import com.smartshop.features.userBranchRole.repository.UserBranchRoleRepository;
import com.smartshop.security.BranchRoleGrant;
import com.smartshop.security.JwtTokenProvider;
import com.smartshop.security.RateLimiterService;
import com.smartshop.security.RefreshTokenBlacklist;
import com.smartshop.shared.email.EmailService;
import com.smartshop.shared.exception.BadRequestException;
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
import java.util.Optional;
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
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

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
        user.setEmailVerified(false);
        User saved = userRepository.save(user);
        
        // Trigger welcome email and verification email asynchronously
        try {
            emailService.sendWelcomeEmail(saved.getEmail(), saved.getFirstName());
            sendVerificationToken(saved);
        } catch (Exception e) {
            log.warn("Failed to send welcome/verification email to {}: {}", saved.getEmail(), e.getMessage());
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
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is not active");
        }
        log.info("Token refreshed for user id: {}", userId);
        return buildAuthResult(user);
    }

    public void logout(String refreshToken) {
        log.info("Logging out user, blacklisting token");
        refreshTokenBlacklist.blacklist(refreshToken);
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        log.info("Processing forgot-password for email: {}", email);
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.info("Forgot password requested for non-existent email: {}", email);
            return; // Return silently for security
        }

        User user = userOpt.get();
        passwordResetTokenRepository.invalidateActiveTokensForUser(user);

        String token = UUID.randomUUID().toString().replace("-", "");
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(java.time.LocalDateTime.now().plusHours(2))
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);

        try {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), token);
        } catch (Exception e) {
            log.warn("Failed to send password reset email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.info("Processing password reset with token");
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken().trim())
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token"));

        if (resetToken.isUsed() || resetToken.isExpired()) {
            throw new BadRequestException("Password reset token has expired or has already been used");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        log.info("Password successfully reset for user: {}", user.getEmail());
    }

    @Transactional
    public void sendVerificationToken(User user) {
        emailVerificationTokenRepository.invalidateActiveTokensForUser(user);

        String token = UUID.randomUUID().toString().replace("-", "");
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .user(user)
                .token(token)
                .expiresAt(java.time.LocalDateTime.now().plusHours(24))
                .used(false)
                .build();
        emailVerificationTokenRepository.save(verificationToken);

        try {
            emailService.sendEmailVerificationEmail(user.getEmail(), user.getFullName(), token);
        } catch (Exception e) {
            log.warn("Failed to send verification email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    @Transactional
    public void verifyEmail(String token) {
        log.info("Verifying email with token");
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token.trim())
                .orElseThrow(() -> new BadRequestException("Invalid or expired email verification token"));

        if (verificationToken.isUsed() || verificationToken.isExpired()) {
            throw new BadRequestException("Email verification token has expired or has already been used");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        verificationToken.setUsed(true);
        emailVerificationTokenRepository.save(verificationToken);
        log.info("Email successfully verified for user: {}", user.getEmail());
    }

    @Transactional
    public void resendVerification(String email) {
        String cleanEmail = email.trim().toLowerCase();
        log.info("Resending verification email for: {}", cleanEmail);
        User user = userRepository.findByEmail(cleanEmail)
                .orElseThrow(() -> new BadRequestException("User with email " + cleanEmail + " not found"));

        if (user.isEmailVerified()) {
            throw new BadRequestException("Email address is already verified");
        }

        sendVerificationToken(user);
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
                .emailVerified(user.isEmailVerified())
                .build();

        return new AuthResult(response, refreshToken);
    }
}