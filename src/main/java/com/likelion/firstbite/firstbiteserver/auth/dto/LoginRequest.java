package com.likelion.firstbite.firstbiteserver.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 64) String password
) {
    public LoginRequest {
        if (email != null) email = email.trim();
    }
}
