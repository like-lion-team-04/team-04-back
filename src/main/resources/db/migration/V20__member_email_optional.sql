-- 소셜 간편가입(카카오)은 이메일 없이도 가입할 수 있어야 한다.
-- 소셜 계정의 실제 식별자는 (provider, provider_id)이며 이메일은 선택 정보다.
-- (uk_members_email UNIQUE 제약은 유지: Postgres는 NULL을 서로 다른 값으로 취급하므로 이메일 없는 회원끼리 충돌하지 않는다.)
alter table members alter column email drop not null;
