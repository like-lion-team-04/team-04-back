create table meals (
    id uuid primary key,
    member_id uuid not null,
    source varchar(20) not null,
    recognition_id uuid,
    status varchar(20) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_meals_member foreign key (member_id) references members(id)
);

create table meal_items (
    id uuid primary key,
    meal_id uuid not null,
    food_id uuid not null,
    food_name varchar(100) not null,
    serving_multiplier numeric(3,1) not null,
    estimated boolean not null,
    created_at timestamp with time zone not null,
    constraint fk_meal_items_meal foreign key (meal_id) references meals(id) on delete cascade,
    constraint fk_meal_items_food foreign key (food_id) references foods(id),
    constraint ck_meal_items_multiplier check (serving_multiplier in (0.5, 1.0, 1.5, 2.0)),
    constraint uk_meal_items_meal_food unique (meal_id, food_id)
);

create index idx_meals_member_created on meals(member_id, created_at desc);
create index idx_meal_items_meal on meal_items(meal_id);
