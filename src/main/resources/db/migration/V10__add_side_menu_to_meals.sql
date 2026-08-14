alter table meals add column coaching_plan_version integer not null default 1;

alter table meal_items add column side_menu_id uuid;
alter table meal_items add constraint fk_meal_items_side_menu
    foreign key (side_menu_id) references side_menus(id);
create index idx_meal_items_side_menu on meal_items(side_menu_id);
