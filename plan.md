# plan.md — Tradev 구현 계획

> **작성일:** 2026-05-23  
> **연관 문서:** [PRD.md](PRD.md) · [Architecture.md](Architecture.md) · [DB Schema.md](DB%20Schema.md) · [API Spec.md](API%20Spec.md)  
> **진행 방식:** 각 태스크 완료 시 `[ ]` → `[x]` 로 변경

---

## 구현 원칙

1. **백엔드 우선** — API가 완성된 후 프론트엔드를 붙인다. 병렬 작업 가능하나 API 계약(API Spec)을 기준으로 Mock 활용.
2. **수직 슬라이스** — 도메인 하나를 엔티티→Repository→Service→Controller→프론트까지 완성한 뒤 다음으로 넘어간다.
3. **테스트 가능한 단위** — 각 Phase 종료 시 실제 동작 확인 가능한 상태여야 한다.
4. **의존 순서 준수** — User → Item → Trade → Reservation → Chat → Notification → Review → Report → AI → Admin 순서로 구현한다.
5. **설계 문서 우선** — 구현 중 스펙 충돌 발생 시 이 문서에 기록하고 설계 문서를 업데이트한다.

---

## Phase 0 — 설계 문서 ✅

- [x] PRD.md
- [x] UserFlow.md
- [x] Architecture.md
- [x] DB Schema.md
- [x] API Spec.md
- [x] UI Spec.md
- [x] Error Spec.md
- [x] Env Spec.md
- [x] plan.md
- [x] CLAUDE.md

---

## Phase 1 — 프로젝트 초기 세팅

### 1-A. 백엔드 프로젝트 초기화

- [x] Spring Initializr로 프로젝트 생성
  - Java 17, Spring Boot 3.x, Gradle
  - 의존성: Spring Web, Spring Security, Spring Data JPA, Spring Data Redis,
    Validation, OAuth2 Client, WebSocket, Mail, Actuator, Lombok
- [x] `build.gradle` 의존성 추가
  - `jjwt` (JWT), `aws-java-sdk-s3` (S3), `querydsl` (동적 쿼리), `p6spy` (슬로우 쿼리)
- [x] 패키지 구조 생성 (`com.tradev` 하위 — Architecture.md 3.2 참고)
- [x] `application.yml` / `application-local.yml` / `application-prod.yml` 작성 (Env Spec.md 4항 참고)
- [x] `backend/.env.example` → `backend/.env.local` 생성 및 값 채우기
- [x] `GlobalExceptionHandler` + `ErrorCode` enum + `ApiResponse<T>` 공통 클래스 작성
- [x] `LoggingAspect` (AOP 기반 요청/응답 로깅) 작성
- [x] Actuator 헬스체크 엔드포인트 확인 (`GET /actuator/health`)

### 1-B. 프론트엔드 프로젝트 초기화

- [x] `npm create vite@latest frontend -- --template vue` 실행
- [x] 의존성 설치: `vue-router`, `pinia`, `axios`, `tailwindcss`, `@stomp/stompjs`
- [x] Tailwind CSS 설정 (`tailwind.config.js`, Pretendard 폰트 CDN)
- [x] `frontend/.env.local` / `frontend/.env.production` 생성 (Env Spec.md 3.3 참고)
- [x] `src/api/axios.js` — baseURL, 인터셉터 (토큰 자동 재발급) 작성
- [x] `src/router/index.js` — 라우트 목록 + 네비게이션 가드 작성
- [x] `AppHeader.vue` / `AppFooter.vue` / `BottomNavigation.vue` 레이아웃 작성
- [x] `useToast.js` composable + `ToastNotification.vue` 작성
- [x] 공통 컴포넌트: `BaseButton`, `BaseInput`, `BaseModal`, `TradeStatusBadge`

### 1-C. 로컬 Docker 환경

- [x] `docker-compose.local.yml` 작성 (MySQL, Redis, MinIO, Mailpit)
- [x] `docker-compose up -d` 기동 확인
- [x] MinIO 버킷 `tradev-local` 생성 + public read 정책 설정
- [x] Mailpit Web UI 접속 확인 (`http://localhost:8025`)
- [x] MySQL 접속 확인 + `tradev` 스키마 생성

---

## Phase 1 — 코어 구현

### 1-1. 카테고리

**백엔드:**
- [x] `Category` 엔티티 (자기참조, DB Schema.md 3.2 참고)
- [x] `CategoryRepository` (JPA)
- [x] `CategoryService.getTree()` — 대분류+중분류 트리 반환
- [x] `GET /api/categories` 컨트롤러
- [x] 시드 데이터 SQL 작성 (`resources/db/migration/V1__init_categories.sql`)

**프론트엔드:**
- [x] `api/item.js` — `getCategories()` 함수
- [x] 카테고리 트리 데이터 앱 초기화 시 로드 (Pinia store 또는 composable)

---

### 1-2. 회원 인증

**백엔드:**
- [x] `User` 엔티티 (DB Schema.md 3.1 참고), `@Where(deleted_at IS NULL)` 적용
- [x] `UserRepository` (이메일/닉네임 중복 확인 쿼리 포함)
- [x] `JwtUtil` — 토큰 생성/파싱/검증 (HS256, `jjwt` 라이브러리)
- [x] `JwtAuthenticationFilter` — 요청마다 토큰 파싱 → `SecurityContextHolder`
- [x] `SecurityConfig` — URL별 접근 권한, CORS 설정, CSRF 비활성화
- [x] `RateLimitFilter` — Redis 기반 IP당 100 req/min
- [x] `POST /api/auth/signup` — 회원가입 (BCrypt 해싱)
- [x] `POST /api/auth/login` — 로그인 (Access Token 응답 + Refresh Token HttpOnly Cookie)
- [x] `POST /api/auth/refresh` — Refresh Token Rotation (Redis 조회/갱신)
- [x] `POST /api/auth/logout` — Redis에서 Refresh Token 삭제
- [x] `GET /api/auth/check-email` / `GET /api/auth/check-nickname` — 중복 확인
- [x] `POST /api/auth/email-verify` / `POST /api/auth/email-verify/resend` — 이메일 인증 (Redis TTL 24h)
- [x] `POST /api/auth/password-reset/request` / `confirm` — 비밀번호 재설정
- [x] Google OAuth2 연동 (`CustomOAuth2UserService`, 신규 가입/기존 사용자 분기)
- [x] `TrustScoreService.updateGrade()` — User.addTrustScore()로 통합 구현

**프론트엔드:**
- [x] `stores/auth.js` — Pinia (user, accessToken, isLoggedIn, isAdmin)
- [x] `api/auth.js` — signup, login, logout, refresh, checkEmail, checkNickname
- [x] `LoginView.vue` — 이메일/비밀번호 + Google 로그인 버튼
- [x] `SignupView.vue` — 실시간 중복 확인(디바운스 500ms), 비밀번호 강도 표시
- [x] `EmailVerifyView.vue` — 재발송 카운트다운 타이머
- [x] 네비게이션 가드 — `requiresAuth` / `guestOnly` 처리

---

### 1-3. 사용자 / 프로필

**백엔드:**
- [x] `GET /api/users/{userId}` — 공개 프로필 조회
- [x] `GET /api/users/me` — 내 정보 조회
- [x] `PUT /api/users/me` — 프로필 수정 (S3 이미지 업로드 포함)
- [x] `DELETE /api/users/me` — 회원 탈퇴 (soft delete)

**프론트엔드:**
- [x] `ProfileView.vue` — 프로필 헤더, 판매 상품 탭
- [x] `ProfileEditView.vue` — 닉네임/소개/이미지 수정 폼
- [x] `TrustScoreBadge.vue` — 등급 아이콘 + 점수 표시 (TrustGrade enum에 통합)

---

### 1-4. 이미지 업로드 (S3 Presigned URL)

**백엔드:**
- [x] `S3Config` — `S3Client` 빈 설정 (로컬: MinIO endpoint 분기)
- [x] `S3Service` — Presigned URL 생성, 파일 삭제
- [x] `POST /api/items/images/presigned` — 최대 10개 URL 일괄 발급
- [x] 파일 타입/크기 검증 (`FILE_INVALID_TYPE`, `FILE_SIZE_EXCEEDED`)

**프론트엔드:**
- [x] `useImageUpload.js` composable
  - Presigned URL 발급 → S3 직접 PUT → s3Key 수집
  - 개별 파일 프로그레스 표시
  - 5MB 초과 사전 차단 (업로드 전 `File.size` 확인)
- [ ] 드래그&드롭 이미지 정렬 (첫 번째 = 썸네일)

---

### 1-5. 상품 CRUD

**백엔드:**
- [x] `Item` 엔티티 + `ItemImage` 엔티티 (DB Schema.md 3.3, 3.4 참고)
- [x] `ItemRepository` + `ItemMapper` (MyBatis 동적 검색 쿼리)
- [x] FULLTEXT 인덱스 설정 (`ngram_token_size=2` my.cnf 확인)
- [x] `POST /api/items` — 상품 등록 (이미지 S3 키 → ItemImage 저장)
- [x] `GET /api/items` — 목록 조회 (커서 페이지네이션, 필터, 정렬, 키워드 검색)
- [x] `GET /api/items/{itemId}` — 상세 조회 (viewCount 비동기 +1)
- [x] `PUT /api/items/{itemId}` — 수정 (상태 검증, 이미지 교체)
- [x] `DELETE /api/items/{itemId}` — 삭제 (거래 진행 중 방지)
- [x] `POST /api/items/{itemId}/boost` — 끌어올리기 (하루 1회 Redis TTL 제한)
- [x] `PATCH /api/items/{itemId}/visibility` — 숨기기/공개

**프론트엔드:**
- [x] `ItemListView.vue` — 필터 사이드바(데스크톱), 무한 스크롤
- [x] `ItemCard.vue` — 썸네일, 상태 오버레이, 찜 버튼(낙관적 업데이트)
- [x] `ItemDetailView.vue` — 갤러리, 판매자 정보, 하단 액션바
- [x] `ItemFormView.vue` — 등록/수정 공용, 이미지 업로드, 페이지 이탈 경고
- [x] `InfiniteScrollList.vue` — Intersection Observer 기반
- [ ] `ItemFilter.vue` — 카테고리/가격/거래방식/상태 필터 (ItemListView에 통합)

---

### 1-6. 관심 상품 (Wishlist)

**백엔드:**
- [x] `Wishlist` 엔티티 (UNIQUE: user_id + item_id)
- [x] `POST /api/items/{itemId}/wishlist` — 토글 (등록/취소)
  - `items.wish_count` 동기화 (트랜잭션 내)
- [x] `GET /api/users/me/wishlist` — 관심 목록 (커서 페이지네이션)

**프론트엔드:**
- [x] 찜 버튼 낙관적 업데이트 (즉시 UI 반영 → 실패 시 롤백)
- [x] `WishlistView.vue`

---

## Phase 2 — 거래 / 예약 / 실시간

### 2-1. 거래 상태 머신

**백엔드:**
- [x] `Trade` 엔티티 (`@Version` 낙관적 잠금 포함, DB Schema.md 3.6 참고)
- [x] `TradeStatus` enum + 허용 전이 맵 정의
- [x] `TradeStateMachine.transition(trade, newStatus, actor)` — 전이 검증 + 부수 효과 발행
- [x] `POST /api/trades` — 구매 요청 (SALE 상태 검증, 진행 중 요청 중복 방지)
- [x] `GET /api/trades` — 목록 (역할/상태 필터, 커서 페이지네이션)
- [x] `GET /api/trades/{tradeId}` — 상세
- [x] `PATCH /api/trades/{tradeId}/accept` — 수락 (판매자만, `item.status → RESERVED`)
- [x] `PATCH /api/trades/{tradeId}/reject` — 거절
- [x] `PATCH /api/trades/{tradeId}/confirm` — 완료 확인 (양측 플래그, 모두 true 시 COMPLETED 전이)
  - COMPLETED 전이 시: 신뢰 점수 +5 (양측), 리뷰 작성 알림 발행
- [x] `PATCH /api/trades/{tradeId}/cancel` — 취소 (슬롯 해제 포함)

**프론트엔드:**
- [x] `TradeListView.vue` — 구매/판매 탭, 상태 필터
- [x] `TradeCard.vue` — 수락/거절/완료/취소 인라인 액션
- [ ] 거래 상세 모달 (진행 타임라인, 확인/취소 버튼)

---

### 2-2. 예약 슬롯

**백엔드:**
- [x] `TimeSlot` 엔티티 (`@Version`, UNIQUE: seller_id + started_at)
- [x] `Reservation` 엔티티 + `ReservationStatus` enum
- [x] `SlotLockService` — Redis `SETNX` 기반 5분 임시 잠금
  - `tryLock(slotId, userId)`, `unlock(slotId)`, `isLockedBy(slotId, userId)`
- [x] `GET /api/slots/seller/{sellerId}?year=&month=` — 월별 슬롯 목록
- [x] `GET /api/slots/seller/{sellerId}/available` — 예약 가능 슬롯 목록
- [x] `POST /api/slots` — 일괄 등록
- [x] `DELETE /api/slots/{slotId}` — 삭제 (AVAILABLE 상태만)
- [x] `POST /api/reservations` — 예약 요청 (Redis 잠금 확인 → DB Pending)
- [x] `PATCH /api/reservations/{id}/accept` — 수락 (판매자)
- [x] `PATCH /api/reservations/{id}/complete` — 완료
- [x] `PATCH /api/reservations/{id}/cancel` — 취소 → 슬롯 해제
- [x] `GET /api/reservations/me` — 내 예약 목록 (커서 페이지네이션)
- [x] `ReservationReminderScheduler` — 24h 리마인더 크론

**프론트엔드:**
- [x] `SlotManageView.vue` — 판매자 월별 슬롯 관리 페이지
- [ ] `SlotCalendar.vue` — 캘린더 뷰 (색상 구분)
- [ ] `SlotReserveModal.vue` — 구매자 예약 요청 모달

---

### 2-3. 채팅 (WebSocket)

**백엔드:**
- [x] `ChatRoom`, `ChatMessage`, `MessageType` 엔티티
- [x] `WebSocketConfig` — STOMP 설정, `JwtHandshakeInterceptor` (쿼리 파라미터 토큰 검증)
- [x] `ChatWebSocketHandler` — STOMP 메시지 수신 처리
- [x] `POST /api/chat/rooms` — 채팅방 생성/조회 (UNIQUE: item_id + buyer_id)
- [x] `GET /api/chat/rooms` — 내 채팅 목록 (updatedAt 정렬)
- [x] `GET /api/chat/rooms/{roomId}/messages` — 메시지 목록 (커서 페이지네이션)
- [x] `POST /api/chat/rooms/{roomId}/messages` — 메시지 전송 (REST fallback)
- [x] `PATCH /api/chat/rooms/{roomId}/read` — 읽음 처리
- [x] `JwtAuthenticationFilter` — 쿼리 파라미터 토큰도 지원 (SSE/WS용)

**프론트엔드:**
- [x] `useWebSocket.js` composable — STOMP/SockJS 연결, 자동 재연결
- [x] `ChatListView.vue` — 채팅 목록, 안읽음 뱃지
- [x] `ChatRoomView.vue` — 메시지 말풍선, 스크롤, 읽음 처리
- [x] `ChatInput.vue` — Shift+Enter 줄바꿈, Enter 전송
- [ ] `PriceOfferCard.vue` — 가격 제안 카드
- [ ] 상품 미니 배너 (채팅방 상단)

---

### 2-4. 알림 (SSE)

**백엔드:**
- [x] `Notification` 엔티티 + `NotificationType` enum (12가지 타입)
- [x] `SseEmitterService` — `ConcurrentHashMap<Long, SseEmitter>` 관리 (30분 타임아웃)
- [x] `GET /api/notifications/subscribe` — SSE 연결 엔드포인트
- [x] `NotificationEventListener` — `@EventListener` + `@Async` (Trade 5가지 이벤트)
- [x] `GET /api/notifications` — 목록 (커서 페이지네이션)
- [x] `GET /api/notifications/unread-count` — 미읽음 수
- [x] `PATCH /api/notifications/{id}/read` — 단건 읽음
- [x] `PATCH /api/notifications/read-all` — 전체 읽음
- [x] `NotificationCleanupScheduler` — 매일 03시 30일 지난 읽은 알림 삭제
- [x] Trade 이벤트 클래스 5개 생성 (TradeService 컴파일 의존)

**프론트엔드:**
- [x] `useSse.js` composable — EventSource + 지수 백오프, 토큰 쿼리 파라미터
- [x] `stores/notification.js` — 알림 목록, 미읽음 수
- [x] `NotificationDropdown.vue` — 헤더 드롭다운 (최근 10건, 타입별 이모지)
- [x] 로그인 시 SSE 자동 연결 (App.vue)
- [ ] `NotificationView.vue` 전체 알림 무한 스크롤

---

## Phase 3 — 나머지 기능 / AI / 배포

### 3-1. 리뷰

**백엔드:**
- [x] `Review` 엔티티 (UNIQUE: trade_id + reviewer_id)
- [x] `POST /api/reviews` — 작성 (COMPLETED 거래, 7일 이내, 중복 방지)
  - 작성 시: 신뢰 점수 +2 (작성자), 피작성자에게 알림
- [x] `GET /api/users/{userId}/reviews` — 받은 리뷰 목록 (평균 별점 포함)
- [x] `POST /api/reviews/{id}/reply` — 판매자 답글 (피작성자만, 1회)

**프론트엔드:**
- [x] `ReviewForm.vue` — 별점 + 텍스트 모달
- [x] `ReviewCard.vue` — 리뷰 + 답글 표시
- [x] 거래 완료 후 리뷰 유도 팝업 (TradeCard.vue — "후기 쓰기" 버튼)

---

### 3-2. 신고

**백엔드:**
- [x] `Report` 엔티티 (다형성 참조: target_type + target_id)
- [x] `POST /api/reports` — 신고 접수 (본인 신고 방지, 중복 신고 방지)

**프론트엔드:**
- [x] `ReportModal.vue` (상품 상세 / 프로필 페이지에서 접근)
- [x] 신고 유형 선택 + 상세 내용 입력

---

### 3-3. AI 기능 (Claude API)

**백엔드:**
- [x] `ClaudeWebClient` — `WebClient` 기반 비동기 HTTP 클라이언트
  - API Key 헤더, anthropic-version 헤더, 30초 타임아웃
- [x] `AiRateLimitService` — Redis `ai:daily:{userId}:{date}` 카운터, 10회/일 제한
- [x] `ItemDescriptionService.generate(title, categoryName)` — 설명 자동완성
  - 스트리밍 응답 (`stream: true`) → SSE로 클라이언트 전달
- [x] `PriceRecommendationService.recommend(title, category, description)` — 가격 추천
- [x] `FraudDetectionService.detectAsync(item)` — 사기 패턴 감지 (`@Async`)
  - 이상 감지 시 관리자에게 알림 생성
- [x] `GET /api/ai/item-description` — 스트리밍 응답 (text/event-stream)
- [x] `POST /api/ai/price-recommendation`
- [x] 상품 등록 시 `FraudDetectionService` 비동기 실행

**프론트엔드:**
- [x] `AiDescriptionAssist.vue`
  - Fetch Streams API SSE 수신 → 타이핑 효과 렌더링
  - 로딩/완료/에러 상태, 일일 한도 초과 안내
- [x] `ItemFormView.vue`에 `AiDescriptionAssist` 컴포넌트 삽입
- [ ] 가격 추천 결과 툴팁 표시 (ItemFormView)

---

### 3-4. 관리자 페이지

**백엔드:**
- [x] `AdminController` — ROLE_ADMIN 권한 체크 (`/admin/api/**`)
- [x] `GET /admin/api/dashboard` — 오늘 통계 (가입자, 거래, 신고, 상품)
- [x] `GET /admin/api/users` — 회원 목록 (커서 페이지네이션)
- [x] `PATCH /admin/api/users/{id}/suspend` — 계정 정지
- [x] `PATCH /admin/api/users/{id}/activate` — 계정 활성화
- [x] `GET /admin/api/reports` — 신고 목록 (상태 필터)
- [x] `POST /admin/api/reports/{id}/process` — 신고 처리 (ACCEPTED/REJECTED)
- [x] `POST /admin/api/reports/{id}/summary` — AI 신고 내용 요약 (Claude API)
- [ ] `GET /admin/api/items` + `PATCH .../visibility` + `DELETE` — 상품 관리
- [ ] `GET/POST/PUT/DELETE /admin/api/categories` — 카테고리 관리
- [ ] `GET /admin/api/stats` — 거래 통계 (기간별)

**프론트엔드 (Admin):**
- [x] `AdminDashboardView.vue` — 통계 카드 4개
- [x] `AdminUsersView.vue` — 회원 목록/정지/활성화
- [x] `AdminReportsView.vue` — 신고 처리 + AI 요약 버튼
- [ ] 관리자 상품 관리 테이블
- [ ] 카테고리 관리 (트리 뷰)

---

### 3-5. CI/CD 파이프라인

**GitHub Actions:**
- [x] `.github/workflows/ci.yml` — PR 시 자동 테스트 (JDK 17, MySQL/Redis 서비스, ./gradlew test)
- [x] `.github/workflows/deploy.yml` — main 머지 시 Blue-Green 자동 배포
  - test → build (bootJar + Docker push) → SSH deploy → health check → nginx 전환
- [x] `Dockerfile` 작성 (multi-stage: build → runtime, non-root user, JVM flags)
- [x] `docker-compose.yml` (운영) — app + mysql + redis + nginx, json-file 로깅
- [ ] GitHub Secrets 전체 등록 (Env Spec.md 6.2 참고) — 수동 작업
- [ ] Docker Hub 저장소 생성 (`tradev-app`) — 수동 작업

**Dockerfile 구조:**
```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

### 3-6. EC2 운영 배포

- [x] `docker-compose.yml` — 운영 컴포즈 파일 (app/mysql/redis/nginx)
- [x] `nginx/nginx.conf` — 메인 Nginx 설정 (gzip, rate limit, json 로깅)
- [x] `nginx/conf.d/tradev.conf` — SSL + SSE/WS 특수 설정, rate limit 분리
- [x] `mysql/.env.mysql` — MySQL 환경 변수 템플릿
- [ ] EC2 인스턴스 생성 (t3.small, Ubuntu 22.04) — 수동 작업
- [ ] 보안 그룹 설정 (80/443 공개, 22 관리자 IP만) — 수동 작업
- [ ] Let's Encrypt SSL 인증서 발급 (`certbot --nginx`) — 수동 작업
- [ ] `backend/.env.prod` 최초 수동 생성 — 수동 작업
- [ ] 첫 배포 후 `GET /actuator/health` 응답 확인 — 수동 작업
- [ ] 도메인 DNS 설정 (A 레코드 → EC2 IP) — 수동 작업

---

### 3-7. 모니터링 / 마무리

- [x] `logback-spring.xml` — JSON 구조화 로그 (prod/test 프로파일), logstash-logback-encoder
  - `MdcLoggingFilter` — traceId + userId 자동 주입, X-Trace-Id 응답 헤더
- [x] `spy.properties` — P6Spy 슬로우 쿼리 로깅 (≥ 1000ms, logLevel=warn)
- [x] `application-test.yml` — CI 테스트 프로파일 (create-drop, H2 대신 MySQL)
- [x] Docker 로그 드라이버 설정 (`json-file`, max-size: 100m) — docker-compose.yml
- [x] `build.gradle` — `logstash-logback-encoder:7.4` 의존성 추가
- [ ] `GET /actuator/health` — DB + Redis 연결 상태 포함 확인 (배포 후)
- [ ] 최종 E2E 시나리오 테스트 (배포 후)
  - 시나리오 A: 신규 판매자 → 상품 등록 → 슬롯 등록
  - 시나리오 B: 구매자 → 탐색 → 채팅 → 예약 → 거래 완료 → 리뷰
  - 시나리오 C: 관리자 → 신고 처리

---

## 스펙 변경 이력

> 구현 중 설계와 달라진 부분을 기록한다.

| 날짜 | 변경 항목 | 이유 | 영향 문서 |
|------|----------|------|----------|
| — | — | — | — |

---

## 알려진 기술 결정 사항

| 항목 | 결정 | 이유 |
|------|------|------|
| 관리자 프론트엔드 | 구현 시작 전 Vue 분리 빌드 vs Thymeleaf 중 선택 | 공수 대비 효과 판단 필요 |
| 채팅 메시지 영속화 | MySQL (Redis 캐시 보조) | 이력 보존 필요 |
| 슬롯 잠금 | Redis SETNX + DB Unique 2중 | Race Condition 방어 |
| 커서 페이지네이션 기준 | `created_at + id` 복합 | 동일 시각 데이터 중복/누락 방지 |
| AI 스트리밍 | Server-Sent Events (SSE) | WebSocket 불필요, 단방향으로 충분 |

---

> **다음 단계:** `CLAUDE.md` — Claude Code 작업 가이드, 코딩 컨벤션, 자주 쓰는 커맨드
