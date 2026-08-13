package com.likelion.firstbite.firstbiteserver.auth;

import com.likelion.firstbite.firstbiteserver.auth.phone.PhoneVerificationRepository;
import com.likelion.firstbite.firstbiteserver.auth.sms.SmsSender;
import com.likelion.firstbite.firstbiteserver.auth.token.RefreshTokenRepository;
import com.likelion.firstbite.firstbiteserver.auth.login.LoginAttemptRepository;
import com.likelion.firstbite.firstbiteserver.member.repository.MemberRepository;
import com.likelion.firstbite.firstbiteserver.member.repository.TermsAgreementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.security.refresh-cookie-secure=true")
@AutoConfigureMockMvc
class AuthControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MemberRepository memberRepository;
    @Autowired TermsAgreementRepository termsAgreementRepository;
    @Autowired PhoneVerificationRepository phoneVerificationRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired LoginAttemptRepository loginAttemptRepository;
    @Autowired CapturingSmsSender smsSender;

    @BeforeEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        loginAttemptRepository.deleteAll();
        termsAgreementRepository.deleteAll();
        memberRepository.deleteAll();
        phoneVerificationRepository.deleteAll();
        smsSender.code.set(null);
    }

    @Test
    void logsInWithAccessTokenAndHttpOnlyRefreshCookieThenLogsOut() throws Exception {
        String verificationToken = verifyPhone("010-1234-5678");
        signUp(validRequest(verificationToken, "user@example.com"));

        var loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", " USER@Example.COM ", "password", "Example!234"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("지훈"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(3600))
                .andReturn();

        String body = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(body).get("data").get("accessToken").asText();
        jakarta.servlet.http.Cookie refreshCookie = loginResult.getResponse().getCookie("refreshToken");
        assertThat(refreshCookie).isNotNull();
        assertThat(refreshCookie.isHttpOnly()).isTrue();
        assertThat(refreshCookie.getSecure()).isTrue();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(refreshCookie))
                .andExpect(status().isNoContent());
        assertThat(refreshTokenRepository.findAll().get(0).getRevokedAt()).isNotNull();
    }

    @Test
    void returnsSameCredentialErrorForUnknownEmailAndWrongPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "unknown@example.com", "password", "Wrong!234"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_INVALID_CREDENTIALS"));
    }

    @Test
    void logoutRequiresValidAccessToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    void rotatesRefreshTokenAndIssuesNewAccessToken() throws Exception {
        jakarta.servlet.http.Cookie originalCookie = registerAndLogin("010-1234-5678", "user@example.com");

        var refreshResult = mockMvc.perform(post("/api/v1/auth/refresh").cookie(originalCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.expiresIn").value(3600))
                .andReturn();

        jakarta.servlet.http.Cookie rotatedCookie = refreshResult.getResponse().getCookie("refreshToken");
        assertThat(rotatedCookie).isNotNull();
        assertThat(rotatedCookie.getValue()).isNotEqualTo(originalCookie.getValue());
        assertThat(rotatedCookie.isHttpOnly()).isTrue();
        assertThat(refreshTokenRepository.findAll()).hasSize(2);
        assertThat(refreshTokenRepository.findAll().stream().filter(token -> token.getRevokedAt() == null)).hasSize(1);
    }

    @Test
    void detectsReusedRefreshTokenAndRevokesActiveSession() throws Exception {
        jakarta.servlet.http.Cookie originalCookie = registerAndLogin("010-1234-5678", "user@example.com");
        mockMvc.perform(post("/api/v1/auth/refresh").cookie(originalCookie))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(originalCookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("AUTH_REFRESH_REUSED"));

        assertThat(refreshTokenRepository.findAll())
                .allMatch(token -> token.getRevokedAt() != null);
    }

    @Test
    void rejectsRefreshWithoutCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_REFRESH_INVALID"));
    }

    @Test
    void authenticatedUserCanReadOwnAccount() throws Exception {
        LoginTokens tokens = registerAndLoginTokens("010-1234-5678", "user@example.com");

        mockMvc.perform(get("/api/v1/accounts/me")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("지훈"))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.phoneNumber").value("010-****-5678"))
                .andExpect(jsonPath("$.data.birthDate").value("2000-01-01"))
                .andExpect(jsonPath("$.data.marketingAgreed").value(false))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.personalization.enabled").value(false))
                .andExpect(jsonPath("$.data.personalization.feedbackCount").value(0));
    }

    @Test
    void accountMeRequiresAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    void deletesAccountRevokesAllSessionsAndBlocksExistingAccessToken() throws Exception {
        LoginTokens firstSession = registerAndLoginTokens("010-1234-5678", "user@example.com");
        var secondLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "user@example.com", "password", "Example!234"))))
                .andExpect(status().isOk()).andReturn();
        String secondAccessToken = objectMapper.readTree(secondLogin.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

        var deleteResult = mockMvc.perform(delete("/api/v1/accounts/me")
                        .header("Authorization", "Bearer " + secondAccessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "password", "Example!234", "confirmText", "탈퇴"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deletedAt").isNotEmpty())
                .andReturn();

        assertThat(deleteResult.getResponse().getCookie("refreshToken").getMaxAge()).isZero();
        assertThat(memberRepository.findAll().get(0).getStatus().name()).isEqualTo("DELETED");
        assertThat(memberRepository.findAll().get(0).getDeletedAt()).isNotNull();
        assertThat(refreshTokenRepository.findAll()).allMatch(token -> token.getRevokedAt() != null);

        mockMvc.perform(get("/api/v1/accounts/me")
                        .header("Authorization", "Bearer " + firstSession.accessToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    void rejectsDeleteWhenConfirmationTextDoesNotMatch() throws Exception {
        LoginTokens tokens = registerAndLoginTokens("010-1234-5678", "user@example.com");
        mockMvc.perform(delete("/api/v1/accounts/me")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "password", "Example!234", "confirmText", "삭제"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_CONFIRMATION_INVALID"));
    }

    @Test
    void rejectsDeleteWhenPasswordIsWrong() throws Exception {
        LoginTokens tokens = registerAndLoginTokens("010-1234-5678", "user@example.com");
        mockMvc.perform(delete("/api/v1/accounts/me")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "password", "Wrong!234", "confirmText", "탈퇴"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTH_UNAUTHORIZED"));
    }

    @Test
    void verifiesPhoneAndSignsUpAccordingToContract() throws Exception {
        String token = verifyPhone("010-1234-5678");
        Map<String, Object> request = validRequest(token, "  USER@Example.COM  ");

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.name").value("지훈"))
                .andExpect(jsonPath("$.data.phoneNumber").value("010-****-5678"))
                .andExpect(jsonPath("$.data.marketingAgreed").value(false));

        assertThat(memberRepository.findAll()).hasSize(1);
        assertThat(termsAgreementRepository.findAll()).hasSize(3);
        assertThat(memberRepository.findAll().get(0).getPasswordHash()).startsWith("$2");
    }

    @Test
    void rejectsPasswordWithOnlyOneCharacterCategory() throws Exception {
        Map<String, Object> request = validRequest("unused-token", "user@example.com");
        request.put("password", "abcdefgh");
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("AUTH_PASSWORD_POLICY_VIOLATION"));
    }

    @Test
    void rejectsUserUnderFourteen() throws Exception {
        Map<String, Object> request = validRequest("unused-token", "user@example.com");
        request.put("birthDate", LocalDate.now().minusYears(14).plusDays(1).toString());
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("AUTH_BIRTH_DATE_INVALID"));
    }

    @Test
    void verificationTokenCanBeUsedOnlyOnce() throws Exception {
        String token = verifyPhone("010-1234-5678");
        signUp(validRequest(token, "first@example.com"));
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(token, "second@example.com"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PHONE_VERIFICATION_TOKEN_USED"));
    }

    private String verifyPhone(String phoneNumber) throws Exception {
        mockMvc.perform(post("/api/v1/auth/phone-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phoneNumber", phoneNumber))))
                .andExpect(status().isCreated());
        String response = mockMvc.perform(post("/api/v1/auth/phone-verifications/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("phoneNumber", phoneNumber, "code", smsSender.code.get()))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(response);
        return json.get("data").get("verificationToken").asText();
    }

    private jakarta.servlet.http.Cookie registerAndLogin(String phoneNumber, String email) throws Exception {
        return registerAndLoginTokens(phoneNumber, email).refreshCookie();
    }

    private LoginTokens registerAndLoginTokens(String phoneNumber, String email) throws Exception {
        String verificationToken = verifyPhone(phoneNumber);
        signUp(validRequest(verificationToken, email));
        var result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email, "password", "Example!234"))))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
        return new LoginTokens(accessToken, result.getResponse().getCookie("refreshToken"));
    }

    private record LoginTokens(String accessToken, jakarta.servlet.http.Cookie refreshCookie) {}

    private void signUp(Map<String, Object> request) throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    private Map<String, Object> validRequest(String token, String email) {
        Map<String, Object> request = new HashMap<>();
        request.put("verificationToken", token);
        request.put("name", "지훈");
        request.put("email", email);
        request.put("password", "Example!234");
        request.put("birthDate", "2000-01-01");
        request.put("termsAgreed", true);
        request.put("privacyAgreed", true);
        request.put("marketingAgreed", false);
        return request;
    }

    @TestConfiguration
    static class SmsTestConfig {
        @Bean @Primary CapturingSmsSender capturingSmsSender() { return new CapturingSmsSender(); }
    }

    static class CapturingSmsSender implements SmsSender {
        final AtomicReference<String> code = new AtomicReference<>();
        @Override public void sendVerificationCode(String phoneNumber, String code) { this.code.set(code); }
    }
}
