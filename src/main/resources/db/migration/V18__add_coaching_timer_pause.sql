alter table coaching_sessions drop constraint ck_coaching_sessions_status;
alter table coaching_sessions
    add constraint ck_coaching_sessions_status
    check (status in ('IN_PROGRESS', 'PAUSED', 'COMPLETED', 'CANCELLED'));

alter table coaching_sessions add column paused_at timestamp with time zone;
alter table coaching_sessions add column paused_remaining_seconds bigint;
