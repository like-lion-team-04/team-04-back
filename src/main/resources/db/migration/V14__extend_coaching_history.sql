alter table coaching_records add column total_stages integer not null default 1;
update coaching_records
set total_stages = (select coaching_sessions.total_stages
                    from coaching_sessions
                    where coaching_sessions.id = coaching_records.session_id);

create table meal_reuses (
    id uuid primary key,
    member_id uuid not null,
    source_record_id uuid not null,
    new_meal_id uuid not null,
    idempotency_key uuid not null,
    request_hash varchar(64) not null,
    copied_item_count integer not null,
    created_at timestamp with time zone not null
);

create index idx_meal_reuses_idempotency
    on meal_reuses(member_id, idempotency_key, created_at);
