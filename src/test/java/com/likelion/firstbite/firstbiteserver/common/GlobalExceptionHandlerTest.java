package com.likelion.firstbite.firstbiteserver.common;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 프레임워크 예외들이 표준 ErrorResponse({success:false, error:{code,message}}) 규격으로
 * 변환되는지 검증한다. (public 엔드포인트만 사용하므로 인증 불필요)
 */
@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {
    @Autowired MockMvc mockMvc;

    @Test
    void malformedJsonBodyReturnsStandardBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON_INVALID_REQUEST"));
    }

    @Test
    void unsupportedHttpMethodReturns405() throws Exception {
        mockMvc.perform(get("/api/v1/auth/login"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON_METHOD_NOT_ALLOWED"));
    }

    @Test
    void unsupportedMediaTypeReturns415() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("email=a"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("COMMON_UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void validationErrorReturnsStandardBadRequest() throws Exception {
        // 이메일 형식 위반 등 @Valid 실패 → 표준 400
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").isNotEmpty());
    }
}
