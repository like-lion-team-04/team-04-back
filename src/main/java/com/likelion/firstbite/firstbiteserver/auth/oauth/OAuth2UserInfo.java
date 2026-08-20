package com.likelion.firstbite.firstbiteserver.auth.oauth;

import java.util.Map;

/**
 * 제공자별로 다른 OAuth2 사용자 속성에서 공통 정보를 추출한다.
 * - Google: 표준 OIDC 속성(sub, email, name, picture)
 * - Kakao : id + kakao_account.email + properties.nickname/profile_image
 */
public record OAuth2UserInfo(String provider, String providerId, String email, String name, String picture) {

    @SuppressWarnings("unchecked")
    public static OAuth2UserInfo from(String registrationId, Map<String, Object> attributes) {
        if ("google".equals(registrationId)) {
            return new OAuth2UserInfo("google",
                    str(attributes.get("sub")),
                    str(attributes.get("email")),
                    str(attributes.get("name")),
                    str(attributes.get("picture")));
        }
        if ("kakao".equals(registrationId)) {
            String providerId = str(attributes.get("id"));
            Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
            Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
            String email = account != null ? str(account.get("email")) : null;
            String name = properties != null ? str(properties.get("nickname")) : null;
            String picture = properties != null ? str(properties.get("profile_image")) : null;
            return new OAuth2UserInfo("kakao", providerId, email, name, picture);
        }
        throw new IllegalArgumentException("지원하지 않는 소셜 제공자입니다: " + registrationId);
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
