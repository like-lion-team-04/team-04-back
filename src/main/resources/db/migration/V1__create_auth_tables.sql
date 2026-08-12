create table members (
    id uuid primary key,
    email varchar(254) not null,
    password_hash varchar(100) not null,
    name varchar(50) not null,
    birth_date date not null,
    phone_encrypted varchar(512) not null,
    phone_hash varchar(64) not null,
    marketing_agreed boolean not null,
    marketing_agreed_at timestamp with time zone,
    status varchar(20) not null,
    created_at timestamp with time zone not null,
    constraint uk_members_email unique (email),
    constraint uk_members_phone_hash unique (phone_hash)
);

create table terms_agreements (
    id uuid primary key,
    member_id uuid not null,
    terms_type varchar(30) not null,
    terms_version varchar(30) not null,
    agreed boolean not null,
    agreed_at timestamp with time zone not null,
    constraint fk_terms_agreements_member foreign key (member_id) references members(id)
);

create table phone_verifications (
    id uuid primary key,
    phone_hash varchar(64) not null,
    phone_encrypted varchar(512) not null,
    code_hash varchar(64) not null,
    token_hash varchar(64),
    code_expires_at timestamp with time zone not null,
    token_expires_at timestamp with time zone,
    resend_available_at timestamp with time zone not null,
    attempt_count integer not null,
    status varchar(20) not null,
    created_at timestamp with time zone not null
);

create index idx_phone_verification_phone_created on phone_verifications(phone_hash, created_at);
create unique index idx_phone_verification_token on phone_verifications(token_hash);
