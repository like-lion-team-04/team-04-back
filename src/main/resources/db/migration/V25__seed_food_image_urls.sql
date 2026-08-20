-- 음식 카탈로그 이미지 URL 채우기.
-- 이미지 파일은 프론트(team04-front) 정적 자원 assets/foods/{food_code}.{ext} 로 서빙되며
-- nginx가 같은 오리진에서 프론트 정적 + 백엔드 /api 를 함께 서빙하므로 절대경로(/assets/...)로 저장한다.
-- 파일이 존재하는 food_code 만 채우고, 없는 항목은 NULL 유지(프론트가 중립 placeholder로 폴백).
-- H2(test)/PostgreSQL(prod) 모두 호환되도록 개별 UPDATE 문으로 작성한다.
update foods set image_url = '/assets/foods/gimbap.jpg', updated_at = current_timestamp where food_code = 'gimbap';
update foods set image_url = '/assets/foods/tteokbokki.webp', updated_at = current_timestamp where food_code = 'tteokbokki';
update foods set image_url = '/assets/foods/white_rice.jpg', updated_at = current_timestamp where food_code = 'white_rice';
update foods set image_url = '/assets/foods/spicy_pork.jpg', updated_at = current_timestamp where food_code = 'spicy_pork';
update foods set image_url = '/assets/foods/soybean_paste_stew.jpg', updated_at = current_timestamp where food_code = 'soybean_paste_stew';
update foods set image_url = '/assets/foods/ramyeon.webp', updated_at = current_timestamp where food_code = 'ramyeon';
update foods set image_url = '/assets/foods/banquet_noodles.jpg', updated_at = current_timestamp where food_code = 'banquet_noodles';
update foods set image_url = '/assets/foods/meat_dumplings.jpg', updated_at = current_timestamp where food_code = 'meat_dumplings';
update foods set image_url = '/assets/foods/ham_sandwich.jpg', updated_at = current_timestamp where food_code = 'ham_sandwich';
update foods set image_url = '/assets/foods/white_bread.jpg', updated_at = current_timestamp where food_code = 'white_bread';
update foods set image_url = '/assets/foods/boiled_egg.jpg', updated_at = current_timestamp where food_code = 'boiled_egg';
update foods set image_url = '/assets/foods/soft_tofu.jpg', updated_at = current_timestamp where food_code = 'soft_tofu';
update foods set image_url = '/assets/foods/chicken_breast.jpg', updated_at = current_timestamp where food_code = 'chicken_breast';
update foods set image_url = '/assets/foods/cherry_tomato.jpg', updated_at = current_timestamp where food_code = 'cherry_tomato';
update foods set image_url = '/assets/foods/radish_namul.jpg', updated_at = current_timestamp where food_code = 'radish_namul';
update foods set image_url = '/assets/foods/cucumber.jpg', updated_at = current_timestamp where food_code = 'cucumber';
update foods set image_url = '/assets/foods/cabbage.jpg', updated_at = current_timestamp where food_code = 'cabbage';
update foods set image_url = '/assets/foods/almonds.jpg', updated_at = current_timestamp where food_code = 'almonds';
update foods set image_url = '/assets/foods/apple.jpg', updated_at = current_timestamp where food_code = 'apple';
update foods set image_url = '/assets/foods/paprika.jpg', updated_at = current_timestamp where food_code = 'paprika';
update foods set image_url = '/assets/foods/boiled_broccoli.jpg', updated_at = current_timestamp where food_code = 'boiled_broccoli';
update foods set image_url = '/assets/foods/canned_tuna.jpg', updated_at = current_timestamp where food_code = 'canned_tuna';
update foods set image_url = '/assets/foods/plain_greek_yogurt.jpg', updated_at = current_timestamp where food_code = 'plain_greek_yogurt';
update foods set image_url = '/assets/foods/edamame.jpg', updated_at = current_timestamp where food_code = 'edamame';
update foods set image_url = '/assets/foods/milk.jpg', updated_at = current_timestamp where food_code = 'milk';
