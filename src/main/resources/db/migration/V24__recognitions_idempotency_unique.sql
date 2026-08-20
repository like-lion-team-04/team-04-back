-- 사진 인식 멱등성 보장: 동일 회원 + 동일 Idempotency-Key 중복 생성 방지.
-- 동시 요청 시 유니크 위반으로 한쪽만 성공하고, 앱은 기존 결과를 반환하도록 처리한다.
create unique index if not exists uk_recognitions_member_idem
    on recognitions (member_id, idempotency_key);
