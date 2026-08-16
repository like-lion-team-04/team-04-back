package com.likelion.firstbite.firstbiteserver.recognition;

import com.likelion.firstbite.firstbiteserver.auth.token.JwtTokenService;
import com.likelion.firstbite.firstbiteserver.member.domain.Member;
import com.likelion.firstbite.firstbiteserver.member.repository.MemberRepository;
import com.likelion.firstbite.firstbiteserver.recognition.domain.ImageType;
import com.likelion.firstbite.firstbiteserver.recognition.domain.RecognitionStatus;
import com.likelion.firstbite.firstbiteserver.recognition.dto.RecognitionAcceptedResponse;
import com.likelion.firstbite.firstbiteserver.recognition.service.RecognitionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecognitionUploadControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired MemberRepository memberRepository;
    @Autowired JwtTokenService jwtTokenService;
    @MockitoBean RecognitionService recognitionService;

    private UUID memberId;
    private String token;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
        Member member = memberRepository.save(Member.create("upload@example.com", "password-hash", "테스터",
                LocalDate.of(2000, 1, 1), "phone", "upload-phone-hash", false));
        memberId = member.getId();
        token = jwtTokenService.issue(memberId);
    }

    @Test
    void acceptsImageTypeAsPlainMultipartText() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        UUID recognitionId = UUID.randomUUID();
        UUID imageId = UUID.randomUUID();
        when(recognitionService.create(eq(memberId), eq(idempotencyKey), any(), eq(ImageType.MENU_BOARD)))
                .thenReturn(new RecognitionAcceptedResponse(recognitionId, imageId, RecognitionStatus.PROCESSING,
                        "/api/v1/recognitions/" + recognitionId, Instant.now()));

        MockMultipartFile image = new MockMultipartFile("image", "menu.jpg", "image/jpeg", new byte[]{1});
        mockMvc.perform(multipart("/api/v1/recognitions")
                        .file(image)
                        .param("imageType", "MENU_BOARD")
                        .header("Idempotency-Key", idempotencyKey)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));

        verify(recognitionService).create(eq(memberId), eq(idempotencyKey), any(), eq(ImageType.MENU_BOARD));
    }

    @Test
    void defaultsImageTypeToFoodPhoto() throws Exception {
        UUID idempotencyKey = UUID.randomUUID();
        UUID recognitionId = UUID.randomUUID();
        when(recognitionService.create(eq(memberId), eq(idempotencyKey), any(), eq(ImageType.FOOD_PHOTO)))
                .thenReturn(new RecognitionAcceptedResponse(recognitionId, UUID.randomUUID(),
                        RecognitionStatus.PROCESSING, "/api/v1/recognitions/" + recognitionId, Instant.now()));

        MockMultipartFile image = new MockMultipartFile("image", "food.jpg", "image/jpeg", new byte[]{1});
        mockMvc.perform(multipart("/api/v1/recognitions")
                        .file(image)
                        .header("Idempotency-Key", idempotencyKey)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isAccepted());

        verify(recognitionService).create(eq(memberId), eq(idempotencyKey), any(), eq(ImageType.FOOD_PHOTO));
    }
}
