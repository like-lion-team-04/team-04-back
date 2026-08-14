create table foods (
    id uuid primary key,
    food_code varchar(80) not null,
    name varchar(100) not null,
    original_category varchar(40) not null,
    search_category varchar(20) not null,
    initials varchar(50) not null,
    serving_description varchar(150) not null,
    serving_amount numeric(10,2) not null,
    serving_unit varchar(10) not null,
    carb_g numeric(10,2) not null,
    fiber_g numeric(10,2) not null,
    protein_g numeric(10,2) not null,
    fat_g numeric(10,2) not null,
    available_carb_g numeric(10,2) not null,
    gi numeric(7,2) not null,
    gl numeric(10,2) not null,
    calorie_kcal numeric(10,2) not null,
    nutrition_data_quality varchar(20) not null,
    gi_data_quality varchar(20) not null,
    is_active boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_foods_food_code unique (food_code)
);

create index idx_foods_name on foods(name);
create index idx_foods_initials on foods(initials);
create index idx_foods_category_active on foods(search_category, is_active);
