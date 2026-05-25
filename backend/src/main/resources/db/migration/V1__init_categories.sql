INSERT INTO categories (parent_id, name, depth, sort_order) VALUES
(NULL, '디지털/가전', 0, 1),
(NULL, '의류/잡화', 0, 2),
(NULL, '가구/인테리어', 0, 3),
(NULL, '도서/티켓/취미', 0, 4),
(NULL, '스포츠/레저', 0, 5),
(NULL, '뷰티/미용', 0, 6),
(NULL, '식품/건강', 0, 7),
(NULL, '반려동물', 0, 8),
(NULL, '기타', 0, 9);

-- 디지털/가전 (id=1)
INSERT INTO categories (parent_id, name, depth, sort_order) VALUES
(1, '스마트폰/태블릿', 1, 1),
(1, '노트북/PC', 1, 2),
(1, '카메라/렌즈', 1, 3),
(1, '음향기기', 1, 4),
(1, '게임/콘솔', 1, 5),
(1, 'TV/영상기기', 1, 6),
(1, '주변기기/액세서리', 1, 7);

-- 의류/잡화 (id=2)
INSERT INTO categories (parent_id, name, depth, sort_order) VALUES
(2, '남성의류', 1, 1),
(2, '여성의류', 1, 2),
(2, '신발', 1, 3),
(2, '가방/지갑', 1, 4),
(2, '시계/주얼리', 1, 5),
(2, '아동/유아의류', 1, 6);

-- 가구/인테리어 (id=3)
INSERT INTO categories (parent_id, name, depth, sort_order) VALUES
(3, '소파/의자', 1, 1),
(3, '침대/매트리스', 1, 2),
(3, '책상/선반', 1, 3),
(3, '조명', 1, 4),
(3, '수납/정리', 1, 5);

-- 도서/티켓/취미 (id=4)
INSERT INTO categories (parent_id, name, depth, sort_order) VALUES
(4, '도서', 1, 1),
(4, '영화/공연 티켓', 1, 2),
(4, '음반/DVD', 1, 3),
(4, '악기', 1, 4),
(4, '취미/공예', 1, 5);

-- 스포츠/레저 (id=5)
INSERT INTO categories (parent_id, name, depth, sort_order) VALUES
(5, '운동용품', 1, 1),
(5, '자전거', 1, 2),
(5, '캠핑/등산', 1, 3),
(5, '수상스포츠', 1, 4),
(5, '구기/라켓', 1, 5);
