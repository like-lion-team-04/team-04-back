package com.likelion.firstbite.firstbiteserver.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import com.likelion.firstbite.firstbiteserver.auth.validation.MinAge;
import com.likelion.firstbite.firstbiteserver.auth.validation.ValidPassword;

import java.time.LocalDate;

public record SignUpRequest(
        @NotBlank(message = "휴대폰 인증 토큰은 필수입니다.")
        String verificationToken,

        @NotBlank(message = "이름은 필수입니다.")
        @Size(max = 50, message = "이름은 50자 이하여야 합니다.")
        String name,

        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이어야 합니다.")
        @Size(max = 254, message = "이메일은 254자 이하여야 합니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
        @ValidPassword
        String password,

        @NotNull(message = "생년월일은 필수입니다.")
        @Past(message = "생년월일은 과거 날짜여야 합니다.")
        @MinAge(14)
        LocalDate birthDate,

        @AssertTrue(message = "이용약관 동의가 필요합니다.")
        @NotNull(message = "이용약관 동의 여부는 필수입니다.")
        Boolean termsAgreed,

        @AssertTrue(message = "개인정보 처리방침 동의가 필요합니다.")
        @NotNull(message = "개인정보 처리방침 동의 여부는 필수입니다.")
        Boolean privacyAgreed,

        @NotNull(message = "마케팅 수신 동의 여부는 필수입니다.")
        Boolean marketingAgreed
) {
    public SignUpRequest {
        if (email != null) {
            email = email.trim();
        }
        if (name != null) {
            name = name.trim();
        }
    }
}
