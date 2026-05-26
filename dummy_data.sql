-- ================================================================
-- Tradev 더미 데이터
-- 비밀번호: Test1234! (BCrypt 해시)
-- 실행: docker exec -i tradev-mysql mysql -u tradev -ptradev_password tradev < dummy_data.sql
-- ================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 기존 데이터 초기화 (재실행 가능하도록)
TRUNCATE TABLE notifications;
TRUNCATE TABLE reviews;
TRUNCATE TABLE chat_messages;
TRUNCATE TABLE chat_rooms;
TRUNCATE TABLE trades;
TRUNCATE TABLE wishlists;
TRUNCATE TABLE item_images;
TRUNCATE TABLE items;
DELETE FROM users WHERE id > 0;

-- ----------------------------------------------------------------
-- 1. Users (7명)
-- ----------------------------------------------------------------
INSERT INTO users (email, password, nickname, profile_image_url, bio, status, role, trust_score, trust_grade, oauth_provider, oauth_provider_id, email_verified, created_at, updated_at) VALUES

-- 관리자
('admin@tradev.shop', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9i', '관리자', NULL, '트레이드 관리자입니다.', 'ACTIVE', 'ADMIN', 95, 'TREE', NULL, NULL, true, NOW() - INTERVAL 60 DAY, NOW()),

-- 판매자들
('seller1@naver.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9i', '김전자', 'https://api.dicebear.com/7.x/thumbs/svg?seed=seller1', '전자제품 전문 판매자. 직거래 환영합니다 🤝', 'ACTIVE', 'USER', 82, 'TREE', NULL, NULL, true, NOW() - INTERVAL 50 DAY, NOW()),
('seller2@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9i', '이패션', 'https://api.dicebear.com/7.x/thumbs/svg?seed=seller2', '의류/잡화 판매 전문. 정품만 취급합니다 👗', 'ACTIVE', 'USER', 65, 'FRUIT', NULL, NULL, true, NOW() - INTERVAL 40 DAY, NOW()),
('seller3@naver.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9i', '박가구', 'https://api.dicebear.com/7.x/thumbs/svg?seed=seller3', '가구 전문 셀러입니다. 인테리어 상담도 가능해요 🪑', 'ACTIVE', 'USER', 45, 'SPROUT', NULL, NULL, true, NOW() - INTERVAL 30 DAY, NOW()),

-- 구매자들
('buyer1@kakao.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9i', '최구매', 'https://api.dicebear.com/7.x/thumbs/svg?seed=buyer1', '좋은 물건을 합리적인 가격에 구매하고 싶어요!', 'ACTIVE', 'USER', 30, 'SPROUT', NULL, NULL, true, NOW() - INTERVAL 20 DAY, NOW()),
('buyer2@naver.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9i', '정쇼퍼', 'https://api.dicebear.com/7.x/thumbs/svg?seed=buyer2', '중고 거래 초보입니다. 잘 부탁드려요 😊', 'ACTIVE', 'USER', 10, 'SEED', NULL, NULL, true, NOW() - INTERVAL 10 DAY, NOW()),
('user1@gmail.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lh9i', '한일반', 'https://api.dicebear.com/7.x/thumbs/svg?seed=user1', NULL, 'ACTIVE', 'USER', 0, 'SEED', NULL, NULL, true, NOW() - INTERVAL 5 DAY, NOW());

-- ----------------------------------------------------------------
-- 2. Items (15개) — category_id는 V1 마이그레이션 기준
--    10=스마트폰, 11=노트북, 13=음향기기, 14=게임/콘솔
--    17=남성의류, 18=여성의류, 19=신발
--    23=소파/의자, 25=책상/선반
--    28=도서, 32=취미/공예
--    33=운동용품, 35=캠핑/등산
-- ----------------------------------------------------------------
INSERT INTO items (seller_id, category_id, title, description, price, status, trade_type, item_condition, location, view_count, wish_count, hidden, created_at, updated_at) VALUES

-- 디지털/가전 (seller_id=2: 김전자)
(2, 10, 'iPhone 15 Pro 256GB 블랙 티타늄', '작년 11월 구매, 케이스 끼고 사용해서 상태 매우 좋습니다.\n기스 전혀 없고 배터리 성능 98%입니다.\n박스/케이블/충전기 모두 있습니다.\n직거래 선호하나 안전거래도 가능합니다.', 1150000, 'SALE', 'ALL', 'LIKE_NEW', '서울 강남구', 245, 18, false, NOW() - INTERVAL 15 DAY, NOW() - INTERVAL 15 DAY),

(2, 11, 'MacBook Air M2 8GB/256GB 스페이스그레이', '2023년 3월 구매, 작업용으로만 사용했습니다.\n외관 깨끗하고 성능 문제 없습니다.\n충전기(MagSafe) 포함, 애플케어+ 2025년 3월까지 남아있습니다.\n직거래만 가능 (강남 근처)', 1050000, 'SALE', 'DIRECT', 'GOOD', '서울 강남구', 312, 24, false, NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 12 DAY),

(2, 13, 'Sony WH-1000XM5 노이즈캔슬링 헤드폰', '6개월 사용, 집에서만 사용했습니다.\n노이즈캔슬링 성능 최상이고 음질도 훌륭합니다.\n파우치/케이블/어댑터 모두 포함.\n급하게 처분합니다.', 280000, 'SALE', 'ALL', 'LIKE_NEW', '서울 마포구', 189, 31, false, NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 10 DAY),

(2, 14, 'Nintendo Switch OLED 화이트 + 게임 5개', '1년 사용. 화면 기스 없고 배터리 정상.\n포함 게임: 젤다, 마리오카트8, 스플래툰3, 포켓몬스칼렛, 링피트.\n독/조이콘/충전기 모두 있습니다.', 380000, 'RESERVED', 'DIRECT', 'GOOD', '서울 송파구', 156, 12, false, NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 2 DAY),

(2, 10, 'iPad Pro 12.9 M2 256GB Wi-Fi + Apple Pencil 2세대', '4개월 사용, 보호필름 부착 상태.\n애플펜슬 2세대, 매직키보드 케이스 포함.\n영상 편집, 드로잉 용도였으나 노트북으로 대체합니다.\n케이스 끼고 사용 스크래치 없음.', 1450000, 'SALE', 'ALL', 'LIKE_NEW', '경기 성남시', 421, 45, false, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 5 DAY),

-- 의류/잡화 (seller_id=3: 이패션)
(3, 17, '나이키 에어포스1 화이트 270mm', '3번 착용, 오염 없고 깨끗합니다.\n박스 포함, 여분 끈도 있습니다.\n택배 거래 가능합니다.', 75000, 'SALE', 'ALL', 'LIKE_NEW', '부산 해운대구', 98, 8, false, NOW() - INTERVAL 7 DAY, NOW() - INTERVAL 7 DAY),

(3, 18, '자라(ZARA) 린넨 블레이저 S사이즈', '한 번 입었습니다. 행사에서 구매했는데 사이즈가 맞지 않아서 판매합니다.\n베이지 컬러, 린넨 소재라 시원합니다.\n세탁 후 보관 중, 상태 완전 새것과 동일합니다.', 45000, 'SALE', 'DELIVERY', 'LIKE_NEW', '서울 종로구', 67, 15, false, NOW() - INTERVAL 6 DAY, NOW() - INTERVAL 6 DAY),

(3, 19, '뉴발란스 993 그레이 265mm', '10회 미만 착용. 매우 깨끗한 상태입니다.\n미국 정품, 영수증 있습니다.\n박스, 여분 끈 포함.', 185000, 'SALE', 'ALL', 'LIKE_NEW', '서울 용산구', 203, 27, false, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 4 DAY),

-- 가구/인테리어 (seller_id=4: 박가구)
(4, 23, '이케아 엑토르프 3인용 소파 베이지', '이사로 인해 판매합니다. 2년 사용.\n세탁 가능한 커버, 세탁 후 깨끗합니다.\n프레임 이상 없고 쿠션감 좋습니다.\n직접 가져가실 분만 연락주세요 (서울 관악구).', 180000, 'SALE', 'DIRECT', 'GOOD', '서울 관악구', 134, 9, false, NOW() - INTERVAL 9 DAY, NOW() - INTERVAL 9 DAY),

(4, 25, '허먼밀러 에어론 체어 사이즈B 리퍼', '오피스에서 사용하던 리퍼 제품 판매합니다.\n외관 사용감 있으나 기능 완벽합니다.\n허리 지지대, 팔걸이 모두 정상.\n직거래만 (경기 수원시)', 650000, 'SALE', 'DIRECT', 'FAIR', '경기 수원시', 287, 33, false, NOW() - INTERVAL 11 DAY, NOW() - INTERVAL 11 DAY),

-- 도서/취미 (seller_id=2)
(2, 28, '개발 서적 묶음 10권 (스프링, 자바, 클린코드 등)', '스프링 부트 완전 정복, 자바 ORM 표준 JPA, 클린코드, 오브젝트 등 개발 베스트셀러 10권.\n모두 밑줄/필기 없는 깨끗한 상태.\n택배 가능 (박스 무게 때문에 착불 요청드립니다).', 95000, 'SALE', 'DELIVERY', 'GOOD', '서울 성동구', 76, 11, false, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY),

(3, 32, '프라모델 건담 MG 1/100 RX-78-2 미조립', '선물 받았는데 프라모델에 관심이 없어 판매합니다.\n미개봉 새상품입니다.\n박스 약간의 눌림 있으나 내용물 완전합니다.', 42000, 'SALE', 'ALL', 'NEW', '인천 연수구', 45, 6, false, NOW() - INTERVAL 2 DAY, NOW() - INTERVAL 2 DAY),

-- 스포츠/레저 (seller_id=3)
(3, 33, '요가매트 + 필라테스 소품 세트', '6개월 사용. 라울프 고급 요가매트(6mm), 폼롤러, 밴드 5종, 요가블록 2개.\n위생 신경 써서 사용 후 항상 세척했습니다.\n택배 포장 후 발송 가능.', 55000, 'COMPLETED', 'ALL', 'GOOD', '서울 강서구', 92, 4, false, NOW() - INTERVAL 20 DAY, NOW() - INTERVAL 3 DAY),

(4, 35, '코베아 캠핑 의자 + 테이블 세트', '10회 미만 사용. 알루미늄 경량 캠핑 의자 2개 + 접이식 테이블 1개.\n세척 완료, 수납 가방 포함.\n부피가 있어 직거래 선호합니다.', 120000, 'SALE', 'DIRECT', 'LIKE_NEW', '경기 고양시', 118, 14, false, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),

(2, 10, 'Galaxy S24 Ultra 256GB 티타늄 블랙', '2개월 사용. 외관 무결점, 배터리 100%.\n S펜 포함, 정품 케이스/필름 부착 상태.\n박스/케이블 모두 있습니다.\n직거래 선호 (강남구 근처)', 1080000, 'SALE', 'ALL', 'LIKE_NEW', '서울 강남구', 334, 28, false, NOW(), NOW());

-- ----------------------------------------------------------------
-- 3. Item Images
-- ----------------------------------------------------------------
INSERT INTO item_images (item_id, image_url, s3key, sort_order) VALUES
-- iPhone 15 Pro (item 1)
(1, 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=600', 'dummy/item1_0.jpg', 0),
(1, 'https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=600', 'dummy/item1_1.jpg', 1),

-- MacBook Air M2 (item 2)
(2, 'https://images.unsplash.com/photo-1611186871348-b1ce696e52c9?w=600', 'dummy/item2_0.jpg', 0),

-- Sony 헤드폰 (item 3)
(3, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600', 'dummy/item3_0.jpg', 0),
(3, 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600', 'dummy/item3_1.jpg', 1),

-- Nintendo Switch (item 4)
(4, 'https://images.unsplash.com/photo-1591161822010-9b4c6bde93e4?w=600', 'dummy/item4_0.jpg', 0),

-- iPad Pro (item 5)
(5, 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=600', 'dummy/item5_0.jpg', 0),
(5, 'https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=600', 'dummy/item5_1.jpg', 1),

-- 나이키 에어포스1 (item 6)
(6, 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=600', 'dummy/item6_0.jpg', 0),

-- 자라 블레이저 (item 7)
(7, 'https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=600', 'dummy/item7_0.jpg', 0),

-- 뉴발란스 993 (item 8)
(8, 'https://images.unsplash.com/photo-1539185441755-769473a23570?w=600', 'dummy/item8_0.jpg', 0),
(8, 'https://images.unsplash.com/photo-1539185441755-769473a23570?w=600', 'dummy/item8_1.jpg', 1),

-- 이케아 소파 (item 9)
(9, 'https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=600', 'dummy/item9_0.jpg', 0),

-- 허먼밀러 체어 (item 10)
(10, 'https://images.unsplash.com/photo-1580480055273-228ff5388ef8?w=600', 'dummy/item10_0.jpg', 0),
(10, 'https://images.unsplash.com/photo-1580480055273-228ff5388ef8?w=600', 'dummy/item10_1.jpg', 1),

-- 개발 서적 (item 11)
(11, 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=600', 'dummy/item11_0.jpg', 0),

-- 건담 프라모델 (item 12)
(12, 'https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=600', 'dummy/item12_0.jpg', 0),

-- 요가매트 세트 (item 13)
(13, 'https://images.unsplash.com/photo-1601925260368-ae2f83cf8b7f?w=600', 'dummy/item13_0.jpg', 0),

-- 캠핑 의자 (item 14)
(14, 'https://images.unsplash.com/photo-1504280390367-361c6d9f38f4?w=600', 'dummy/item14_0.jpg', 0),
(14, 'https://images.unsplash.com/photo-1504280390367-361c6d9f38f4?w=600', 'dummy/item14_1.jpg', 1),

-- Galaxy S24 Ultra (item 15)
(15, 'https://images.unsplash.com/photo-1610945415295-d9bbf067e59c?w=600', 'dummy/item15_0.jpg', 0),
(15, 'https://images.unsplash.com/photo-1610945415295-d9bbf067e59c?w=600', 'dummy/item15_1.jpg', 1);

-- ----------------------------------------------------------------
-- 4. Wishlists
-- ----------------------------------------------------------------
INSERT INTO wishlists (user_id, item_id, created_at) VALUES
(5, 1, NOW() - INTERVAL 10 DAY),  -- buyer1 → iPhone
(5, 2, NOW() - INTERVAL 9 DAY),   -- buyer1 → MacBook
(5, 3, NOW() - INTERVAL 8 DAY),   -- buyer1 → 헤드폰
(5, 5, NOW() - INTERVAL 7 DAY),   -- buyer1 → iPad
(5, 10, NOW() - INTERVAL 6 DAY),  -- buyer1 → 허먼밀러
(6, 1, NOW() - INTERVAL 5 DAY),   -- buyer2 → iPhone
(6, 8, NOW() - INTERVAL 4 DAY),   -- buyer2 → 뉴발란스
(6, 15, NOW() - INTERVAL 3 DAY),  -- buyer2 → Galaxy S24
(7, 3, NOW() - INTERVAL 2 DAY),   -- user1 → 헤드폰
(7, 14, NOW() - INTERVAL 1 DAY);  -- user1 → 캠핑 의자

-- ----------------------------------------------------------------
-- 5. Trades
-- ----------------------------------------------------------------
INSERT INTO trades (item_id, buyer_id, seller_id, status, price, buyer_confirmed, seller_confirmed, created_at, updated_at) VALUES
-- 완료된 거래 (요가매트)
(13, 5, 3, 'COMPLETED', 55000, true, true, NOW() - INTERVAL 18 DAY, NOW() - INTERVAL 3 DAY),
-- 수락된 거래 (Switch, 예약중)
(4, 6, 2, 'RESERVED', 380000, false, false, NOW() - INTERVAL 5 DAY, NOW() - INTERVAL 2 DAY),
-- 대기 중 거래 요청 (헤드폰)
(3, 5, 2, 'PENDING', 280000, false, false, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),
-- 취소된 거래
(9, 7, 4, 'CANCELLED', 180000, false, false, NOW() - INTERVAL 10 DAY, NOW() - INTERVAL 8 DAY),
-- 완료된 거래 (MacBook)
(2, 6, 2, 'COMPLETED', 1050000, true, true, NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 25 DAY);

-- ----------------------------------------------------------------
-- 6. Chat Rooms & Messages
-- ----------------------------------------------------------------
INSERT INTO chat_rooms (item_id, seller_id, buyer_id, buyer_unread_count, seller_unread_count, created_at, updated_at) VALUES
(1, 2, 5, 0, 1, NOW() - INTERVAL 8 DAY, NOW() - INTERVAL 1 DAY),   -- iPhone 채팅
(3, 2, 5, 0, 0, NOW() - INTERVAL 1 DAY, NOW() - INTERVAL 1 DAY),   -- 헤드폰 채팅 (거래 연결)
(10, 4, 6, 1, 0, NOW() - INTERVAL 4 DAY, NOW() - INTERVAL 2 DAY),  -- 허먼밀러 채팅
(2, 2, 6, 0, 0, NOW() - INTERVAL 30 DAY, NOW() - INTERVAL 25 DAY); -- MacBook 채팅 (완료)

INSERT INTO chat_messages (room_id, sender_id, content, type, created_at) VALUES
-- iPhone 채팅방 (room 1)
(1, 5, '안녕하세요! iPhone 15 Pro 아직 판매 중인가요?', 'TEXT', NOW() - INTERVAL 8 DAY),
(1, 2, '네, 판매 중입니다! 상태 정말 좋아요 😊', 'TEXT', NOW() - INTERVAL 8 DAY + INTERVAL 10 MINUTE),
(1, 5, '직거래 가능하신가요? 강남 쪽 어디서 만날 수 있을까요?', 'TEXT', NOW() - INTERVAL 7 DAY),
(1, 2, '강남역 근처에서 만나시면 좋을 것 같아요. 언제 가능하세요?', 'TEXT', NOW() - INTERVAL 7 DAY + INTERVAL 5 MINUTE),
(1, 5, '이번 주말 오전에 가능한데 혹시 100만원에 안 되실까요?', 'TEXT', NOW() - INTERVAL 6 DAY),
(1, 2, '최저가가 110만원이에요. 5천원만 더 할인해드릴 수 있어요!', 'TEXT', NOW() - INTERVAL 6 DAY + INTERVAL 15 MINUTE),
(1, 5, '넵 알겠습니다 주말에 연락드릴게요!', 'TEXT', NOW() - INTERVAL 5 DAY),
(1, 2, '좋아요! 연락 주세요 📱', 'TEXT', NOW() - INTERVAL 5 DAY + INTERVAL 5 MINUTE),

-- 헤드폰 채팅방 (room 2)
(2, 5, '헤드폰 혹시 최저가가 어떻게 되나요?', 'TEXT', NOW() - INTERVAL 1 DAY),
(2, 2, '25만원까지는 가능해요!', 'TEXT', NOW() - INTERVAL 1 DAY + INTERVAL 10 MINUTE),
(2, 5, '좋아요. 구매 요청 드릴게요!', 'TEXT', NOW() - INTERVAL 1 DAY + INTERVAL 20 MINUTE),

-- 허먼밀러 채팅방 (room 3)
(3, 6, '안녕하세요~ 허먼밀러 체어 상태 사진 더 있으신가요?', 'TEXT', NOW() - INTERVAL 4 DAY),
(3, 4, '네! 추가 사진 촬영해서 보내드릴게요.', 'TEXT', NOW() - INTERVAL 4 DAY + INTERVAL 30 MINUTE),
(3, 4, '등받이 사진입니다. 기능 모두 정상이에요.', 'IMAGE', NOW() - INTERVAL 3 DAY),
(3, 6, '감사합니다. 직거래 가능하신 날짜가 있으신가요?', 'TEXT', NOW() - INTERVAL 2 DAY),
(3, 4, '다음 주 토요일 오후 가능합니다! 수원역 근처에서 만나요.', 'TEXT', NOW() - INTERVAL 2 DAY + INTERVAL 1 HOUR),

-- MacBook 채팅방 (room 4) - 완료된 거래
(4, 6, '맥북 관심 있어요! 직거래 어디서 하시나요?', 'TEXT', NOW() - INTERVAL 30 DAY),
(4, 2, '강남역 스타벅스에서 만나요!', 'TEXT', NOW() - INTERVAL 30 DAY + INTERVAL 10 MINUTE),
(4, 6, '거래 완료! 감사합니다 상태 정말 좋네요 👍', 'TEXT', NOW() - INTERVAL 25 DAY),
(4, 2, '좋은 거래 감사합니다 😊 리뷰도 부탁드려요!', 'TEXT', NOW() - INTERVAL 25 DAY + INTERVAL 5 MINUTE);

-- ----------------------------------------------------------------
-- 7. Reviews (완료된 거래에 대해)
-- ----------------------------------------------------------------
INSERT INTO reviews (trade_id, reviewer_id, reviewee_id, rating, content, created_at) VALUES
-- 요가매트 거래 (trade 1) - 구매자→판매자
(1, 5, 3, 5, '판매자분이 정말 친절하시고 상품 상태도 설명 그대로였어요. 다음에도 꼭 이용하고 싶습니다! 강력 추천 드립니다 👍', NOW() - INTERVAL 2 DAY),
-- 요가매트 거래 (trade 1) - 판매자→구매자
(1, 3, 5, 5, '시간 약속 잘 지켜주시고 깔끔하게 거래 잘 됐습니다. 좋은 구매자분이에요!', NOW() - INTERVAL 2 DAY + INTERVAL 1 HOUR),
-- MacBook 거래 (trade 5) - 구매자→판매자
(5, 6, 2, 4, '제품 상태 좋고 거래 빠르게 됐습니다. 충전기 케이블이 살짝 휘어있었지만 기능에는 문제 없어요. 전반적으로 만족합니다.', NOW() - INTERVAL 24 DAY),
-- MacBook 거래 (trade 5) - 판매자→구매자
(5, 2, 6, 5, '매너 좋으시고 약속 장소에 정시에 오셨습니다. 좋은 분이에요!', NOW() - INTERVAL 24 DAY + INTERVAL 2 HOUR);

-- ----------------------------------------------------------------
-- 8. Notifications
-- ----------------------------------------------------------------
INSERT INTO notifications (user_id, type, message, link, is_read, created_at) VALUES
-- buyer1 알림
(5, 'TRADE_REQUESTED', '헤드폰에 구매 요청이 들어왔습니다.', '/trades', false, NOW() - INTERVAL 1 DAY),
(5, 'REVIEW_RECEIVED', '김전자님이 리뷰를 남겼습니다.', '/profile', true, NOW() - INTERVAL 2 DAY),

-- seller1 (김전자) 알림
(2, 'TRADE_REQUESTED', '최구매님이 Sony 헤드폰 구매를 요청했습니다.', '/trades', false, NOW() - INTERVAL 1 DAY),
(2, 'CHAT_MESSAGE', '최구매님이 메시지를 보냈습니다.', '/chat', true, NOW() - INTERVAL 6 DAY),
(2, 'REVIEW_RECEIVED', '정쇼퍼님이 리뷰를 남겼습니다.', '/users/2/reviews', true, NOW() - INTERVAL 24 DAY),

-- seller2 (이패션) 알림
(3, 'REVIEW_RECEIVED', '최구매님이 거래 리뷰를 남겼습니다.', '/users/3/reviews', true, NOW() - INTERVAL 2 DAY),

-- buyer2 알림
(6, 'TRADE_ACCEPTED', '닌텐도 스위치 OLED 거래가 수락되었습니다.', '/trades', false, NOW() - INTERVAL 2 DAY);

SET FOREIGN_KEY_CHECKS = 1;

SELECT '✅ 더미 데이터 삽입 완료!' AS result;
SELECT CONCAT('users: ', COUNT(*)) AS summary FROM users
UNION ALL SELECT CONCAT('items: ', COUNT(*)) FROM items
UNION ALL SELECT CONCAT('item_images: ', COUNT(*)) FROM item_images
UNION ALL SELECT CONCAT('wishlists: ', COUNT(*)) FROM wishlists
UNION ALL SELECT CONCAT('trades: ', COUNT(*)) FROM trades
UNION ALL SELECT CONCAT('chat_rooms: ', COUNT(*)) FROM chat_rooms
UNION ALL SELECT CONCAT('chat_messages: ', COUNT(*)) FROM chat_messages
UNION ALL SELECT CONCAT('reviews: ', COUNT(*)) FROM reviews
UNION ALL SELECT CONCAT('notifications: ', COUNT(*)) FROM notifications;
