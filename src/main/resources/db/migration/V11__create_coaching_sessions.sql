create table coaching_sessions (
    id uuid primary key,
    meal_id uuid not null,
    member_id uuid not null,
    plan_version integer not null,
    total_stages integer not null,
    status varchar(20) not null,
    current_stage integer not null,
    started_at timestamp with time zone not null,
    stage_started_at timestamp with time zone not null,
    stage_ends_at timestamp with time zone,
    completed_at timestamp with time zone,
    idempotency_key uuid not null,
    request_hash varchar(64) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_coaching_sessions_meal foreign key (meal_id) references meals(id),
    constraint fk_coaching_sessions_member foreign key (member_id) references members(id),
    constraint ck_coaching_sessions_status check (status in ('IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    constraint ck_coaching_sessions_stage check (current_stage >= 1 and total_stages >= current_stage)
);

create index idx_coaching_sessions_member_status on coaching_sessions(member_id, status);
create index idx_coaching_sessions_idempotency on coaching_sessions(member_id, idempotency_key, created_at desc);
