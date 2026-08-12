package com.likelion.firstbite.firstbiteserver.auth;

import com.likelion.firstbite.firstbiteserver.auth.phone.PhoneVerificationRepository;
import com.likelion.firstbite.firstbiteserver.auth.sms.SmsSender;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired MemberRepository memberRepository;
    @Autowired TermsAgreementRepository termsAgreementRepository;
    @Autowired PhoneVerificationRepository phoneVerificationRepository;
    @Autowired CapturingSmsSender smsSender;

    @BeforeEach
    void cleanUp() {
        termsAgreementRepository.deleteAll();
        memberRepository.deleteAll();
        phoneVerificationRepository.deleteAll();
        smsSender.code.set(null);
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
