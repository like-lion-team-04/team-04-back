-- 고등어구이(grilled_mackerel) 이미지 URL 추가. V25 이후 이미지 파일이 확보되어 별도 보완.
-- 이미지 파일은 프론트 정적 자원 assets/foods/grilled_mackerel.jpg 로 서빙된다.
update foods set image_url = '/assets/foods/grilled_mackerel.jpg', updated_at = current_timestamp where food_code = 'grilled_mackerel';
