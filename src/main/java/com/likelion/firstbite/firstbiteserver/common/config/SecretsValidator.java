package com.likelion.firstbite.firstbiteserver.common.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 운영(local/test 이외) 프로파일에서 보안 시크릿이 커밋된 기본값이거나 비어 있으면
 * 애플리케이션 기동을 즉시 중단시킨다. 실수로 기본 시크릿으로 배포되어
 * 토큰 위조/전화번호 복호화가 가능해지는 사고를 원천 차단한다.
 */
@Component
public class SecretsValidator implements InitializingBean {
    // application.yml 에 커밋된 개발용 기본값들(운영에서 절대 사용 금지)
    private static final Set<String> FORBIDDEN_DEFAULTS = Set.of(
            "firstbite-local-jwt-secret-change-me-at-least-32-bytes",
            "firstbite-local-encryption-key-change-me",
            "firstbite-local-hmac-key-change-me"
    );

    private final Environment environment;
    private final String jwtSecret;
    private final String phoneEncryptionKey;
    private final String phoneHmacKey;

    public SecretsValidator(Environment environment,
                            @Value("${app.security.jwt-secret:}") String jwtSecret,
                            @Value("${app.security.phone-encryption-key:}") String phoneEncryptionKey,
                            @Value("${app.security.phone-hmac-key:}") String phoneHmacKey) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
        this.phoneEncryptionKey = phoneEncryptionKey;
        this.phoneHmacKey = phoneHmacKey;
    }

    @Override
    public void afterPropertiesSet() {
        List<String> activeProfiles = List.of(environment.getActiveProfiles());
        boolean isDevProfile = activeProfiles.contains("local") || activeProfiles.contains("test")
                || activeProfiles.isEmpty();
        if (isDevProfile) {
            return; // 로컬/테스트는 기본값 허용
        }
        checkSecret("app.security.jwt-secret", jwtSecret);
        checkSecret("app.security.phone-encryption-key", phoneEncryptionKey);
        checkSecret("app.security.phone-hmac-key", phoneHmacKey);
    }

    private void checkSecret(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " 시크릿이 설정되지 않았습니다. 운영 배포 시 환경변수로 주입해야 합니다.");
        }
        if (FORBIDDEN_DEFAULTS.contains(value)) {
            throw new IllegalStateException(name + " 가 커밋된 개발용 기본값입니다. 운영에서는 반드시 별도 시크릿으로 교체하세요.");
        }
    }
}
