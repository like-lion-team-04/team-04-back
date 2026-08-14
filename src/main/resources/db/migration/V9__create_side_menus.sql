create table side_menus (
    id uuid primary key,
    food_id uuid not null,
    nutrient_focus varchar(20) not null,
    estimated_price integer not null,
    is_active boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_side_menus_food foreign key (food_id) references foods(id),
    constraint uk_side_menus_food unique (food_id),
    constraint ck_side_menus_focus check (nutrient_focus in ('PROTEIN', 'FIBER')),
    constraint ck_side_menus_price check (estimated_price >= 0)
);

create index idx_side_menus_focus_active on side_menus(nutrient_focus, is_active);
