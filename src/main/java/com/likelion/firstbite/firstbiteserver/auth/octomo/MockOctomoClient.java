package com.likelion.firstbite.firstbiteserver.auth.octomo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 로컬/개발 전용 목 구현. 실제 OCTOMO 계정 없이 휴대폰 인증 흐름을 검증하기 위한 것이다.
 * app.octomo.mock=true 일 때만 활성화되며, 문자 수신을 항상 성공(true)으로 간주한다.
 * 운영 환경에서는 절대 활성화하지 않는다.
 */
@Component
@ConditionalOnProperty(prefix = "app.octomo", name = "mock", havingValue = "true")
public class MockOctomoClient implements OctomoClient {
    private static final Logger log = LoggerFactory.getLogger(MockOctomoClient.class);

    public MockOctomoClient() {
        log.warn("MockOctomoClient 활성화: 휴대폰 인증이 항상 성공 처리됩니다. 운영 환경에서 사용 금지.");
    }

    @Override
    public boolean messageExists(String phoneNumber, String messageText, int withinMinutes) {
        log.warn("[MOCK OCTOMO] 문자 수신 확인을 성공으로 간주합니다. phone={}, text={}", phoneNumber, messageText);
        return true;
    }
}
