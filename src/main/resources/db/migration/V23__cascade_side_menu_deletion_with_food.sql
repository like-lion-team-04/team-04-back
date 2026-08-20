ALTER TABLE side_menus
    DROP CONSTRAINT fk_side_menus_food;

ALTER TABLE side_menus
    ADD CONSTRAINT fk_side_menus_food
        FOREIGN KEY (food_id) REFERENCES foods (id) ON DELETE CASCADE;
