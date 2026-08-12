package com.likelion.firstbite.firstbiteserver.auth.phone;

import com.likelion.firstbite.firstbiteserver.common.exception.BusinessException;
import org.springframework.http.HttpStatus;

public final class PhoneNumberNormalizer {
    private PhoneNumberNormalizer() {}

    public static String normalize(String raw) {
        String digits = raw == null ? "" : raw.replaceAll("[^0-9+]", "");
        String normalized;
        if (digits.startsWith("+82")) normalized = digits;
        else if (digits.startsWith("82")) normalized = "+" + digits;
        else if (digits.startsWith("0")) normalized = "+82" + digits.substring(1);
        else normalized = "";
        if (!normalized.matches("\\+8210\\d{8}")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "COMMON_INVALID_REQUEST", "올바른 휴대폰 번호 형식이 아닙니다.");
        }
        return normalized;
    }

    public static String mask(String normalized) {
        String local = "0" + normalized.substring(3);
        return local.substring(0, 3) + "-****-" + local.substring(local.length() - 4);
    }
}
