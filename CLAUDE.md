# CLAUDE.md — Tradev 프로젝트 작업 가이드

> Claude Code가 이 프로젝트에서 작업할 때 항상 참조하는 파일.  
> 설계 세부사항은 각 Spec 문서를 참조한다.

---

## 프로젝트 개요

**Tradev** — 개인 간(C2C) 중고 물품 거래 및 서비스 예약 웹 플랫폼.  
상품 등록 → 예약 → 채팅 협상 → 거래 완료 → 리뷰까지 end-to-end 거래 경험 제공.  
Claude API 기반 AI 기능(설명 자동완성, 가격 추천, 사기 감지) 포함.

**스펙 문서 위치:**
- 기획: [PRD.md](PRD.md) · [UserFlow.md](UserFlow.md)
- 설계: [Architecture.md](Architecture.md) · [DB Schema.md](DB%20Schema.md)
- API: [API Spec.md](API%20Spec.md) · [Error Spec.md](Error%20Spec.md)
- UI: [UI Spec.md](UI%20Spec.md)
- 환경: [Env Spec.md](Env%20Spec.md)
- 구현 계획: [plan.md](plan.md)

---

## 기술 스택

| 레이어 | 기술 |
|--------|------|
| 백엔드 | Java 17, Spring Boot 3.x, Gradle |
| ORM | Spring Data JPA + MyBatis (복잡 쿼리) |
| 인증 | Spring Security + JWT + Google OAuth2 |
| 실시간 | WebSocket/STOMP (채팅), SSE (알림) |
| 캐시/저장 | Redis 7.x (토큰, 슬롯 잠금, Rate Limit) |
| 스토리지 | AWS S3 (Presigned URL 방식) |
| 프론트엔드 | Vue.js 3 (Composition API), Pinia, Vite |
| 스타일 | Tailwind CSS, Pretendard 폰트 |
| DB | MySQL 8.0 |
| 인프라 | Docker Compose, GitHub Actions, Nginx, EC2 |
| AI | Anthropic Claude API (`claude-sonnet-4-6`) |

---

## 디렉토리 구조

```
reservehub/
├── backend/                  # Spring Boot
│   ├── src/main/java/com/tradev/
│   │   ├── TradevApplication.java
│   │   ├── common/           # config, exception, response, aop, util
│   │   ├── auth/             # 인증 (JWT, OAuth2)
│   │   ├── user/             # 회원, 프로필, 신뢰 점수
│   │   ├── item/             # 상품, 이미지, 카테고리, 관심상품
│   │   ├── trade/            # 거래 상태 머신
│   │   ├── reservation/      # 슬롯, 예약, 잠금
│   │   ├── chat/             # 채팅방, 메시지, WebSocket
│   │   ├── notification/     # SSE, 알림 이벤트
│   │   ├── review/           # 리뷰, 답글
│   │   ├── report/           # 신고
│   │   ├── ai/               # Claude API 연동
│   │   ├── admin/            # 관리자 API
│   │   └── scheduler/        # 예약 리마인더, 알림 정리
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-local.yml
│   │   ├── application-prod.yml
│   │   └── db/migration/     # Flyway SQL (또는 data.sql)
│   └── .env.local            # 로컬 환경 변수 (gitignore)
│
├── frontend/                 # Vue.js 3
│   ├── src/
│   │   ├── api/              # Axios 모듈
│   │   ├── composables/      # use*.js
│   │   ├── stores/           # Pinia
│   │   ├── views/            # 페이지 컴포넌트
│   │   ├── components/       # 재사용 컴포넌트
│   │   └── router/
│   └── .env.local            # 로컬 환경 변수 (gitignore)
│
├── docker-compose.yml        # 운영
├── docker-compose.local.yml  # 로컬 개발
├── nginx/nginx.conf
└── *.md                      # 설계 문서
```

---

## 로컬 개발 환경 시작

```bash
# 1. 로컬 인프라 기동 (MySQL, Redis, MinIO, Mailpit)
docker-compose -f docker-compose.local.yml up -d

# 2. 환경 변수 파일 준비
cp backend/.env.example backend/.env.local
# → backend/.env.local 에 실제 값 채우기 (Env Spec.md 3.1 참고)

# 3. 백엔드 실행
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'

# 4. 프론트엔드 실행 (별도 터미널)
cd frontend
npm install
npm run dev
```

**주요 포트:**

| 서비스 | 포트 | URL |
|--------|------|-----|
| 백엔드 | 8080 | http://localhost:8080 |
| 프론트엔드 | 5173 | http://localhost:5173 |
| MySQL | 3306 | — |
| Redis | 6379 | — |
| MinIO API | 9000 | http://localhost:9000 |
| MinIO Console | 9001 | http://localhost:9001 |
| Mailpit | 8025 | http://localhost:8025 |

---

## 자주 쓰는 커맨드

```bash
# 백엔드 테스트 실행
cd backend && ./gradlew test

# 특정 테스트 클래스만 실행
./gradlew test --tests "com.tradev.trade.TradeServiceTest"

# 백엔드 빌드 (JAR 생성)
./gradlew bootJar

# 프론트엔드 프로덕션 빌드
cd frontend && npm run build

# Docker 운영 배포 (로컬 테스트용)
docker-compose up -d --build

# 로컬 인프라 초기화 (볼륨 삭제 포함)
docker-compose -f docker-compose.local.yml down -v

# Redis CLI 접속
docker exec -it $(docker ps -q -f name=redis) redis-cli

# MySQL 접속
docker exec -it $(docker ps -q -f name=mysql) mysql -u tradev -p tradev
```

---

## 백엔드 코딩 컨벤션

### 응답 형식

**모든 API는 `ApiResponse<T>`로 감싼다.**

```java
// 성공
return ResponseEntity.ok(ApiResponse.success(data));
return ResponseEntity.status(201).body(ApiResponse.success(data));

// 성공 (응답 바디 없음)
return ResponseEntity.noContent().build();
```

### 예외 처리

**비즈니스 예외는 반드시 `TradevException(ErrorCode.xxx)`를 사용한다.**  
`GlobalExceptionHandler`가 자동으로 처리하므로 컨트롤러/서비스에서 직접 ResponseEntity 예외 반환 금지.

```java
// 올바른 예외 처리
if (!trade.getSellerId().equals(currentUserId)) {
    throw new TradevException(ErrorCode.TRADE_ACCESS_DENIED);
}

// 새 에러 코드 추가 시 ErrorCode enum에 먼저 등록 후 사용
// ErrorCode enum 위치: common/exception/ErrorCode.java
```

### 레이어 간 데이터 전달

```
Controller  ←→  DTO (Request/Response)
Service     ←→  Domain Entity / Command Object
Repository  ←→  Entity / QueryDSL Predicate
```

- Controller는 DTO만 다룬다. Entity를 직접 반환하지 않는다.
- Service에서 Entity → DTO 변환. `toResponse()` 정적 팩토리 메서드 권장.
- `@Transactional`은 Service 레이어에만 붙인다.

### 페이지네이션

**커서 기반만 사용한다. `Pageable` 오프셋 방식 사용 금지.**

```java
// 커서 파싱 유틸
CursorUtils.parse(cursor)  // "2026-05-23T10:00:00|42" → {createdAt, id}
CursorUtils.encode(entity) // Entity → cursor 문자열

// 쿼리 조건
WHERE (created_at < :createdAt OR (created_at = :createdAt AND id < :id))
ORDER BY created_at DESC, id DESC
LIMIT :size + 1  // +1로 hasNext 판단
```

### 상태 머신 전이

**거래 상태는 반드시 `TradeStateMachine`을 통해 변경한다. 직접 `setStatus()` 금지.**

```java
// 올바른 방법
tradeStateMachine.transition(trade, TradeStatus.RESERVED, currentUser);

// 금지
trade.setStatus(TradeStatus.RESERVED);  // ❌
```

### 이벤트 기반 알림

**알림은 도메인 이벤트로 발행한다. 서비스에서 `NotificationService` 직접 호출 금지.**

```java
// 올바른 방법
eventPublisher.publishEvent(new TradeAcceptedEvent(trade));

// NotificationEventListener가 비동기로 처리
// 금지
notificationService.send(userId, ...);  // ❌ 직접 호출
```

### 낙관적 잠금 처리

`ObjectOptimisticLockingFailureException`은 `GlobalExceptionHandler`가 409로 자동 처리한다.  
서비스에서 별도 catch 불필요. 단, 재시도 로직이 필요한 경우만 `@Retryable` 적용.

### 소프트 삭제

`users`, `items` 테이블은 소프트 삭제 적용.

```java
@Where(clause = "deleted_at IS NULL")  // Entity 클래스에 적용
@SQLDelete(sql = "UPDATE items SET deleted_at = NOW() WHERE id = ?")
```

### Redis Key 네이밍

```
refresh:{userId}           — Refresh Token
slot:lock:{slotId}         — 슬롯 임시 잠금
rate:limit:{ip}            — Rate Limit 카운터
email:verify:{token}       — 이메일 인증 토큰
password:reset:{token}     — 비밀번호 재설정 토큰
ai:daily:{userId}:{date}   — AI 일일 사용 카운터
```

---

## 프론트엔드 코딩 컨벤션

### 파일 네이밍

```
Views      — PascalCase + View 접미사   (ItemListView.vue)
Components — PascalCase                 (ItemCard.vue)
Composables— camelCase + use 접두사    (useWebSocket.js)
Stores     — camelCase                  (auth.js)
API 모듈   — camelCase                  (item.js)
```

### API 호출 패턴

```javascript
// api/ 모듈에 함수로 분리
// api/item.js
export const itemApi = {
  getList: (params) => api.get('/items', { params }),
  getDetail: (id)   => api.get(`/items/${id}`),
  create: (data)    => api.post('/items', data),
}

// 컴포넌트에서 사용
import { itemApi } from '@/api/item'
const { data } = await itemApi.getList({ cursor, size })
```

### 에러 처리 패턴

```javascript
// 폼 제출 에러: 인라인 표시
try {
  await authApi.signup(form)
} catch (error) {
  if (error.response?.data?.code === 'AUTH_EMAIL_DUPLICATED') {
    fieldErrors.email = '이미 사용 중인 이메일입니다.'
  } else {
    useApiError().handle(error)   // 토스트 표시
  }
}
```

### Pinia Store 패턴

```javascript
// stores/auth.js
export const useAuthStore = defineStore('auth', () => {
  const user = ref(null)
  const accessToken = ref(null)
  const isLoggedIn = computed(() => !!accessToken.value)

  function setAuth(userData, token) { ... }
  function clearAuth() { ... }

  return { user, accessToken, isLoggedIn, setAuth, clearAuth }
})
```

### 컴포넌트 작성 순서

```vue
<script setup>
// 1. import
// 2. props/emits
// 3. stores
// 4. reactive state
// 5. computed
// 6. lifecycle hooks
// 7. functions
</script>

<template>
  <!-- 단일 루트 엘리먼트 -->
</template>
```

---

## 도메인별 핵심 파일 위치

| 도메인 | 핵심 파일 | 참조 문서 |
|--------|----------|----------|
| 인증 | `auth/service/AuthService.java`, `auth/filter/JwtAuthenticationFilter.java` | API Spec.md §2 |
| 상품 | `item/service/ItemService.java`, `item/repository/ItemRepositoryCustom.java` | API Spec.md §4 |
| 거래 | `trade/service/TradeStateMachine.java` | API Spec.md §5, DB Schema.md §3.6 |
| 슬롯 | `reservation/service/SlotLockService.java` | API Spec.md §6 |
| 채팅 | `chat/handler/ChatWebSocketHandler.java` | Architecture.md §6.1 |
| 알림 | `notification/service/SseEmitterService.java`, `notification/event/` | Architecture.md §6.2 |
| AI | `ai/client/ClaudeWebClient.java`, `ai/service/ItemDescriptionService.java` | Architecture.md §8 |
| 에러 | `common/exception/ErrorCode.java`, `common/exception/GlobalExceptionHandler.java` | Error Spec.md |

---

## 새 기능 추가 체크리스트

### 백엔드 새 API 추가 시

1. `ErrorCode.java`에 필요한 에러 코드 추가
2. Entity/Repository 작성 (필요 시)
3. Service 작성 (`@Transactional` 위치 확인)
4. DTO (Request/Response) 작성
5. Controller 작성 (`@Valid`, 인증 어노테이션 확인)
6. `GlobalExceptionHandler`에 새 예외 타입이 있으면 추가
7. API Spec.md 업데이트
8. plan.md 해당 태스크 체크

### 프론트엔드 새 페이지 추가 시

1. `views/`에 `XxxView.vue` 생성
2. `router/index.js`에 라우트 등록 (meta: requiresAuth 여부)
3. `api/` 모듈에 API 함수 추가
4. `BottomNavigation.vue` / `AppHeader.vue` 수정 (네비게이션 항목)
5. UI Spec.md 참조하여 레이아웃 구현

### 새 알림 이벤트 추가 시

1. `NotificationType` enum에 타입 추가
2. 도메인 이벤트 클래스 생성 (`XxxEvent.java`)
3. 서비스에서 `eventPublisher.publishEvent()` 호출
4. `NotificationEventListener`에 `@EventListener` 메서드 추가
5. API Spec.md §8 알림 이벤트 목록 업데이트
6. 프론트엔드 `notifications/`에 아이콘/메시지 매핑 추가

---

## 주요 설계 결정 사항 요약

| 항목 | 결정 | 위치 |
|------|------|------|
| 페이지네이션 | 커서 기반 (`created_at + id`) | 모든 목록 API |
| 상태 변경 | `TradeStateMachine` 통해서만 | trade 도메인 |
| 슬롯 잠금 | Redis SETNX + DB Unique 2중 방어 | reservation 도메인 |
| 소프트 삭제 | `users`, `items`만 적용 | `@Where`, `@SQLDelete` |
| 낙관적 잠금 | `trades`, `time_slots`에 `@Version` | GlobalExceptionHandler 자동 처리 |
| 알림 발행 | ApplicationEvent → `@Async` Listener | notification/event/ |
| 이미지 업로드 | S3 Presigned URL (클라이언트 직접 PUT) | item 도메인 |
| AI 응답 | 스트리밍 SSE (`stream: true`) | ai 도메인 |
| 비밀값 | GitHub Secrets → EC2 .env.prod | Env Spec.md §7 |
| 에러 코드 | `TradevException(ErrorCode.xxx)` 통일 | Error Spec.md |

---

## 주의 사항

- **`.env.*` 파일은 절대 Git에 커밋하지 않는다.** (`.gitignore` 확인)
- **Entity를 Controller에서 직접 반환하지 않는다.** DTO로 변환 필수.
- **오프셋 페이지네이션(`Pageable`) 사용 금지.** 커서 방식만 사용.
- **`NotificationService` 직접 호출 금지.** 이벤트 발행으로만 알림 생성.
- **Admin API는 `/admin/api/` 경로, ROLE_ADMIN 체크 필수.**
- **AI API 호출 전 Redis 일일 한도 확인** (`ai:daily:{userId}:{date}`).
- **슬롯 상태 변경은 낙관적 잠금(@Version)과 Redis 잠금을 함께 사용한다.**
- **채팅 메시지 전송은 WebSocket이지만 읽음 처리는 REST API로 한다.**
- **SSE Emitter는 타임아웃/에러 시 반드시 `emitterMap`에서 제거한다.**

---

## 구현 현황

> plan.md 의 체크리스트가 원본. 여기서는 Phase 단위만 표시.

| Phase | 상태 |
|-------|------|
| Phase 0 — 설계 문서 | ✅ 완료 |
| Phase 1 — 프로젝트 세팅 | ✅ 완료 |
| Phase 1 — 코어 구현 | ✅ 완료 |
| Phase 2 — 거래/예약/실시간 | ✅ 완료 |
| Phase 3 — AI/관리자/배포 | ✅ 완료 (EC2 실제 배포는 수동) |
