# DB Schema — Tradev (중고거래 / 예약 플랫폼)

> **문서 버전:** v1.0.0  
> **작성일:** 2026-05-23  
> **DBMS:** MySQL 8.0  
> **연관 문서:** [PRD.md](PRD.md) · [Architecture.md](Architecture.md)

---

## 목차

1. [ERD 개요](#1-erd-개요)
2. [테이블 목록](#2-테이블-목록)
3. [테이블 정의](#3-테이블-정의)
   - 3.1 users
   - 3.2 categories
   - 3.3 items
   - 3.4 item_images
   - 3.5 wishlists
   - 3.6 trades
   - 3.7 time_slots
   - 3.8 reservations
   - 3.9 chat_rooms
   - 3.10 chat_messages
   - 3.11 notifications
   - 3.12 reviews
   - 3.13 reports
4. [Enum 타입 정의](#4-enum-타입-정의)
5. [인덱스 전략](#5-인덱스-전략)
6. [설계 결정 사항](#6-설계-결정-사항)

---

## 1. ERD 개요

```
                          ┌──────────────┐
                          │   categories │
                          │──────────────│
                          │ id           │
                          │ parent_id ───┼──(self)
                          │ name         │
                          └──────┬───────┘
                                 │ 1
                                 │
┌──────────────┐          ┌──────▼───────┐          ┌──────────────┐
│    users     │ 1      N │    items     │ 1      N │ item_images  │
│──────────────│◄─────────│──────────────│──────────►│──────────────│
│ id           │ seller   │ id           │          │ id           │
│ email        │          │ seller_id    │          │ item_id      │
│ nickname     │          │ category_id  │          │ image_url    │
│ trust_score  │          │ title        │          │ sort_order   │
│ grade        │          │ price        │          │ is_thumbnail │
│ status       │          │ status       │          └──────────────┘
└──────┬───────┘          │ trade_type   │
       │                  │ wish_count   │
       │ 1                └──────┬───────┘
       │                         │ 1
       │    ┌────────────────────┤
       │    │                    │ 1
       │  N │                  N │
       │ ┌──▼──────────┐  ┌──────▼────────┐
       │ │  wishlists  │  │    trades     │
       │ │─────────────│  │───────────────│
       │ │ id          │  │ id            │
       │ │ user_id     │  │ item_id       │
       │ │ item_id     │  │ buyer_id  ────┼──► users
       │ └─────────────┘  │ seller_id ───┼──► users
       │                  │ status        │
       │ 1                │ version       │
       │                  └──────┬────────┘
       │                         │ 1
       │                         │
       │                  ┌──────▼────────┐       ┌──────────────┐
       │                  │ reservations  │       │  time_slots  │
       │                  │───────────────│   N   │──────────────│
       │                  │ id            │◄──────│ id           │
       │                  │ trade_id      │       │ seller_id ───┼──► users
       │                  │ slot_id       │       │ started_at   │
       │                  │ status        │       │ ended_at     │
       │                  └───────────────┘       │ status       │
       │                                          │ version      │
       │                                          └──────────────┘
       │
       │ 1                                ┌──────────────┐
       ├──────────────────────────────────► notifications│
       │                                  │──────────────│
       │                                  │ id           │
       │                                  │ receiver_id  │
       │                                  │ type         │
       │                                  │ is_read      │
       │                                  └──────────────┘
       │
       │          ┌──────────────┐
       │        N │  chat_rooms  │ 1
       ├──────────►──────────────│◄────── items
       │  buyer   │ id           │
       └──────────►──────────────│
         seller   │ buyer_id     │         ┌──────────────────┐
                  │ seller_id    │ 1     N │  chat_messages   │
                  │ item_id      │─────────►──────────────────│
                  │ last_message │         │ id               │
                  └──────────────┘         │ room_id          │
                                           │ sender_id ───────┼──► users
                                           │ type             │
                                           │ content          │
                                           │ metadata (JSON)  │
                                           └──────────────────┘

    trades 1 ──── N reviews (최대 2건: 구매자→판매자, 판매자→구매자)
    users  1 ──── N reports (신고자)
```

---

## 2. 테이블 목록

| 테이블명 | 설명 | 주요 관계 |
|---------|------|----------|
| `users` | 회원 | 모든 도메인의 중심 |
| `categories` | 상품 카테고리 | 자기 참조 (대/중분류) |
| `items` | 상품 | users(seller), categories |
| `item_images` | 상품 이미지 | items |
| `wishlists` | 관심 상품 | users ↔ items |
| `trades` | 거래 | items, users(buyer/seller) |
| `time_slots` | 직거래 예약 슬롯 | users(seller) |
| `reservations` | 예약 확정 내역 | trades, time_slots |
| `chat_rooms` | 채팅방 | items, users(buyer/seller) |
| `chat_messages` | 채팅 메시지 | chat_rooms, users |
| `notifications` | 알림 | users(receiver) |
| `reviews` | 리뷰 | trades, users |
| `reports` | 신고 | users(reporter), 대상 다형성 |

---

## 3. 테이블 정의

### 3.1 users

```sql
CREATE TABLE users (
    id                BIGINT          NOT NULL AUTO_INCREMENT,
    email             VARCHAR(255)    NOT NULL,
    password          VARCHAR(255)    NULL,           -- OAuth 사용자는 NULL
    nickname          VARCHAR(30)     NOT NULL,
    profile_image_url VARCHAR(500)    NULL,
    bio               VARCHAR(200)    NULL,
    trust_score       INT             NOT NULL DEFAULT 50,
    grade             VARCHAR(10)     NOT NULL DEFAULT 'SPROUT',
                                                      -- SEED/SPROUT/FRUIT/TREE
    role              VARCHAR(10)     NOT NULL DEFAULT 'USER',
                                                      -- USER/ADMIN
    provider          VARCHAR(10)     NOT NULL DEFAULT 'LOCAL',
                                                      -- LOCAL/GOOGLE
    provider_id       VARCHAR(255)    NULL,           -- OAuth provider의 고유 ID
    status            VARCHAR(15)     NOT NULL DEFAULT 'ACTIVE',
                                                      -- ACTIVE/SUSPENDED/WITHDRAWN
    suspended_until   DATETIME        NULL,           -- 정지 만료 일시
    email_verified    TINYINT(1)      NOT NULL DEFAULT 0,
    last_boosted_at   DATETIME        NULL,           -- 상품 끌어올리기 마지막 시각
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                               ON UPDATE CURRENT_TIMESTAMP,
    deleted_at        DATETIME        NULL,           -- 소프트 삭제

    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email    (email),
    UNIQUE KEY uq_users_nickname (nickname),
    INDEX idx_users_status       (status),
    INDEX idx_users_provider     (provider, provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**grade 자동 관리:**  
`trust_score` 변경 시 애플리케이션 레이어에서 grade 동기화.

| grade | trust_score 범위 |
|-------|----------------|
| `SEED`   | 0 ~ 29  |
| `SPROUT` | 30 ~ 59 |
| `FRUIT`  | 60 ~ 79 |
| `TREE`   | 80 ~ 100 |

---

### 3.2 categories

```sql
CREATE TABLE categories (
    id         INT          NOT NULL AUTO_INCREMENT,
    parent_id  INT          NULL,                     -- NULL이면 대분류
    name       VARCHAR(30)  NOT NULL,
    sort_order TINYINT      NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    FOREIGN KEY fk_categories_parent (parent_id)
        REFERENCES categories (id) ON DELETE RESTRICT,
    INDEX idx_categories_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**초기 시드 데이터 예시:**

```sql
-- 대분류
INSERT INTO categories VALUES
(1, NULL, '전자기기',      1),
(2, NULL, '의류',          2),
(3, NULL, '가구/인테리어', 3),
(4, NULL, '도서',          4),
(5, NULL, '스포츠/레저',   5),
(6, NULL, '티켓/쿠폰',     6),
(7, NULL, '기타',          7);

-- 중분류 (전자기기)
INSERT INTO categories VALUES
(11, 1, '스마트폰', 1),
(12, 1, '노트북',   2),
(13, 1, '태블릿',   3),
(14, 1, '카메라',   4),
(15, 1, '기타',     5);
```

---

### 3.3 items

```sql
CREATE TABLE items (
    id            BIGINT          NOT NULL AUTO_INCREMENT,
    seller_id     BIGINT          NOT NULL,
    category_id   INT             NOT NULL,
    title         VARCHAR(100)    NOT NULL,
    description   TEXT            NOT NULL,
    price         INT             NOT NULL,           -- 원 단위
    trade_type    VARCHAR(10)     NOT NULL,           -- DIRECT/DELIVERY/BOTH
    status        VARCHAR(10)     NOT NULL DEFAULT 'SALE',
                                                      -- SALE/RESERVED/COMPLETED/HIDDEN/DELETED
    view_count    INT             NOT NULL DEFAULT 0,
    wish_count    INT             NOT NULL DEFAULT 0, -- 비정규화 캐시
    boosted_at    DATETIME        NULL,               -- 끌어올리기 시각
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                           ON UPDATE CURRENT_TIMESTAMP,
    deleted_at    DATETIME        NULL,               -- 소프트 삭제 (관리자 강제 삭제)

    PRIMARY KEY (id),
    FOREIGN KEY fk_items_seller   (seller_id)   REFERENCES users      (id),
    FOREIGN KEY fk_items_category (category_id) REFERENCES categories (id),

    -- 목록 조회 최적화 (상태 + 카테고리 + 최신순)
    INDEX idx_items_status_created        (status, created_at DESC),
    INDEX idx_items_category_status       (category_id, status, created_at DESC),
    -- 판매자 상품 목록
    INDEX idx_items_seller                (seller_id, status),
    -- 끌어올리기 정렬 (boosted_at이 있으면 최상단)
    INDEX idx_items_boosted               (status, boosted_at DESC, created_at DESC),
    -- 가격 범위 필터
    INDEX idx_items_price                 (price),
    -- 인기순 정렬
    INDEX idx_items_wish_count            (status, wish_count DESC),
    -- 전문 검색 (제목 + 설명)
    FULLTEXT INDEX ft_items_search        (title, description)
        WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**`status` 전이:**

```
item.status 변화:
  SALE      → 구매 요청 수락 → RESERVED
  RESERVED  → 거래 완료     → COMPLETED
  SALE/RESERVED → 숨기기    → HIDDEN
  HIDDEN    → 공개 복원     → SALE
  임의 상태 → 관리자 강제 삭제 → DELETED (soft delete)
```

---

### 3.4 item_images

```sql
CREATE TABLE item_images (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    item_id      BIGINT        NOT NULL,
    image_url    VARCHAR(500)  NOT NULL,              -- S3 URL
    s3_key       VARCHAR(300)  NOT NULL,              -- S3 오브젝트 키 (삭제 시 사용)
    sort_order   TINYINT       NOT NULL DEFAULT 0,    -- 0이 썸네일
    is_thumbnail TINYINT(1)    NOT NULL DEFAULT 0,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    FOREIGN KEY fk_item_images_item (item_id)
        REFERENCES items (id) ON DELETE CASCADE,
    INDEX idx_item_images_item (item_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 3.5 wishlists

```sql
CREATE TABLE wishlists (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    user_id    BIGINT   NOT NULL,
    item_id    BIGINT   NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_wishlists_user_item (user_id, item_id),
    FOREIGN KEY fk_wishlists_user (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY fk_wishlists_item (item_id) REFERENCES items (id) ON DELETE CASCADE,
    INDEX idx_wishlists_item (item_id)     -- 상품 삭제 시 wish_count 업데이트용
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 3.6 trades

```sql
CREATE TABLE trades (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    item_id          BIGINT        NOT NULL,
    buyer_id         BIGINT        NOT NULL,
    seller_id        BIGINT        NOT NULL,
    status           VARCHAR(15)   NOT NULL DEFAULT 'PENDING',
                                             -- PENDING/RESERVED/COMPLETED/CANCELLED/REJECTED
    request_message  VARCHAR(500)  NULL,     -- 구매 요청 메시지
    cancel_reason    VARCHAR(300)  NULL,     -- 취소/거절 사유
    buyer_confirmed  TINYINT(1)    NOT NULL DEFAULT 0,  -- 거래 완료 확인 여부
    seller_confirmed TINYINT(1)    NOT NULL DEFAULT 0,
    version          INT           NOT NULL DEFAULT 0,  -- 낙관적 잠금
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                             ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    FOREIGN KEY fk_trades_item   (item_id)   REFERENCES items (id),
    FOREIGN KEY fk_trades_buyer  (buyer_id)  REFERENCES users (id),
    FOREIGN KEY fk_trades_seller (seller_id) REFERENCES users (id),

    -- 구매자 거래 내역
    INDEX idx_trades_buyer  (buyer_id,  status, created_at DESC),
    -- 판매자 거래 내역
    INDEX idx_trades_seller (seller_id, status, created_at DESC),
    -- 상품별 진행 중 거래 조회 (중복 요청 방지)
    INDEX idx_trades_item   (item_id,   status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**비즈니스 규칙 (애플리케이션 레이어 적용):**
- 동일 상품에 `PENDING` 또는 `RESERVED` 거래가 이미 존재하면 추가 구매 요청 불가.
- `buyer_confirmed = 1` AND `seller_confirmed = 1` → status = `COMPLETED` 자동 전이.

---

### 3.7 time_slots

```sql
CREATE TABLE time_slots (
    id         BIGINT    NOT NULL AUTO_INCREMENT,
    seller_id  BIGINT    NOT NULL,
    started_at DATETIME  NOT NULL,
    ended_at   DATETIME  NOT NULL,
    status     VARCHAR(10) NOT NULL DEFAULT 'AVAILABLE',
                                    -- AVAILABLE/LOCKED/RESERVED
    version    INT       NOT NULL DEFAULT 0,  -- 낙관적 잠금
    created_at DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    FOREIGN KEY fk_slots_seller (seller_id) REFERENCES users (id) ON DELETE CASCADE,

    -- 동일 판매자의 동일 시작 시간 중복 방지
    UNIQUE KEY uq_slots_seller_start (seller_id, started_at),
    -- 판매자 슬롯 캘린더 조회
    INDEX idx_slots_seller_time (seller_id, started_at, status),
    -- 스케줄러: 만료된 LOCKED 슬롯 정리
    INDEX idx_slots_status      (status, started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**슬롯 상태 전이:**

```
AVAILABLE ──[예약 요청]──► LOCKED     (Redis TTL 5분 + DB 반영)
LOCKED    ──[판매자 수락]──► RESERVED  (예약 확정)
LOCKED    ──[TTL 만료]────► AVAILABLE  (스케줄러 또는 요청 시 확인)
RESERVED  ──[예약 취소]───► AVAILABLE
```

---

### 3.8 reservations

```sql
CREATE TABLE reservations (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    trade_id   BIGINT      NOT NULL,
    slot_id    BIGINT      NOT NULL,
    buyer_id   BIGINT      NOT NULL,
    seller_id  BIGINT      NOT NULL,
    status     VARCHAR(10) NOT NULL DEFAULT 'PENDING',
                                    -- PENDING/CONFIRMED/CANCELLED/EXPIRED
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
                                    ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    FOREIGN KEY fk_reservations_trade  (trade_id)  REFERENCES trades     (id),
    FOREIGN KEY fk_reservations_slot   (slot_id)   REFERENCES time_slots (id),
    FOREIGN KEY fk_reservations_buyer  (buyer_id)  REFERENCES users      (id),
    FOREIGN KEY fk_reservations_seller (seller_id) REFERENCES users      (id),

    -- 슬롯당 확정된 예약은 1건만 허용 (애플리케이션 레이어 보조 적용)
    UNIQUE KEY uq_reservations_slot_confirmed (slot_id, status),  -- NOTE ①
    INDEX idx_reservations_trade  (trade_id),
    INDEX idx_reservations_buyer  (buyer_id,  status),
    INDEX idx_reservations_seller (seller_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- NOTE ①: MySQL은 partial unique index 미지원.
--   CANCELLED/EXPIRED는 동일 slot_id로 여러 건 가능하므로,
--   UNIQUE KEY (slot_id, status)는 CONFIRMED 상태에서만 중복 방지 효과.
--   (CANCELLED는 같은 slot_id + CANCELLED 복수 허용 필요 → 아래 대안 사용)
```

**대안 설계 (NOTE ① 보완):**

```sql
-- reservations 테이블에 confirmed_slot_id 컬럼 추가
ALTER TABLE reservations ADD COLUMN confirmed_slot_id BIGINT NULL;
ALTER TABLE reservations ADD UNIQUE KEY uq_confirmed_slot (confirmed_slot_id);
-- 예약 확정 시 confirmed_slot_id = slot_id 설정
-- 취소/만료 시 confirmed_slot_id = NULL
-- → CONFIRMED 상태의 슬롯 중복 방지를 DB 레벨에서 보장
```

---

### 3.9 chat_rooms

```sql
CREATE TABLE chat_rooms (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    item_id          BIGINT        NOT NULL,
    buyer_id         BIGINT        NOT NULL,
    seller_id        BIGINT        NOT NULL,
    last_message     VARCHAR(200)  NULL,    -- 목록 미리보기용 비정규화
    last_message_at  DATETIME      NULL,
    buyer_unread     INT           NOT NULL DEFAULT 0,  -- 구매자 미읽음 수
    seller_unread    INT           NOT NULL DEFAULT 0,  -- 판매자 미읽음 수
    created_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                             ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    FOREIGN KEY fk_chatrooms_item   (item_id)   REFERENCES items (id),
    FOREIGN KEY fk_chatrooms_buyer  (buyer_id)  REFERENCES users (id),
    FOREIGN KEY fk_chatrooms_seller (seller_id) REFERENCES users (id),

    -- 상품:구매자 쌍 유니크 (채팅방 중복 생성 방지)
    UNIQUE KEY uq_chatrooms_item_buyer (item_id, buyer_id),
    -- 사용자별 채팅 목록 조회
    INDEX idx_chatrooms_buyer  (buyer_id,  last_message_at DESC),
    INDEX idx_chatrooms_seller (seller_id, last_message_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 3.10 chat_messages

```sql
CREATE TABLE chat_messages (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    room_id    BIGINT        NOT NULL,
    sender_id  BIGINT        NOT NULL,
    type       VARCHAR(20)   NOT NULL DEFAULT 'TEXT',
                                      -- TEXT/IMAGE/PRICE_OFFER/PRICE_ACCEPT
                                      -- /PRICE_REJECT/TRADE_REQUEST
    content    TEXT          NOT NULL,
    metadata   JSON          NULL,    -- type별 부가 데이터 (아래 참고)
    is_read    TINYINT(1)    NOT NULL DEFAULT 0,
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    FOREIGN KEY fk_messages_room   (room_id)   REFERENCES chat_rooms (id) ON DELETE CASCADE,
    FOREIGN KEY fk_messages_sender (sender_id) REFERENCES users      (id),

    -- 채팅방 메시지 목록 (커서 기반 페이징)
    INDEX idx_messages_room (room_id, created_at DESC),
    -- 읽음 처리 배치
    INDEX idx_messages_unread (room_id, is_read, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**metadata JSON 예시:**

```json
// type = IMAGE
{ "imageUrl": "https://s3.../chat/xxx.jpg", "s3Key": "chat/xxx.jpg" }

// type = PRICE_OFFER
{ "offeredPrice": 45000, "originalPrice": 50000 }

// type = PRICE_ACCEPT / PRICE_REJECT
{ "offeredPrice": 45000, "messageId": 1234 }

// type = TRADE_REQUEST
{ "tradeId": 789 }
```

---

### 3.11 notifications

```sql
CREATE TABLE notifications (
    id          BIGINT        NOT NULL AUTO_INCREMENT,
    receiver_id BIGINT        NOT NULL,
    type        VARCHAR(30)   NOT NULL,   -- NotificationType enum (아래 참고)
    title       VARCHAR(100)  NOT NULL,
    content     VARCHAR(200)  NOT NULL,
    link_url    VARCHAR(300)  NULL,       -- 클릭 시 이동 URL
    is_read     TINYINT(1)    NOT NULL DEFAULT 0,
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    FOREIGN KEY fk_notifications_receiver (receiver_id)
        REFERENCES users (id) ON DELETE CASCADE,

    -- 사용자 알림 목록 (최근 30일, 읽지 않은 것 우선)
    INDEX idx_noti_receiver (receiver_id, is_read, created_at DESC),
    -- 30일 지난 알림 정리용 (배치)
    INDEX idx_noti_created  (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

**NotificationType 목록:**

| 값 | 설명 |
|----|------|
| `NEW_CHAT_MESSAGE`         | 새 채팅 메시지 |
| `TRADE_REQUEST_RECEIVED`   | 구매 요청 도착 |
| `TRADE_REQUEST_ACCEPTED`   | 요청 수락 |
| `TRADE_REQUEST_REJECTED`   | 요청 거절 |
| `TRADE_CONFIRM_REQUESTED`  | 거래 완료 확인 요청 |
| `TRADE_COMPLETED`          | 거래 완료 |
| `TRADE_CANCELLED`          | 거래 취소 |
| `RESERVATION_RECEIVED`     | 예약 요청 도착 |
| `RESERVATION_CONFIRMED`    | 예약 확정 |
| `RESERVATION_CANCELLED`    | 예약 취소 |
| `RESERVATION_REMINDER`     | 예약 1시간 전 리마인더 |
| `REVIEW_RECEIVED`          | 리뷰 등록 |
| `REPORT_PROCESSED`         | 신고 처리 결과 |
| `ITEM_PRICE_DROPPED`       | 관심 상품 가격 인하 |

---

### 3.12 reviews

```sql
CREATE TABLE reviews (
    id           BIGINT    NOT NULL AUTO_INCREMENT,
    trade_id     BIGINT    NOT NULL,
    reviewer_id  BIGINT    NOT NULL,   -- 작성자
    reviewee_id  BIGINT    NOT NULL,   -- 피작성자
    rating       TINYINT   NOT NULL,   -- 1 ~ 5
    content      TEXT      NULL,
    reply        TEXT      NULL,       -- 판매자 답글
    replied_at   DATETIME  NULL,
    created_at   DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP
                                       ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    FOREIGN KEY fk_reviews_trade    (trade_id)    REFERENCES trades (id),
    FOREIGN KEY fk_reviews_reviewer (reviewer_id) REFERENCES users  (id),
    FOREIGN KEY fk_reviews_reviewee (reviewee_id) REFERENCES users  (id),

    -- 동일 거래에서 동일 작성자의 중복 리뷰 방지
    UNIQUE KEY uq_reviews_trade_reviewer (trade_id, reviewer_id),
    -- 프로필 페이지: 받은 리뷰 목록
    INDEX idx_reviews_reviewee (reviewee_id, created_at DESC),
    -- 리뷰 작성 가능 여부 확인 (작성자 시점)
    INDEX idx_reviews_reviewer (reviewer_id, trade_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

### 3.13 reports

```sql
CREATE TABLE reports (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    reporter_id    BIGINT        NOT NULL,
    target_type    VARCHAR(10)   NOT NULL,   -- ITEM / USER
    target_id      BIGINT        NOT NULL,   -- item.id 또는 user.id
    reason         VARCHAR(20)   NOT NULL,
                                             -- ILLEGAL/FALSE_INFO/FRAUD/ETC
    content        TEXT          NULL,       -- 상세 내용
    status         VARCHAR(10)   NOT NULL DEFAULT 'PENDING',
                                             -- PENDING/WARNING/SUSPENDED/REJECTED
    admin_note     TEXT          NULL,       -- 관리자 처리 메모
    processed_by   BIGINT        NULL,       -- 처리 관리자 id
    processed_at   DATETIME      NULL,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    FOREIGN KEY fk_reports_reporter  (reporter_id) REFERENCES users (id),
    FOREIGN KEY fk_reports_admin     (processed_by) REFERENCES users (id),

    -- 관리자: 미처리 신고 목록 (최신순)
    INDEX idx_reports_status   (status, created_at DESC),
    -- 대상별 신고 집계
    INDEX idx_reports_target   (target_type, target_id, status),
    -- 동일 신고자의 중복 신고 확인
    INDEX idx_reports_reporter (reporter_id, target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 4. Enum 타입 정의

> JPA `@Enumerated(EnumType.STRING)` 적용. DB 컬럼은 VARCHAR로 저장.

```java
// User
enum UserRole    { USER, ADMIN }
enum UserProvider{ LOCAL, GOOGLE }
enum UserStatus  { ACTIVE, SUSPENDED, WITHDRAWN }
enum UserGrade   { SEED, SPROUT, FRUIT, TREE }

// Item
enum TradeType   { DIRECT, DELIVERY, BOTH }
enum ItemStatus  { SALE, RESERVED, COMPLETED, HIDDEN, DELETED }

// Trade
enum TradeStatus { PENDING, RESERVED, COMPLETED, CANCELLED, REJECTED }

// TimeSlot
enum SlotStatus  { AVAILABLE, LOCKED, RESERVED }

// Reservation
enum ReservationStatus { PENDING, CONFIRMED, CANCELLED, EXPIRED }

// ChatMessage
enum MessageType {
    TEXT, IMAGE, PRICE_OFFER, PRICE_ACCEPT, PRICE_REJECT, TRADE_REQUEST
}

// Notification
enum NotificationType {
    NEW_CHAT_MESSAGE, TRADE_REQUEST_RECEIVED, TRADE_REQUEST_ACCEPTED,
    TRADE_REQUEST_REJECTED, TRADE_CONFIRM_REQUESTED, TRADE_COMPLETED,
    TRADE_CANCELLED, RESERVATION_RECEIVED, RESERVATION_CONFIRMED,
    RESERVATION_CANCELLED, RESERVATION_REMINDER, REVIEW_RECEIVED,
    REPORT_PROCESSED, ITEM_PRICE_DROPPED
}

// Report
enum ReportTargetType { ITEM, USER }
enum ReportReason     { ILLEGAL, FALSE_INFO, FRAUD, ETC }
enum ReportStatus     { PENDING, WARNING, SUSPENDED, REJECTED }
```

---

## 5. 인덱스 전략

### 5.1 주요 쿼리 패턴 및 인덱스 매핑

| 쿼리 패턴 | 사용 인덱스 | 비고 |
|----------|------------|------|
| 상품 목록 (최신순, 상태 필터) | `idx_items_status_created` | 커버링 인덱스 활용 |
| 상품 카테고리 + 상태 필터 | `idx_items_category_status` | 대/중분류 동시 필터 |
| 상품 키워드 검색 | `ft_items_search` | FULLTEXT + ngram (한글 지원) |
| 상품 가격 범위 필터 | `idx_items_price` | BETWEEN 쿼리 |
| 거래 내역 조회 (구매자) | `idx_trades_buyer` | (buyer_id, status, created_at) |
| 거래 내역 조회 (판매자) | `idx_trades_seller` | (seller_id, status, created_at) |
| 알림 목록 (미읽음 우선) | `idx_noti_receiver` | (receiver_id, is_read, created_at) |
| 채팅 목록 (최신 메시지) | `idx_chatrooms_buyer/seller` | last_message_at 정렬 |
| 슬롯 캘린더 조회 | `idx_slots_seller_time` | 날짜 범위 + 판매자 |
| 프로필 리뷰 목록 | `idx_reviews_reviewee` | 피작성자 기준 최신순 |

### 5.2 커서 기반 페이지네이션

```sql
-- 상품 목록 첫 페이지 (cursor 없음)
SELECT * FROM items
WHERE status = 'SALE'
  AND deleted_at IS NULL
ORDER BY created_at DESC, id DESC
LIMIT 20;

-- 다음 페이지 (cursor: lastCreatedAt, lastId)
SELECT * FROM items
WHERE status = 'SALE'
  AND deleted_at IS NULL
  AND (created_at < ? OR (created_at = ? AND id < ?))
ORDER BY created_at DESC, id DESC
LIMIT 20;
```

### 5.3 FULLTEXT 검색 설정

```sql
-- MySQL 8.0 ngram 파서 (최소 토큰 길이 2)
-- my.cnf 설정 필요:
-- [mysqld]
-- ngram_token_size = 2
-- innodb_ft_min_token_size = 2

-- 검색 쿼리 (Boolean Mode 사용)
SELECT * FROM items
WHERE MATCH(title, description) AGAINST(? IN BOOLEAN MODE)
  AND status = 'SALE'
  AND deleted_at IS NULL
ORDER BY created_at DESC
LIMIT 20;
```

---

## 6. 설계 결정 사항

### 6.1 소프트 삭제 (Soft Delete)

```
적용 테이블: users, items
미적용: 나머지 (물리 삭제 또는 ON DELETE CASCADE)

이유:
- users.deleted_at: 개인정보 처리 방침상 즉시 삭제 불가, 30일 유예 후 배치 삭제
- items.deleted_at: 관리자 강제 비공개/삭제 시 거래 이력 보존 필요

JPA 적용:
  @Where(clause = "deleted_at IS NULL")  -- 모든 쿼리에 자동 적용
  @SQLDelete(sql = "UPDATE items SET deleted_at = NOW() WHERE id = ?")
```

### 6.2 비정규화 컬럼

| 테이블 | 컬럼 | 원본 | 이유 |
|--------|------|------|------|
| `items` | `wish_count` | `COUNT(wishlists)` | 목록 조회마다 COUNT 방지 |
| `chat_rooms` | `last_message` | `chat_messages.content` | 채팅 목록 미리보기 JOIN 방지 |
| `chat_rooms` | `buyer_unread` / `seller_unread` | `COUNT(unread messages)` | 안읽음 수 실시간 표시 |

**동기화 전략:** 이벤트 기반 업데이트 (ApplicationEvent) + 트랜잭션 내 처리.

### 6.3 낙관적 잠금 적용 대상

| 테이블 | version 컬럼 | 이유 |
|--------|-------------|------|
| `trades` | O | 동시 상태 변경 (양측 거래 완료 확인) |
| `time_slots` | O | 슬롯 잠금 경합 방지 (Redis 보조) |

### 6.4 reports 다형성 참조

```
target_type + target_id 조합으로 다형성 참조 구현.
(JPA @Any 또는 서비스 레이어에서 분기 처리)

이유: 상품 신고 / 사용자 신고를 단일 테이블로 관리해
     관리자 페이지에서 통합 목록 조회 가능.

주의: target_id에 FK 제약 미적용 (다형성 참조 특성상)
     → 대상 삭제 시 reports는 남겨두고 target 존재 여부를 서비스에서 확인.
```

### 6.5 Redis와의 역할 분리

| 데이터 | 저장소 | 이유 |
|--------|--------|------|
| Refresh Token | Redis | 빠른 조회/무효화, TTL 관리 |
| 슬롯 임시 잠금 | Redis | TTL 기반 자동 해제, 원자적 SETNX |
| 이메일 인증 토큰 | Redis | TTL 24시간, 인증 후 즉시 삭제 |
| 채팅 최근 메시지 캐시 | Redis | WebSocket 재연결 시 빠른 로드 |
| 거래/리뷰/알림 데이터 | MySQL | 영속성 보장, 이력 조회 필요 |

### 6.6 문자셋

```
전체 utf8mb4 / utf8mb4_unicode_ci 통일
- 이모지 포함 한국어 완전 지원
- 정렬/비교 시 대소문자 무시 (닉네임, 이메일)
```

---

> **다음 단계:** `API Spec.md` — REST API 엔드포인트 상세 명세, 요청/응답 스키마

