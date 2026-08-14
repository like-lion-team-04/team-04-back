create table coaching_records (
    id uuid primary key,
    session_id uuid not null,
    meal_id uuid not null,
    member_id uuid not null,
    reason varchar(20) not null,
    completed_stages integer not null,
    skipped_stages integer not null,
    total_seconds bigint not null,
    client_ended_at timestamp with time zone not null,
    completed_at timestamp with time zone not null,
    idempotency_key uuid not null,
    request_hash varchar(64) not null,
    created_at timestamp with time zone not null,
    constraint fk_coaching_records_session foreign key (session_id) references coaching_sessions(id),
    constraint fk_coaching_records_meal foreign key (meal_id) references meals(id),
    constraint fk_coaching_records_member foreign key (member_id) references members(id),
    constraint uk_coaching_records_session unique (session_id),
    constraint ck_coaching_records_reason check (reason in ('COMPLETED', 'USER_ENDED')),
    constraint ck_coaching_records_counts check (completed_stages >= 0 and skipped_stages >= 0 and total_seconds >= 0)
);

create index idx_coaching_records_member_completed on coaching_records(member_id, completed_at desc);
create index idx_coaching_records_idempotency on coaching_records(member_id, idempotency_key, created_at desc);
