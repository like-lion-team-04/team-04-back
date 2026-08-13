package com.likelion.firstbite.firstbiteserver.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteAccountRequest(
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
        String password,
        @NotBlank(message = "탈퇴 확인 문구는 필수입니다.")
        String confirmText
) {
}
