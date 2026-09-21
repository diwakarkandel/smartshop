package com.smartshop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.smartshop.features.user.repository.UserRepository userRepository;

    @Autowired
    private com.smartshop.features.auth.repository.PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private com.smartshop.features.auth.repository.EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Test
    void registerLoginRefreshAndLogout() throws Exception {
        String email = "auth.user@test.com";
        String password = "secret12345";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Auth","lastName":"User","email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(cookie().exists("refresh_token"));

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(cookie().exists("refresh_token"))
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
        String refreshToken = loginResult.getResponse().getCookie("refresh_token").getValue();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", refreshToken)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh_token", refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateRegistrationIsRejected() throws Exception {
        String email = "dup.user@test.com";
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Dup","lastName":"User","email":"%s","password":"secret12345"}
                                """.formatted(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Dup","lastName":"User","email":"%s","password":"secret12345"}
                                """.formatted(email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void loginWithWrongPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@test.com","password":"wrongpass1"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void forgotPasswordAndResetPasswordFlow() throws Exception {
        String email = "reset.test@example.com";
        String oldPassword = "oldPassword123";
        String newPassword = "newPassword456";

        // 1. Register user
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Reset","lastName":"Tester","email":"%s","password":"%s"}
                                """.formatted(email, oldPassword)))
                .andExpect(status().isCreated());

        // 2. Request password reset
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 3. Retrieve token from repository
        var user = userRepository.findByEmail(email).orElseThrow();
        var tokens = passwordResetTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()) && !t.isUsed())
                .toList();
        org.junit.jupiter.api.Assertions.assertFalse(tokens.isEmpty(), "Reset token should be created");
        String resetToken = tokens.get(tokens.size() - 1).getToken();

        // 4. Reset password
        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"%s"}
                                """.formatted(resetToken, newPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 5. Old password no longer works
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, oldPassword)))
                .andExpect(status().isUnauthorized());

        // 6. New password works successfully!
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, newPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void emailVerificationFlow() throws Exception {
        String email = "verify.test@example.com";
        String password = "secretPassword123";

        // 1. Register user
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Verify","lastName":"User","email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail(email).orElseThrow();
        org.junit.jupiter.api.Assertions.assertFalse(user.isEmailVerified(), "New user should not be verified yet");

        // 2. Retrieve verification token
        var tokens = emailVerificationTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getId().equals(user.getId()) && !t.isUsed())
                .toList();
        org.junit.jupiter.api.Assertions.assertFalse(tokens.isEmpty(), "Verification token should exist");
        String verifyToken = tokens.get(tokens.size() - 1).getToken();

        // 3. Verify email endpoint
        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s"}
                                """.formatted(verifyToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 4. Confirm user entity updated
        var updatedUser = userRepository.findByEmail(email).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(updatedUser.isEmailVerified(), "User email should now be verified");
    }
}