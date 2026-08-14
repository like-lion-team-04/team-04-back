create table coaching_stage_records (
    id uuid primary key,
    session_id uuid not null,
    stage integer not null,
    action varchar(20) not null,
    result varchar(20) not null,
    actual_seconds bigint not null,
    occurred_at timestamp with time zone not null,
    received_at timestamp with time zone not null,
    constraint fk_coaching_stage_records_session foreign key (session_id)
        references coaching_sessions(id) on delete cascade,
    constraint uk_coaching_stage_records_session_stage unique (session_id, stage),
    constraint ck_coaching_stage_records_action check (action in ('AUTO_ADVANCE', 'SKIP', 'NEXT', 'COMPLETE', 'USER_END')),
    constraint ck_coaching_stage_records_result check (result in ('COMPLETED', 'SKIPPED')),
    constraint ck_coaching_stage_records_seconds check (actual_seconds >= 0)
);

create index idx_coaching_stage_records_session on coaching_stage_records(session_id, stage);
