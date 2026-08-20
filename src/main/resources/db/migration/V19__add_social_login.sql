-- 소셜 로그인(카카오, 구글) 지원.
-- 소셜 가입자는 비밀번호/휴대폰 인증/생년월일이 없을 수 있으므로 필수 제약을 완화한다.
alter table members alter column password_hash drop not null;
alter table members alter column phone_encrypted drop not null;
alter table members alter column phone_hash drop not null;
alter table members alter column birth_date drop not null;

-- 소셜 제공자 식별 정보
alter table members add column provider varchar(20);
alter table members add column provider_id varchar(191);
alter table members add column profile_image_url varchar(512);

-- (provider, provider_id) 조합은 유일해야 한다. (Postgres는 NULL을 서로 다른 값으로 취급하므로 이메일 회원끼리는 충돌하지 않는다.)
create unique index uk_members_provider on members (provider, provider_id);
