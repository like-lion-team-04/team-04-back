create table refresh_tokens (
    id uuid primary key,
    member_id uuid not null,
    token_hash varchar(64) not null,
    expires_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    revoked_at timestamp with time zone,
    constraint uk_refresh_tokens_hash unique (token_hash),
    constraint fk_refresh_tokens_member foreign key (member_id) references members(id)
);

create index idx_refresh_tokens_member on refresh_tokens(member_id);

create table login_attempts (
    identifier_hash varchar(64) primary key,
    failed_count integer not null,
    window_started_at timestamp with time zone not null,
    locked_until timestamp with time zone
);
