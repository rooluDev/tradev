# Tradev

**상품 등록 → 예약 → 채팅 협상 → 거래 완료 → 리뷰까지, C2C 중고거래 end-to-end 플랫폼**

> Claude Code(AI 코딩 에이전트)를 활용한 **AI-assisted 풀스택 프로젝트**  
> 단순히 "AI가 코드를 써줬다"가 아닌, **기획 → 설계 → 구현 → 배포** 전 과정을 AI와 협업하여 완성한 프로젝트입니다.

---

## 프로젝트 소개

기존 중고거래 플랫폼은 상품 등록과 채팅 기능에 집중되어, **직거래 약속 잡기**, **거래 상태 추적**, **신뢰도 기반 보호** 같은 실제 거래 흐름이 분리된 경우가 많습니다.

Tradev는 거래의 처음부터 끝까지를 **하나의 서비스** 안에서 처리합니다.

```
구매자: "3인용 소파 채팅하기"
  → 1:1 채팅으로 조건 협의
  → 판매자가 등록한 캘린더 슬롯에 예약 요청
  → 양측 확인으로 거래 완료
  → 신뢰 점수 반영 + 리뷰 작성

판매자: "상품 설명을 어떻게 쓰지?"
  → AI 자동완성 버튼 클릭
  → Claude API가 제목·카테고리 기반으로 설명 초안 생성
  → 가격 추천도 AI가 제안
```

---

## 배포 URL

배포 URL : https://tradev.shop


---
## 화면

### 로그인 및 메인화면

https://github.com/user-attachments/assets/64da2f38-a9b1-426d-b1c9-11a869e28e02

## 추가 및 보기

https://github.com/user-attachments/assets/c6689a58-989f-498a-ba9a-eac9a100bfb2

### 채팅

https://github.com/user-attachments/assets/3f2aaa67-c774-42f5-bba6-bbe2c3f4a653

### 회원가입

https://github.com/user-attachments/assets/544b9399-3a1b-4129-aed8-1a65122b8155



---

## AI 활용 개발 방식 (핵심 어필 포인트)

이 프로젝트에서 가장 강조하고 싶은 부분은 **코드 자체가 아니라, AI를 도구로 활용하는 능력**입니다.

### 1. 체계적인 스펙 문서 주도 개발

코드를 한 줄 작성하기 전에 **8개의 설계 문서**를 먼저 완성하고, 이를 AI에게 컨텍스트로 제공해 구현했습니다.

```
docs/
├── PRD.md          # 기능 명세, 사용자 시나리오, 마일스톤
├── UserFlow.md     # 상세 유저 플로우 (분기 조건, 엣지 케이스)
├── Architecture.md # 시스템 구조, 실시간 통신·인증·AI 연동 흐름도
├── DB Schema.md    # 테이블 설계, 인덱스 전략, DDL
├── API Spec.md     # REST + WebSocket STOMP 전체 API 명세
├── UI Spec.md      # 페이지 레이아웃, 컴포넌트 스펙, 디자인 토큰
├── Error Spec.md   # 에러 코드 목록, 프론트 처리 방식
└── Env Spec.md     # 환경 변수 목록, 배포 가이드
```

> "어떻게 만들지"보다 **"무엇을 만들지"를 먼저 정의**하고, AI가 그 스펙을 따르도록 지시한 것이 핵심입니다.

### 2. 단계별 구현 계획 (plan.md)

무작정 구현하지 않고 **4개의 Phase**로 작업을 분할하고 도메인 의존성 순서를 설계했습니다.

```
Phase 0  설계 문서 8종 완성 (PRD, Architecture, DB Schema, API Spec 등)
Phase 1  코어 구현 (인증, 상품 CRUD, 이미지 업로드, 검색/필터)
Phase 2  거래/예약/실시간 (상태 머신, 슬롯 잠금, WebSocket 채팅, SSE 알림)
Phase 3  AI 기능 + 관리자 페이지 + CI/CD + EC2 배포
```

> User → Item → Trade → Reservation → Chat → Notification → Review → Report → AI → Admin  
> 의존 방향을 설계하고 이 순서를 AI에게 명시해 충돌 없는 코드를 생성했습니다.

### 3. CLAUDE.md를 통한 AI 행동 규칙 정의

프로젝트 루트에 `CLAUDE.md`를 두어 AI가 코드를 작성할 때 **반드시 따라야 할 규칙**을 명시했습니다.

- 모든 API 응답은 `ApiResponse<T>`로 통일 (직접 Entity 반환 금지)
- 페이지네이션은 커서 기반만 사용 (`Pageable` 오프셋 방식 사용 금지)
- 거래 상태 변경은 반드시 `TradeStateMachine`을 통해서만 처리 (`setStatus()` 직접 호출 금지)
- 알림 발행은 `ApplicationEvent → @Async Listener` 패턴만 사용 (`NotificationService` 직접 호출 금지)
- 비즈니스 예외는 `TradevException(ErrorCode.xxx)` 통일 (컨트롤러에서 직접 예외 반환 금지)
- 슬롯 상태 변경은 낙관적 잠금(@Version)과 Redis 잠금을 함께 사용
- 로그에 JWT Secret, API Key, 비밀번호 출력 절대 금지

> AI에게 자유를 주는 것이 아니라 **일관성 있는 아키텍처를 유지하도록 제약을 설계**한 점이 핵심입니다.

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Java 17, Spring Boot 3.x, Spring Security, JPA, MyBatis |
| 인증 | JWT (Access + Refresh Token Rotation), Google OAuth2 |
| 실시간 | WebSocket/STOMP (채팅), SSE (알림) |
| 캐시 | Redis 7.x (토큰, 슬롯 임시 잠금, Rate Limit) |
| 스토리지 | AWS S3 (Presigned URL 클라이언트 직접 업로드) |
| Frontend | Vue.js 3 (Composition API), Pinia, Vite, Tailwind CSS |
| DB | MySQL 8.0 |
| AI | Anthropic Claude API (claude-sonnet-4-6) |
| 인프라 | AWS EC2, Docker Compose, GitHub Actions CI/CD, Nginx |

---

## 주요 기능

### 상품 관리
- 상품 등록 / 수정 / 삭제 (멀티 이미지 업로드, 최대 10장)
- S3 Presigned URL 방식으로 클라이언트가 S3에 직접 업로드 (서버 부하 없음)
- 카테고리 · 가격 범위 · 키워드 · 거래 방식 복합 필터 검색
- 커서 기반 무한 스크롤 페이지네이션
- 관심 상품(찜) 등록/해제, 관심 수 실시간 표시
- 상품 끌어올리기 (하루 1회)

### 거래 상태 머신
- `SALE → PENDING → RESERVED → COMPLETED` 상태 전이 관리
- 낙관적 잠금(`@Version`)으로 동시 상태 변경 충돌 방지 → 409 응답 자동 처리
- 거래 완료 시 양측 신뢰 점수 +5점 자동 반영

### 예약 시스템
- 판매자가 직거래 가능 날짜/시간 슬롯을 캘린더로 등록
- 슬롯 예약 시 **Redis SETNX + DB 유니크 제약 2중 방어**로 중복 예약 방지
- 5분 TTL 임시 잠금 → 판매자 미수락 시 자동 해제
- 예약 1시간 전 SSE 리마인더 알림 (Spring Scheduler)

### 실시간 채팅
- WebSocket/STOMP 기반 1:1 채팅
- 채팅 목록: 커서 페이지네이션, 안읽음 수 뱃지, 최근 메시지 미리보기
- 연결 불가 시 REST API Fallback

### 알림 시스템 (SSE)
- 로그인 사용자에게 SSE 커넥션 유지, 이벤트 발생 시 실시간 Push
- 채팅 수신, 거래 요청/수락/거절, 예약 확정, 예약 리마인더 등 13가지 알림 이벤트
- Exponential Backoff 자동 재연결 (1s → 2s → 4s → 최대 30s)
- `ApplicationEvent → @Async EventListener` 패턴으로 서비스 간 결합도 제거

### AI 기능 (Claude API)
- **상품 설명 자동완성**: 제목 + 카테고리 입력 시 Claude API가 200자 내외 설명 초안 생성 (일일 10회)
- **가격 추천**: 카테고리 + 제목 기반 적정 가격 범위 제안
- **사기 패턴 감지**: 상품 등록 시 의심 키워드 감지 후 관리자 알림

### 신뢰 점수 시스템
- 신규 가입 50점 기본 부여
- 거래 완료 +5 / 리뷰 작성 +2 / 신고 제재 −10
- 씨앗(0~29) / 새싹(30~59) / 열매(60~79) / 나무(80~100) 등급 표시

### 관리자 페이지
- 회원 · 상품 · 신고 관리, 대시보드 통계
- 신고 접수 → 경고 / 정지 / 기각 처리

---

## 아키텍처

### 시스템 전체 구성도

```
[Vue.js SPA]  [관리자 페이지]
      │               │
      └───────┬───────┘
              │ HTTPS
              ▼
     Nginx (SSL + Reverse Proxy)
     /api/**  → Spring Boot :8080
     /ws/**   → WebSocket Upgrade
     /        → Vue SPA 정적 파일
              │
    ┌─────────┴──────────┐
    │   Spring Boot      │
    │   :8080            │
    │                    │
    │  REST API          │
    │  WebSocket Handler │
    │  SSE Controller    │
    │  Scheduler         │
    └──┬──────┬──────────┘
       │      │
   MySQL   Redis        Anthropic API
   :3306   :6379        (claude-sonnet-4-6)
       │
    AWS S3 (이미지)
```

### 거래 상태 머신

```
[판매중] ──구매 요청──▶ [요청 대기]
                              │
              판매자 수락 ─────┤───── 거절 ──▶ [거절됨]
                              ▼
                          [예약중]
                              │
                    양측 완료 확인
                              ▼
                          [거래완료] ──▶ 리뷰 작성
                                        신뢰 점수 +5

[판매중] / [예약중] ──취소──▶ [취소됨]
```

### 예약 슬롯 잠금 흐름

```
구매자가 슬롯 선택
    │
    ▼
① Redis SETNX — 5분 TTL 임시 잠금 (원자적)
    │ 이미 잠금 → 409 반환
    ▼
② 예약 요청 생성 (DB 저장)
    │
    ▼
③ 판매자 수락 (5분 이내)
    │ 미수락 시 TTL 만료 → 자동 해제 → 슬롯 복구
    ▼
④ DB 유니크 제약으로 최종 중복 방어
    │
    ▼
예약 확정 + Redis 잠금 해제
```

### CI/CD 파이프라인

```
[git push → main]
        │
        ▼
[GitHub Actions]
  Job 1: 프론트엔드 빌드 (npm run build)
  Job 2: 백엔드 빌드 (./gradlew bootJar)
         Docker 이미지 빌드 + Docker Hub Push
  Job 3: EC2 SSH 접속
         docker compose pull + 재시작
         GET /actuator/health → 200 확인 후 완료

환경 변수: GitHub Secrets → EC2 .env.prod
```

### SSE 알림 흐름

```
[도메인 서비스]
    │ eventPublisher.publishEvent(new XxxEvent(...))
    ▼
[NotificationEventListener] @Async @EventListener
    │ notificationService.createAndSend(userId, ...)
    ▼
[SseEmitterService]
    │ emitterMap.get(userId) → SseEmitter.send()
    ▼
[Vue Client] EventSource onmessage
    │ notificationStore 업데이트 → 벨 아이콘 뱃지 갱신
```

---

## 실행 방법

### 사전 요구사항

| 도구 | 버전 |
|------|------|
| Java | 17 이상 |
| Node.js | 18 이상 |
| Docker Desktop | 최신 |

### 1. 로컬 인프라 기동

```bash
docker-compose -f docker-compose.local.yml up -d
# MySQL · Redis · MinIO · Mailpit 기동
```

### 2. 환경 변수 설정

```bash
cp backend/.env.example backend/.env.local
# ANTHROPIC_API_KEY, JWT_SECRET, DB 접속 정보 등 입력 (Env Spec.md 참고)
```

### 3. 백엔드 실행

```bash
cd backend
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 4. 프론트엔드 실행

```bash
cd frontend
npm install
npm run dev
```

`http://localhost:5173` 접속

**주요 포트:**

| 서비스 | 포트 |
|--------|------|
| 프론트엔드 | 5173 |
| 백엔드 | 8080 |
| MinIO Console | 9001 |
| Mailpit | 8025 |

---

## 프로젝트 구조

```
reservehub/
├── backend/
│   └── src/main/java/com/tradev/
│       ├── common/        # GlobalExceptionHandler, ApiResponse, AOP 로깅
│       ├── auth/          # JWT, OAuth2, 토큰 재발급
│       ├── user/          # 회원 프로필, 신뢰 점수
│       ├── item/          # 상품 CRUD, 이미지, 카테고리, 관심상품
│       ├── trade/         # 거래 상태 머신 (TradeStateMachine)
│       ├── reservation/   # 슬롯 관리, 예약, Redis 잠금 (SlotLockService)
│       ├── chat/          # 채팅방, 메시지, WebSocket 핸들러
│       ├── notification/  # SSE Emitter, 알림 이벤트 리스너
│       ├── review/        # 리뷰, 답글
│       ├── report/        # 신고, 관리자 제재
│       ├── ai/            # ClaudeWebClient, 설명 자동완성, 가격 추천
│       ├── admin/         # 관리자 API, 통계
│       └── scheduler/     # 예약 리마인더
│
├── frontend/
│   └── src/
│       ├── api/           # axios 인스턴스 + 도메인별 API 모듈
│       ├── composables/   # useWebSocket, useSse, useInfiniteScroll, useToast
│       ├── stores/        # Pinia (auth, notification, chat, item)
│       ├── views/         # 페이지 컴포넌트 (item, trade, chat, reservation 등)
│       └── components/    # 재사용 컴포넌트 (ItemCard, SlotCalendar, ChatInput 등)
│
├── docs/                  # 설계 문서 8종
├── CLAUDE.md              # AI 행동 규칙 (코딩 컨벤션, 금지 패턴)
├── plan.md                # 4개 Phase 구현 체크리스트
├── docker-compose.yml     # 운영 환경
├── docker-compose.local.yml  # 로컬 개발 환경
└── nginx/nginx.conf
```

---

## 이 프로젝트에서 배운 것

### AI 협업에서 가장 중요한 것: 설계 먼저, 코드는 그 다음

AI에게 "중고거래 플랫폼 만들어줘"라고 하면 동작은 하지만 일관성 없는 코드가 나옵니다.
8개의 설계 문서를 직접 작성하며 비로소:

- **거래 상태 머신**이 왜 필요한지 (`setStatus()` 직접 호출이 왜 위험한지)
- **슬롯 잠금**에 왜 Redis + DB 2중 방어가 필요한지 (TTL 내 극단적 동시 요청)
- **SSE 알림**이 서비스에서 직접 호출이 아닌 이벤트 발행이어야 하는 이유 (도메인 결합도)
- **Presigned URL**이 왜 서버 업로드보다 나은지 (EC2 메모리/대역폭 부하)

를 깊이 이해하게 됐습니다. AI는 그 이해를 코드로 옮기는 도구였습니다.

### AI 생산성을 높이는 것은 '제약'의 설계

CLAUDE.md에 명시된 규칙들은 단순한 스타일 가이드가 아닙니다.  
"왜 이 패턴을 사용해야 하는지" 이유를 함께 적어야 AI가 비슷한 상황에서도 올바른 판단을 합니다.

```
# 나쁜 제약 (이유 없음)
"TradeStateMachine을 사용할 것"

# 좋은 제약 (이유 포함)
"거래 상태 변경은 반드시 TradeStateMachine을 통해서만 처리한다.
 직접 setStatus()를 호출하면 알림 발행, 신뢰 점수 반영, 부수 효과가
 누락되어 데이터 불일치가 발생하기 때문이다."
```

이유가 있는 제약 덕분에 AI가 예외적인 상황에서도 올바른 선택을 했고,  
개발자인 저도 도메인 로직을 더 명확히 이해하게 됐습니다.

---

## 관련 문서

| 문서 | 내용 |
|------|------|
| [PRD](PRD.md) | 기능 명세, 사용자 시나리오, 마일스톤 |
| [Architecture](Architecture.md) | 시스템 구조, 실시간 통신·인증·AI 흐름도 |
| [API Spec](API%20Spec.md) | REST + WebSocket STOMP 전체 API |
| [DB Schema](DB%20Schema.md) | 테이블 설계, DDL, Redis 키 구조 |
| [Error Spec](Error%20Spec.md) | 에러 코드 목록, 프론트 처리 방식 |
| [Plan](plan.md) | 4개 Phase 구현 체크리스트 |

---

## ERD

<img width="895" height="909" alt="스크린샷 2026-05-26 오후 5 40 09" src="https://github.com/user-attachments/assets/8104423e-7cbe-4e19-96b1-bf46d8510d19" />

