create table meal_analyses (
    id uuid primary key,
    meal_id uuid not null,
    member_id uuid not null,
    idempotency_key uuid not null,
    request_hash varchar(64) not null,
    baseline_gl numeric(10,2) not null,
    recommended_gl numeric(10,2) not null,
    relief_rate numeric(6,4) not null,
    personal_coefficient numeric(6,4) not null,
    estimated_item_ratio numeric(6,4) not null,
    created_at timestamp with time zone not null,
    constraint fk_meal_analyses_meal foreign key (meal_id) references meals(id) on delete cascade,
    constraint fk_meal_analyses_member foreign key (member_id) references members(id)
);

create index idx_meal_analyses_meal on meal_analyses(meal_id, created_at desc);
create index idx_meal_analyses_idempotency on meal_analyses(member_id, idempotency_key, created_at desc);
