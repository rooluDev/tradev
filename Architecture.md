# Architecture — Tradev (중고거래 / 예약 플랫폼)

> **문서 버전:** v1.0.0  
> **작성일:** 2026-05-23  
> **연관 문서:** [PRD.md](PRD.md) · [UserFlow.md](UserFlow.md)

---

## 목차

1. [시스템 전체 구성도](#1-시스템-전체-구성도)
2. [인프라 아키텍처](#2-인프라-아키텍처)
3. [백엔드 아키텍처](#3-백엔드-아키텍처)
   - 3.1 레이어드 아키텍처
   - 3.2 패키지 구조
   - 3.3 도메인 모듈 구성
4. [프론트엔드 아키텍처](#4-프론트엔드-아키텍처)
   - 4.1 컴포넌트 구조
   - 4.2 상태 관리 (Pinia)
   - 4.3 라우팅
5. [데이터 아키텍처](#5-데이터-아키텍처)
   - 5.1 MySQL 역할
   - 5.2 Redis 역할
6. [실시간 통신 아키텍처](#6-실시간-통신-아키텍처)
   - 6.1 WebSocket (채팅)
   - 6.2 SSE (알림)
7. [인증 / 보안 아키텍처](#7-인증--보안-아키텍처)
8. [AI 연동 아키텍처](#8-ai-연동-아키텍처)
9. [이미지 업로드 플로우](#9-이미지-업로드-플로우)
10. [핵심 도메인 로직](#10-핵심-도메인-로직)
    - 10.1 거래 상태 머신
    - 10.2 예약 슬롯 잠금
11. [CI/CD 파이프라인](#11-cicd-파이프라인)
12. [환경 구성](#12-환경-구성)

---

## 1. 시스템 전체 구성도

```
┌─────────────────────────────────────────────────────────────────┐
│                          Client Side                            │
│                                                                 │
│   ┌─────────────────┐          ┌──────────────────────┐        │
│   │  Vue.js 3 SPA   │          │  Admin MPA (Thymeleaf│        │
│   │  (사용자 페이지)  │          │  또는 Vue 분리 빌드)  │        │
│   └────────┬────────┘          └──────────┬───────────┘        │
└────────────┼──────────────────────────────┼────────────────────┘
             │ HTTPS                        │ HTTPS
             ▼                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      AWS EC2 (t3.micro/small)                   │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    Nginx (Reverse Proxy)                  │  │
│  │  - SSL Termination (Let's Encrypt)                        │  │
│  │  - /api/** → Spring Boot :8080                           │  │
│  │  - /ws/**  → Spring Boot :8080 (WebSocket Upgrade)       │  │
│  │  - /       → Vue SPA (정적 파일 서빙)                     │  │
│  │  - /admin  → Admin 페이지                                 │  │
│  │  - Rate Limit: 100 req/min per IP                        │  │
│  └────────────────────────┬─────────────────────────────────┘  │
│                           │                                     │
│  ┌────────────────────────▼─────────────────────────────────┐  │
│  │              Spring Boot Application :8080                │  │
│  │                                                           │  │
│  │  ┌─────────┐  ┌─────────┐  ┌──────────┐  ┌──────────┐  │  │
│  │  │REST API │  │WebSocket│  │   SSE    │  │Scheduler │  │  │
│  │  │Controller│  │Handler  │  │Controller│  │(예약알림) │  │  │
│  │  └────┬────┘  └────┬────┘  └────┬─────┘  └────┬─────┘  │  │
│  │       └────────────┴────────────┴──────────────┘        │  │
│  │                         │                                │  │
│  │              ┌──────────▼──────────┐                    │  │
│  │              │    Service Layer     │                    │  │
│  │              └──────────┬──────────┘                    │  │
│  │                         │                                │  │
│  │         ┌───────────────┼───────────────┐               │  │
│  │         ▼               ▼               ▼               │  │
│  │   ┌──────────┐   ┌──────────┐   ┌──────────┐           │  │
│  │   │   JPA    │   │  Redis   │   │ Claude   │           │  │
│  │   │Repository│   │ Client   │   │ API      │           │  │
│  │   └────┬─────┘   └────┬─────┘   └────┬─────┘           │  │
│  └────────┼──────────────┼──────────────┼──────────────────┘  │
│           │              │              │                       │
│   ┌───────▼──────┐ ┌─────▼──────┐      │ (외부)               │
│   │  MySQL 8.0   │ │  Redis 7.x │      ▼                      │
│   │  :3306       │ │  :6379     │  Anthropic API               │
│   └──────────────┘ └────────────┘  (claude-sonnet-4-6)         │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                           │
                    ┌──────▼──────┐
                    │   AWS S3    │
                    │ (이미지 저장)│
                    └─────────────┘
```

---

## 2. 인프라 아키텍처

### 2.1 Docker Compose 구성

```
docker-compose.yml
├── nginx          (80, 443 → 8080)
├── app            (Spring Boot :8080)
├── mysql          (:3306, 볼륨 마운트)
└── redis          (:6379, 볼륨 마운트)
```

```yaml
# 서비스 의존 관계
nginx → app
app   → mysql
app   → redis
```

**Blue-Green 무중단 배포 전략:**

```
[GitHub Actions Push]
        │
        ▼
  Docker Hub에 새 이미지 Push (app:green)
        │
        ▼
  EC2: docker-compose up app-green
  (포트 8081로 신규 컨테이너 기동)
        │
        ▼
  Nginx: upstream을 8081(green)로 reload
        │
        ▼
  기존 app-blue 컨테이너 제거
        │
        ▼
  다음 배포 시 app-blue가 신규, app-green이 구버전
```

### 2.2 네트워크 구성

```
인터넷 → EC2 Public IP
  EC2 Security Group:
    - 80 (HTTP)   → Nginx
    - 443 (HTTPS) → Nginx
    - 22 (SSH)    → 관리자 IP만 허용

  내부 Docker Network (bridge):
    nginx → app:8080
    app   → mysql:3306
    app   → redis:6379
    (mysql, redis 포트는 외부 노출 없음)
```

### 2.3 AWS 서비스 연동

| 서비스 | 용도 | 비고 |
|--------|------|------|
| EC2 t3.small | 애플리케이션 서버 | Docker Compose 운영 |
| S3 | 상품 이미지 저장 | Presigned URL 방식 업로드 |
| SES / SMTP | 이메일 발송 | 회원가입 인증, 비밀번호 재설정 |

---

## 3. 백엔드 아키텍처

### 3.1 레이어드 아키텍처

```
┌──────────────────────────────────────────────┐
│           Presentation Layer                  │
│  RestController / WebSocketHandler /          │
│  SseController / AdminController (MPA)        │
└──────────────────┬───────────────────────────┘
                   │ DTO
┌──────────────────▼───────────────────────────┐
│             Application Layer                 │
│  @Service — 트랜잭션 경계, 도메인 조합,        │
│             이벤트 발행, AI 호출 오케스트레이션 │
└──────────────────┬───────────────────────────┘
                   │ Domain Object / Command
┌──────────────────▼───────────────────────────┐
│              Domain Layer                     │
│  Entity, Value Object, 상태 머신,             │
│  도메인 이벤트, 비즈니스 규칙                  │
└──────────────────┬───────────────────────────┘
                   │
┌──────────────────▼───────────────────────────┐
│           Infrastructure Layer                │
│  JpaRepository, RedisTemplate,               │
│  S3Client, ClaudeApiClient,                  │
│  EmailSender, EventPublisher                 │
└──────────────────────────────────────────────┘
```

### 3.2 패키지 구조

```
com.tradev
├── TradevApplication.java
│
├── common/
│   ├── config/          # SecurityConfig, WebSocketConfig, RedisConfig, S3Config
│   ├── exception/       # GlobalExceptionHandler, CustomException, ErrorCode
│   ├── response/        # ApiResponse<T>, PageResponse<T>
│   ├── util/            # JwtUtil, SlugUtil
│   └── aop/             # LoggingAspect, PerformanceAspect
│
├── auth/
│   ├── controller/      # AuthController, OAuth2Controller
│   ├── service/         # AuthService, TokenService, OAuth2UserService
│   ├── dto/             # LoginRequest, SignupRequest, TokenResponse
│   ├── entity/          # RefreshToken (Redis Hash)
│   └── filter/          # JwtAuthenticationFilter
│
├── user/
│   ├── controller/      # UserController, ProfileController
│   ├── service/         # UserService, TrustScoreService
│   ├── dto/
│   └── entity/          # User
│
├── item/
│   ├── controller/      # ItemController, ItemSearchController
│   ├── service/         # ItemService, ItemSearchService, WishlistService
│   ├── dto/
│   ├── entity/          # Item, ItemImage, Category, Wishlist
│   └── repository/      # ItemRepository, ItemRepositoryCustom (QueryDSL/MyBatis)
│
├── trade/
│   ├── controller/      # TradeController
│   ├── service/         # TradeService, TradeStateMachine
│   ├── dto/
│   └── entity/          # Trade, TradeStatus (enum)
│
├── reservation/
│   ├── controller/      # SlotController, ReservationController
│   ├── service/         # SlotService, ReservationService, SlotLockService
│   ├── dto/
│   └── entity/          # TimeSlot, Reservation
│
├── chat/
│   ├── controller/      # ChatRoomController, ChatMessageController
│   ├── handler/         # ChatWebSocketHandler
│   ├── service/         # ChatService, ChatMessageService
│   ├── dto/
│   └── entity/          # ChatRoom, ChatMessage
│
├── notification/
│   ├── controller/      # NotificationController, SseController
│   ├── service/         # NotificationService, SseEmitterService
│   ├── dto/
│   ├── entity/          # Notification, NotificationType (enum)
│   └── event/           # NotificationEvent, NotificationEventListener
│
├── review/
│   ├── controller/      # ReviewController
│   ├── service/         # ReviewService
│   ├── dto/
│   └── entity/          # Review
│
├── report/
│   ├── controller/      # ReportController
│   ├── service/         # ReportService
│   ├── dto/
│   └── entity/          # Report, ReportType (enum), ReportStatus (enum)
│
├── ai/
│   ├── service/         # ClaudeApiService, ItemDescriptionService, FraudDetectionService
│   ├── dto/             # ClaudeRequest, ClaudeResponse
│   └── client/          # ClaudeWebClient
│
├── admin/
│   ├── controller/      # AdminDashboardController, AdminUserController,
│   │                    # AdminItemController, AdminReportController
│   ├── service/         # AdminService, AdminStatsService
│   └── dto/
│
└── scheduler/
    └── ReservationReminderScheduler.java
```

### 3.3 도메인 모듈 구성

```
도메인 간 의존 관계 (단방향 원칙)

notification ◄── trade
notification ◄── reservation
notification ◄── chat
notification ◄── review
notification ◄── report

trade ──────────► item (상태 변경)
trade ──────────► user (신뢰 점수)
reservation ────► item (슬롯 조회)
review ─────────► trade (완료 여부 확인)
report ─────────► item / user (신고 대상)

ai ─────────────► item (설명 생성, 사기 감지)
ai ─────────────► report (신고 요약)
```

---

## 4. 프론트엔드 아키텍처

### 4.1 컴포넌트 구조

```
src/
├── main.js
├── App.vue
│
├── router/
│   └── index.js          # Vue Router 설정, 네비게이션 가드
│
├── stores/               # Pinia
│   ├── auth.js           # 사용자 인증 상태, Access Token
│   ├── notification.js   # 알림 목록, 읽지 않은 수
│   ├── chat.js           # 채팅방 목록, 현재 채팅방
│   └── item.js           # 상품 목록 필터/정렬 상태
│
├── api/                  # Axios 인스턴스 및 API 모듈
│   ├── axios.js          # baseURL, 인터셉터 (토큰 갱신)
│   ├── auth.js
│   ├── item.js
│   ├── trade.js
│   ├── reservation.js
│   ├── chat.js
│   ├── notification.js
│   ├── review.js
│   └── report.js
│
├── composables/          # 재사용 로직
│   ├── useAuth.js        # 로그인 상태 확인, 로그아웃
│   ├── useInfiniteScroll.js  # 커서 기반 무한 스크롤
│   ├── useSse.js         # SSE 연결 / 재연결 관리
│   ├── useWebSocket.js   # WebSocket 연결 관리
│   └── useToast.js       # 토스트 메시지
│
├── views/                # 페이지 단위 컴포넌트
│   ├── HomeView.vue
│   ├── auth/
│   │   ├── LoginView.vue
│   │   ├── SignupView.vue
│   │   └── EmailVerifyView.vue
│   ├── item/
│   │   ├── ItemListView.vue
│   │   ├── ItemDetailView.vue
│   │   ├── ItemFormView.vue      # 등록/수정 공용
│   │   └── ItemSearchView.vue
│   ├── trade/
│   │   └── TradeListView.vue
│   ├── chat/
│   │   ├── ChatListView.vue
│   │   └── ChatRoomView.vue
│   ├── notification/
│   │   └── NotificationView.vue
│   ├── profile/
│   │   ├── ProfileView.vue
│   │   ├── ProfileEditView.vue
│   │   ├── WishlistView.vue
│   │   └── SlotManageView.vue
│   └── error/
│       ├── NotFoundView.vue
│       └── ForbiddenView.vue
│
└── components/           # 재사용 UI 컴포넌트
    ├── layout/
    │   ├── AppHeader.vue
    │   ├── AppFooter.vue
    │   └── AppSidebar.vue
    ├── item/
    │   ├── ItemCard.vue
    │   ├── ItemFilter.vue
    │   ├── ItemGallery.vue
    │   └── AiDescriptionAssist.vue
    ├── trade/
    │   ├── TradeStatusBadge.vue
    │   └── TradeRequestModal.vue
    ├── reservation/
    │   ├── SlotCalendar.vue
    │   └── SlotReserveModal.vue
    ├── chat/
    │   ├── ChatMessage.vue
    │   ├── PriceOfferCard.vue
    │   └── ChatInput.vue
    ├── notification/
    │   └── NotificationDropdown.vue
    ├── review/
    │   ├── ReviewCard.vue
    │   └── ReviewForm.vue
    └── common/
        ├── BaseModal.vue
        ├── BaseButton.vue
        ├── BaseInput.vue
        ├── TrustScoreBadge.vue
        └── InfiniteScrollList.vue
```

### 4.2 상태 관리 (Pinia)

```
┌──────────────────────────────────────────────┐
│                auth store                     │
│  state: { user, accessToken, isLoggedIn }     │
│  actions: login(), logout(), refreshToken()   │
│  persist: localStorage (user 정보만)           │
└──────────────────────────────────────────────┘

┌──────────────────────────────────────────────┐
│             notification store                │
│  state: { notifications[], unreadCount }      │
│  actions: fetchAll(), markRead(), markAllRead │
│  SSE 이벤트 수신 시 자동 업데이트             │
└──────────────────────────────────────────────┘

┌──────────────────────────────────────────────┐
│                chat store                     │
│  state: { rooms[], currentRoom, messages[] }  │
│  actions: connectWs(), sendMessage()          │
│  WebSocket 메시지 수신 시 자동 업데이트        │
└──────────────────────────────────────────────┘
```

### 4.3 라우팅 및 네비게이션 가드

```javascript
// 접근 제어 규칙
router.beforeEach((to, from) => {
  const auth = useAuthStore()

  // 로그인 필요 페이지 → 비로그인 시 /login 리다이렉트
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 관리자 전용 → 일반 사용자 접근 시 403
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { path: '/403' }
  }

  // 로그인 상태에서 /login, /signup 접근 → 홈 리다이렉트
  if (to.meta.guestOnly && auth.isLoggedIn) {
    return { path: '/' }
  }
})
```

---

## 5. 데이터 아키텍처

### 5.1 MySQL 역할 (영속 데이터)

```
영속 저장 대상:
  - 사용자 (users)
  - 상품 / 이미지 / 카테고리 (items, item_images, categories)
  - 거래 (trades)
  - 예약 슬롯 / 예약 (time_slots, reservations)
  - 채팅방 / 메시지 (chat_rooms, chat_messages)
  - 알림 (notifications)
  - 리뷰 (reviews)
  - 신고 (reports)
  - 관심 상품 (wishlists)

성능 전략:
  - 커서 기반 페이지네이션 (LIMIT + WHERE id < ?)
  - 복합 인덱스: (category_id, status, created_at), (seller_id, status)
  - 슬로우 쿼리 로깅: ≥ 1,000ms
  - 복잡 검색 쿼리: MyBatis 동적 쿼리 사용
```

### 5.2 Redis 역할 (임시 / 캐시 데이터)

| Key 패턴 | 타입 | TTL | 용도 |
|----------|------|-----|------|
| `refresh:{userId}` | String | 7일 | Refresh Token 저장 (Rotation) |
| `slot:lock:{slotId}` | String | 5분 | 예약 슬롯 임시 잠금 |
| `chat:recent:{roomId}` | List | 1시간 | 최근 메시지 캐시 (50건) |
| `rate:limit:{ip}` | String | 1분 | API Rate Limit 카운터 |
| `email:verify:{token}` | String | 24시간 | 이메일 인증 토큰 |
| `password:reset:{token}` | String | 1시간 | 비밀번호 재설정 토큰 |

---

## 6. 실시간 통신 아키텍처

### 6.1 WebSocket (채팅)

```
[Vue Client]                    [Spring Boot]
     │                               │
     │ WS Connect                    │
     │ ws://host/ws/chat/{roomId}    │
     │──────────────────────────────►│
     │                               │ JwtHandshakeInterceptor
     │                               │ (쿼리 파라미터 토큰 검증)
     │◄──────────────────────────────│
     │ Connection Established        │
     │                               │
     │ SEND /app/chat.send           │
     │ { roomId, content, type }     │
     │──────────────────────────────►│
     │                               │ ChatWebSocketHandler
     │                               │ → DB 저장
     │                               │ → SUBSCRIBE 구독자에게 브로드캐스트
     │                               │ → SSE 알림 발행 (상대방 오프라인)
     │◄──────────────────────────────│
     │ MESSAGE /topic/chat/{roomId}  │
     │                               │

WebSocket 메시지 타입:
  TEXT      — 일반 텍스트 메시지
  IMAGE     — 이미지 URL 메시지
  PRICE_OFFER — 가격 제안 카드
  PRICE_ACCEPT / PRICE_REJECT — 가격 제안 응답
  TRADE_REQUEST — 거래 요청 알림

Fallback:
  WebSocket 연결 불가 시 → HTTP 폴링 (5초 간격, 개발 환경 호환성)
```

### 6.2 SSE (알림)

```
[Vue Client]                    [Spring Boot]
     │                               │
     │ GET /api/notifications/stream │
     │ Authorization: Bearer {token} │
     │──────────────────────────────►│
     │                               │ SseEmitter 생성 (timeout: 10분)
     │                               │ emitterMap에 userId로 저장
     │◄──────────────────────────────│
     │ text/event-stream             │
     │                               │
     │          [이벤트 발생]          │
     │◄──────────────────────────────│
     │ data: {"type":"NEW_CHAT",      │
     │        "message":"...",        │
     │        "url":"/chats/123"}     │
     │                               │

SseEmitterService:
  - ConcurrentHashMap<Long, SseEmitter> emitterMap
  - 이벤트 발행: notifyUser(userId, event)
  - 타임아웃 / 에러 시 emitter 자동 제거

재연결 전략 (클라이언트):
  EventSource 끊김 감지
  → 1s → 2s → 4s → 8s (최대 30s) Exponential Backoff 재연결
  → 재연결 성공 시 REST API로 미수신 알림 조회
```

---

## 7. 인증 / 보안 아키텍처

### 7.1 JWT 인증 플로우

```
[클라이언트]                     [서버]
     │                               │
     │ POST /api/auth/login           │
     │──────────────────────────────►│
     │                               │ 자격증명 검증
     │                               │ Access Token 생성 (30분, HS256)
     │                               │ Refresh Token 생성 (UUID, 7일)
     │                               │ Redis: refresh:{userId} = token
     │◄──────────────────────────────│
     │ { accessToken }               │
     │ Set-Cookie: refreshToken      │
     │ (HttpOnly, Secure, SameSite)  │
     │                               │
     │ API 요청 (Authorization: Bearer accessToken)
     │──────────────────────────────►│
     │                               │ JwtAuthenticationFilter
     │                               │ SecurityContextHolder 설정
     │                               │
     │ [401 Unauthorized]            │ (Access Token 만료)
     │◄──────────────────────────────│
     │                               │
     │ POST /api/auth/refresh         │
     │ Cookie: refreshToken          │
     │──────────────────────────────►│
     │                               │ Redis 조회 + 검증
     │                               │ 새 Access Token + Refresh Token 발급
     │                               │ (Rotation: 기존 Refresh Token 폐기)
     │◄──────────────────────────────│
     │ { accessToken }               │
```

### 7.2 Spring Security 필터 체인

```
Request
  │
  ├── CorsFilter               (CORS 정책: 허용 Origin만)
  ├── RateLimitFilter          (Redis 기반, 100 req/min per IP)
  ├── JwtAuthenticationFilter  (토큰 파싱 → SecurityContext)
  ├── ExceptionTranslationFilter
  └── FilterSecurityInterceptor (URL 권한 매핑)

URL 권한 설정:
  PUBLIC:  GET /api/items/**, GET /api/users/:id/profile
  AUTH:    /api/trades/**, /api/chats/**, /api/reservations/**
  ADMIN:   /admin/**
```

### 7.3 OAuth2 Google 연동

```
[클라이언트]          [Spring Boot]          [Google]
     │                     │                     │
     │ GET /oauth2/google   │                     │
     │────────────────────►│                     │
     │◄────────────────────│                     │
     │ Redirect to Google  │                     │
     │──────────────────────────────────────────►│
     │◄──────────────────────────────────────────│
     │ Authorization Code  │                     │
     │────────────────────►│                     │
     │                     │ Exchange Code ──────►│
     │                     │◄────────────────────│
     │                     │ Google AccessToken   │
     │                     │ (Google UserInfo 조회)│
     │                     │                     │
     │                     │ DB: 사용자 조회/생성
     │                     │ JWT 발급
     │◄────────────────────│
     │ Redirect + Token    │
```

---

## 8. AI 연동 아키텍처

### 8.1 Claude API 연동 구조

```
┌─────────────────────────────────────────────┐
│              ClaudeWebClient                 │
│                                             │
│  - WebClient (Reactor 기반 비동기)           │
│  - Base URL: https://api.anthropic.com      │
│  - Header: x-api-key, anthropic-version     │
│  - Timeout: 30초                            │
└──────────────────┬──────────────────────────┘
                   │
        ┌──────────┴───────────────┐
        ▼                          ▼
┌─────────────────┐    ┌──────────────────────┐
│ItemDescription  │    │  FraudDetection       │
│Service          │    │  Service              │
│                 │    │                       │
│ - 제목+카테고리  │    │ - 상품 설명 입력       │
│   → 설명 초안   │    │ - 사기 패턴 의심 여부  │
│ - 스트리밍 응답  │    │ - 비동기 (@Async)     │
│   (SSE via API) │    │ - 감지 시 Admin 알림  │
└─────────────────┘    └──────────────────────┘

추가 AI 기능:
  PriceRecommendationService  — 가격 추천
  ReportSummaryService        — 신고 내용 요약 (관리자)
```

### 8.2 AI 요청/응답 흐름 (설명 자동완성)

```
[Vue: 상품 등록 폼]
        │ "AI 설명 자동완성" 클릭
        ▼
POST /api/ai/item-description
{ title: "...", categoryId: 1 }
        │
        ▼
[ItemDescriptionService]
  프롬프트 구성:
    system: "당신은 중고거래 플랫폼의 상품 설명 작성 전문가입니다..."
    user: "제목: {title}, 카테고리: {category}에 맞는 상품 설명을 작성해주세요."
        │
        ▼
[Claude API — streaming: true]
        │
        ▼ (Server-Sent Events 스트리밍)
[클라이언트] 텍스트 점진적 렌더링
  → 완성 후 textarea에 자동 입력
  → 사용자가 직접 수정 가능
```

---

## 9. 이미지 업로드 플로우

```
[Vue Client]              [Spring Boot]            [AWS S3]
     │                         │                       │
     │ POST /api/items/images/presigned
     │ { fileName, contentType }
     │────────────────────────►│                       │
     │                         │ S3 Presigned URL 생성  │
     │                         │ (PUT, TTL 5분)         │
     │                         │──────────────────────►│
     │                         │◄──────────────────────│
     │◄────────────────────────│                       │
     │ { uploadUrl, fileKey }  │                       │
     │                         │                       │
     │ PUT {uploadUrl}          │                       │
     │ (이미지 바이너리 직접 전송)│                       │
     │──────────────────────────────────────────────── ►│
     │◄────────────────────────────────────────────────│
     │ 200 OK                  │                       │
     │                         │                       │
     │ POST /api/items (상품 등록)
     │ body: { ..., imageKeys: ["key1", "key2"] }
     │────────────────────────►│                       │
     │                         │ DB: item_images 저장   │
     │                         │ URL: CDN or S3 public URL
     │◄────────────────────────│

이미지 URL 형식: https://s3.{region}.amazonaws.com/{bucket}/{key}
썸네일: 첫 번째 이미지가 자동 지정
파일 크기 제한: 클라이언트 사전 검증 (5MB 초과 시 업로드 전 거부)
```

---

## 10. 핵심 도메인 로직

### 10.1 거래 상태 머신

```java
// TradeStateMachine 핵심 설계

enum TradeStatus {
    SALE, PENDING, RESERVED, COMPLETED, CANCELLED, REJECTED
}

// 허용된 상태 전이 맵
Map<TradeStatus, Set<TradeStatus>> transitions = {
    SALE     → { PENDING, CANCELLED },
    PENDING  → { RESERVED, REJECTED, CANCELLED },
    RESERVED → { COMPLETED, CANCELLED },
    COMPLETED → {},  // 종료 상태
    CANCELLED → {}, // 종료 상태
    REJECTED  → {}  // 종료 상태
}

// 상태 전이 시 부수 효과
SALE → PENDING   : 판매자에게 알림 발송
PENDING → RESERVED  : 구매자에게 수락 알림, 슬롯 확정
PENDING → REJECTED  : 구매자에게 거절 알림
RESERVED → COMPLETED: 양측 신뢰 점수 +5, 리뷰 작성 유도 알림
RESERVED → CANCELLED: 상대방에게 취소 알림, 슬롯 해제
```

**낙관적 잠금 (Optimistic Lock):**

```java
@Entity
public class Trade {
    @Version
    private Long version;  // 동시 상태 변경 감지
    // ...
}

// 충돌 발생 시 (두 사용자가 동시에 상태 변경 시도)
// → ObjectOptimisticLockingFailureException
// → 409 Conflict 응답
// → 클라이언트: 페이지 새로고침 유도
```

### 10.2 예약 슬롯 잠금

```
슬롯 중복 예약 방지 — 2중 방어:

① Redis 임시 잠금 (5분 TTL)
   SET slot:lock:{slotId} {userId} NX EX 300
   → NX: 키가 없을 때만 설정 (원자적)
   → 이미 잠금된 경우 → 409 "다른 사용자가 예약 중입니다"

② DB 유니크 제약
   reservations 테이블:
   UNIQUE KEY (time_slot_id, status)
     WHERE status = 'CONFIRMED'
   → 잠금 TTL 내 두 요청이 동시 도달하는 극단적 상황 방어

③ 낙관적 잠금
   TimeSlot.version으로 동시 수정 감지

타임라인:
  T+0:00  구매자 A가 슬롯 선택 → Redis 잠금 획득
  T+0:01  구매자 B가 동일 슬롯 선택 → Redis 잠금 실패 (409)
  T+4:59  판매자가 수락 → 예약 확정, Redis 키 삭제
  T+5:00  TTL 만료 (수락 없을 시) → Redis 키 자동 삭제 → 슬롯 다시 가용
```

---

## 11. CI/CD 파이프라인

```
[개발자 git push → main]
        │
        ▼
[GitHub Actions Workflow]
  ┌─────────────────────────────┐
  │  Job 1: test                │
  │  - JDK 17 setup             │
  │  - ./gradlew test           │
  │  - 테스트 실패 시 파이프라인 중단│
  └────────────┬────────────────┘
               │ 성공
  ┌────────────▼────────────────┐
  │  Job 2: build               │
  │  - ./gradlew bootJar        │
  │  - Docker build (app image) │
  │  - Docker push → Docker Hub │
  └────────────┬────────────────┘
               │
  ┌────────────▼────────────────┐
  │  Job 3: deploy              │
  │  - EC2 SSH 접속             │
  │  - docker-compose pull      │
  │  - Blue-Green 전환          │
  │  - Health Check             │
  │    GET /actuator/health     │
  │    → 200 OK 확인 후 완료    │
  └─────────────────────────────┘

환경 변수 관리:
  GitHub Secrets → EC2 환경 변수 (.env 파일 생성)
  민감 정보: DB 패스워드, JWT Secret, Claude API Key, AWS 키
```

---

## 12. 환경 구성

### 12.1 환경별 설정 분리

```
application.yml          # 공통 설정
application-local.yml    # 로컬 개발 (H2 또는 로컬 MySQL)
application-dev.yml      # EC2 개발 서버
application-prod.yml     # EC2 운영 서버
```

### 12.2 로컬 개발 환경

```yaml
# docker-compose.local.yml
services:
  mysql:
    image: mysql:8.0
    ports: ["3306:3306"]
  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]
  # MinIO (S3 대체, 로컬 이미지 업로드)
  minio:
    image: minio/minio
    ports: ["9000:9000", "9001:9001"]
```

### 12.3 모니터링 / 로깅

```
로그 레벨:
  INFO  — 일반 API 요청/응답 (AOP Filter)
  WARN  — 비즈니스 규칙 위반 (잘못된 상태 전이 시도 등)
  ERROR — 예외 발생 (GlobalExceptionHandler)

로그 형식: JSON (Logback)
  { "timestamp", "level", "traceId", "userId", "method", "uri", "duration", "message" }

슬로우 쿼리: ≥ 1,000ms → WARN 레벨 로깅
Docker 로그 드라이버: json-file (max-size: 100m, max-file: 3)

헬스체크: GET /actuator/health
  → DB 연결, Redis 연결 상태 포함
```

---

> **다음 단계:** `DB Schema.md` — ERD 및 테이블 정의, 인덱스 전략
