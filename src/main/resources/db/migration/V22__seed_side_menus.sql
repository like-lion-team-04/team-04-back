-- Generated from data/food-images/delicacies_database.xlsx (side_menu_catalog sheet).
-- estimated_price text such as "1,000원" is stored as an integer number of KRW.
-- The database supports one nutrient focus; almonds (BOTH) are categorized as FIBER
-- to match the workbook's fiber-focused recommendation reason.
insert into side_menus (id, food_id, nutrient_focus, estimated_price, is_active, created_at, updated_at) values
    ('e2a8ad35-44eb-5a3d-9af7-afe5572ebcfe', (select id from foods where food_code = 'boiled_egg'), 'PROTEIN', 1000, true, current_timestamp, current_timestamp),
    ('62a7c7dd-2952-503a-969b-94d54369f5ec', (select id from foods where food_code = 'soft_tofu'), 'PROTEIN', 1600, true, current_timestamp, current_timestamp),
    ('651e2494-e9e3-594d-a37d-0552375ef606', (select id from foods where food_code = 'chicken_breast'), 'PROTEIN', 3000, true, current_timestamp, current_timestamp),
    ('f58901f5-d607-550b-b2fe-a88698a3d232', (select id from foods where food_code = 'cherry_tomato'), 'FIBER', 1300, true, current_timestamp, current_timestamp),
    ('5ca775a1-d733-58aa-a98e-fb0c91c83a08', (select id from foods where food_code = 'radish_namul'), 'FIBER', 1000, true, current_timestamp, current_timestamp),
    ('789a4179-0af5-55da-8cc2-1e6630ccddde', (select id from foods where food_code = 'cucumber'), 'FIBER', 1500, true, current_timestamp, current_timestamp),
    ('fc651979-ab80-5a78-b433-576e738e35e9', (select id from foods where food_code = 'cabbage'), 'FIBER', 300, true, current_timestamp, current_timestamp),
    ('3bfc4b9d-77e7-5942-965a-0c833da542c2', (select id from foods where food_code = 'almonds'), 'FIBER', 1500, true, current_timestamp, current_timestamp),
    ('162dceb0-3f44-5baf-be73-65730bf7c3af', (select id from foods where food_code = 'apple'), 'FIBER', 3000, true, current_timestamp, current_timestamp),
    ('1b32d40d-11b0-5a58-81e8-73191174a149', (select id from foods where food_code = 'paprika'), 'FIBER', 2000, true, current_timestamp, current_timestamp),
    ('9c7bab5f-499c-5845-a35d-03d5347a491a', (select id from foods where food_code = 'boiled_broccoli'), 'FIBER', 1600, true, current_timestamp, current_timestamp),
    ('f9cdc667-a448-5099-9b4f-88176115ad04', (select id from foods where food_code = 'canned_tuna'), 'PROTEIN', 2000, true, current_timestamp, current_timestamp),
    ('3e680d7d-4b2a-55af-af0c-edbd8020d2fd', (select id from foods where food_code = 'plain_greek_yogurt'), 'PROTEIN', 2700, true, current_timestamp, current_timestamp),
    ('e30800d5-dab0-508f-88a7-9ff4dd43adec', (select id from foods where food_code = 'milk'), 'PROTEIN', 1100, true, current_timestamp, current_timestamp),
    ('5ea727e2-f588-5318-870d-d6abd624496c', (select id from foods where food_code = 'edamame'), 'PROTEIN', 500, true, current_timestamp, current_timestamp);
