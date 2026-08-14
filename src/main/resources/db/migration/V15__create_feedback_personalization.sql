create table coaching_feedbacks (
    id uuid primary key,
    record_id uuid not null unique,
    member_id uuid not null,
    sleepiness_score integer,
    skipped boolean not null,
    answered_at timestamp with time zone not null,
    idempotency_key uuid not null,
    request_hash varchar(64) not null,
    feedback_count integer not null,
    personalization_updated boolean not null,
    created_at timestamp with time zone not null,
    constraint chk_feedback_score check (
        (skipped = true and sleepiness_score is null)
        or (skipped = false and sleepiness_score between 1 and 5)
    )
);

create index idx_feedback_member_created on coaching_feedbacks(member_id, created_at);
create index idx_feedback_idempotency on coaching_feedbacks(member_id, idempotency_key, created_at);

create table personalization_profiles (
    member_id uuid primary key,
    feedback_count integer not null,
    coefficient decimal(4, 2),
    direction varchar(20) not null,
    updated_at timestamp with time zone not null
);
