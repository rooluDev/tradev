# Env Spec — Tradev (중고거래 / 예약 플랫폼)

> **문서 버전:** v1.0.0  
> **작성일:** 2026-05-23  
> **연관 문서:** [Architecture.md](Architecture.md) · [Error Spec.md](Error%20Spec.md)

---

## 목차

1. [환경 구성 개요](#1-환경-구성-개요)
2. [환경 변수 전체 목록](#2-환경-변수-전체-목록)
   - 2.1 Spring Boot 백엔드
   - 2.2 Vue.js 프론트엔드
   - 2.3 MySQL
   - 2.4 Redis
   - 2.5 Nginx
3. [환경별 .env 파일](#3-환경별-env-파일)
   - 3.1 로컬 개발
   - 3.2 운영 (EC2)
4. [Spring Boot application.yml](#4-spring-boot-applicationyml)
5. [Docker Compose 환경 주입](#5-docker-compose-환경-주입)
6. [GitHub Actions Secrets](#6-github-actions-secrets)
7. [비밀값 관리 전략](#7-비밀값-관리-전략)
8. [환경 변수 체크리스트](#8-환경-변수-체크리스트)

---

## 1. 환경 구성 개요

### 1.1 환경 종류

| 환경 | 설명 | 인프라 |
|------|------|--------|
| `local` | 개발자 로컬 머신 | Docker Compose (MySQL, Redis, MinIO) |
| `prod` | EC2 운영 서버 | Docker Compose (MySQL, Redis) + AWS S3/SES |

> 별도 `dev`/`staging` 서버 없음 (포트폴리오 규모). 필요 시 `prod` 브랜치 분기로 대응.

### 1.2 비밀값 분류

```
🔴 Secret  — 외부 유출 시 보안 침해. 절대 코드/Git에 포함 금지.
             (DB 비밀번호, JWT Secret, API Key, OAuth Secret 등)

🟡 Config  — 환경별로 달라지는 설정. 코드에 기본값 포함 가능하나
             운영 환경에서는 환경 변수로 오버라이드.
             (DB Host, Redis Host, S3 Region 등)

🟢 Public  — 노출되어도 무방. 프론트엔드 빌드 시 번들에 포함됨.
             (API Base URL, Google Client ID 등)
```

### 1.3 환경 변수 주입 흐름

```
로컬 개발:
  .env.local 파일 → Docker Compose env_file → 컨테이너 환경 변수

운영 배포:
  GitHub Secrets → GitHub Actions → EC2 SSH → .env.prod 파일 생성
  → Docker Compose env_file → 컨테이너 환경 변수

프론트엔드 빌드:
  .env.local / .env.production → Vite 빌드 시 번들에 포함
  (VITE_ 접두사 변수만 클라이언트 코드에 노출됨)
```

---

## 2. 환경 변수 전체 목록

### 2.1 Spring Boot 백엔드

#### 서버 기본

| 변수명 | 타입 | 필수 | 기본값 | 설명 |
|--------|------|------|--------|------|
| `SERVER_PORT` | int | N | `8080` | 애플리케이션 포트 |
| `SPRING_PROFILES_ACTIVE` | string | Y | `local` | 활성 프로파일 (`local` / `prod`) |
| `APP_BASE_URL` | string | Y | — | 서버 자체 URL (이메일 링크 생성용) |
| `FRONTEND_URL` | string | Y | — | 프론트엔드 URL (CORS 허용 Origin) |

#### 데이터베이스 (MySQL)

| 변수명 | 타입 | 🔴/🟡 | 기본값 | 설명 |
|--------|------|--------|--------|------|
| `DB_HOST` | string | 🟡 | `localhost` | MySQL 호스트 |
| `DB_PORT` | int | 🟡 | `3306` | MySQL 포트 |
| `DB_NAME` | string | 🟡 | `tradev` | 데이터베이스명 |
| `DB_USERNAME` | string | 🟡 | `tradev` | DB 계정명 |
| `DB_PASSWORD` | string | 🔴 | — | DB 계정 비밀번호 |
| `DB_DDL_AUTO` | string | 🟡 | `validate` | Hibernate DDL 전략 (`create-drop` / `validate`) |
| `DB_SHOW_SQL` | boolean | 🟡 | `false` | SQL 로그 출력 여부 |

#### Redis

| 변수명 | 타입 | 🔴/🟡 | 기본값 | 설명 |
|--------|------|--------|--------|------|
| `REDIS_HOST` | string | 🟡 | `localhost` | Redis 호스트 |
| `REDIS_PORT` | int | 🟡 | `6379` | Redis 포트 |
| `REDIS_PASSWORD` | string | 🔴 | — | Redis 인증 비밀번호 (없으면 빈 값) |

#### JWT

| 변수명 | 타입 | 🔴/🟡 | 기본값 | 설명 |
|--------|------|--------|--------|------|
| `JWT_SECRET` | string | 🔴 | — | HS256 서명 키 (Base64, 최소 32자) |
| `JWT_ACCESS_EXPIRY` | long | 🟡 | `1800000` | Access Token 만료 (ms, 기본 30분) |
| `JWT_REFRESH_EXPIRY` | long | 🟡 | `604800000` | Refresh Token 만료 (ms, 기본 7일) |

#### OAuth2 (Google)

| 변수명 | 타입 | 🔴/🟡 | 설명 |
|--------|------|--------|------|
| `GOOGLE_CLIENT_ID` | string | 🔴 | Google Cloud Console에서 발급 |
| `GOOGLE_CLIENT_SECRET` | string | 🔴 | Google Cloud Console에서 발급 |
| `OAUTH2_REDIRECT_URI` | string | 🟡 | OAuth2 콜백 URL (`{APP_BASE_URL}/login/oauth2/code/google`) |

#### AWS S3 (이미지 스토리지)

| 변수명 | 타입 | 🔴/🟡 | 기본값 | 설명 |
|--------|------|--------|--------|------|
| `AWS_ACCESS_KEY_ID` | string | 🔴 | — | IAM 사용자 Access Key |
| `AWS_SECRET_ACCESS_KEY` | string | 🔴 | — | IAM 사용자 Secret Key |
| `AWS_REGION` | string | 🟡 | `ap-northeast-2` | S3 리전 (서울) |
| `S3_BUCKET_NAME` | string | 🟡 | — | S3 버킷명 |
| `S3_BASE_URL` | string | 🟡 | — | 이미지 접근 기본 URL (`https://{bucket}.s3.{region}.amazonaws.com`) |
| `S3_PRESIGNED_EXPIRY` | long | 🟡 | `300` | Presigned URL 만료 (초, 기본 5분) |

> **로컬 개발:** MinIO 사용 시 `AWS_ENDPOINT_URL` 추가 (`http://localhost:9000`).

#### 이메일 (AWS SES / SMTP)

| 변수명 | 타입 | 🔴/🟡 | 기본값 | 설명 |
|--------|------|--------|--------|------|
| `MAIL_HOST` | string | 🟡 | `email-smtp.ap-northeast-2.amazonaws.com` | SMTP 호스트 |
| `MAIL_PORT` | int | 🟡 | `587` | SMTP 포트 (STARTTLS) |
| `MAIL_USERNAME` | string | 🔴 | — | SES SMTP 자격증명 사용자명 |
| `MAIL_PASSWORD` | string | 🔴 | — | SES SMTP 자격증명 비밀번호 |
| `MAIL_FROM` | string | 🟡 | `noreply@tradev.kr` | 발신자 이메일 주소 |
| `MAIL_FROM_NAME` | string | 🟡 | `Tradev` | 발신자 표시명 |

> **로컬 개발:** Mailpit 컨테이너 사용 (`MAIL_HOST=localhost`, `MAIL_PORT=1025`).

#### AI (Claude API)

| 변수명 | 타입 | 🔴/🟡 | 기본값 | 설명 |
|--------|------|--------|--------|------|
| `CLAUDE_API_KEY` | string | 🔴 | — | Anthropic API Key |
| `CLAUDE_MODEL` | string | 🟡 | `claude-sonnet-4-6` | 사용 모델 ID |
| `CLAUDE_MAX_TOKENS` | int | 🟡 | `1024` | 최대 응답 토큰 수 |
| `CLAUDE_TIMEOUT_SECONDS` | int | 🟡 | `30` | API 호출 타임아웃 (초) |
| `AI_DAILY_LIMIT_PER_USER` | int | 🟡 | `10` | 사용자당 일일 AI 기능 호출 한도 |

#### 로깅

| 변수명 | 타입 | 🟡 | 기본값 | 설명 |
|--------|------|-----|--------|------|
| `LOG_LEVEL_ROOT` | string | 🟡 | `INFO` | 루트 로그 레벨 |
| `LOG_LEVEL_APP` | string | 🟡 | `DEBUG` (local) / `INFO` (prod) | 애플리케이션 로그 레벨 |
| `LOG_SLOW_QUERY_MS` | int | 🟡 | `1000` | 슬로우 쿼리 기준 (ms) |

---

### 2.2 Vue.js 프론트엔드

> `VITE_` 접두사 변수만 클라이언트 번들에 포함됨. 비밀값 절대 포함 금지.

| 변수명 | 타입 | 🟢/🟡 | 설명 |
|--------|------|--------|------|
| `VITE_API_BASE_URL` | string | 🟡 | 백엔드 API 기본 URL (`https://api.tradev.kr/api`) |
| `VITE_WS_BASE_URL` | string | 🟡 | WebSocket 기본 URL (`wss://api.tradev.kr/ws`) |
| `VITE_GOOGLE_CLIENT_ID` | string | 🟢 | Google OAuth2 Client ID (공개값) |
| `VITE_S3_BASE_URL` | string | 🟡 | S3 이미지 기본 URL (이미지 경로 조합용) |
| `VITE_APP_ENV` | string | 🟡 | 현재 환경 표시 (`local` / `production`) |

---

### 2.3 MySQL

| 변수명 | 설명 |
|--------|------|
| `MYSQL_ROOT_PASSWORD` | 🔴 root 계정 비밀번호 |
| `MYSQL_DATABASE` | 🟡 초기 생성 데이터베이스명 (`tradev`) |
| `MYSQL_USER` | 🟡 애플리케이션용 계정명 (`tradev`) |
| `MYSQL_PASSWORD` | 🔴 애플리케이션용 계정 비밀번호 (`DB_PASSWORD`와 동일) |

---

### 2.4 Redis

| 변수명 | 설명 |
|--------|------|
| `REDIS_PASSWORD` | 🔴 Redis `requirepass` 비밀번호 (백엔드 `REDIS_PASSWORD`와 동일) |

---

### 2.5 Nginx

> 파일 기반 설정 (`nginx.conf`). 환경 변수보다 설정 파일로 관리.

| 항목 | 설명 |
|------|------|
| `server_name` | 도메인명 (`tradev.kr`, `www.tradev.kr`) |
| SSL 인증서 경로 | Let's Encrypt 발급 경로 (`/etc/letsencrypt/live/tradev.kr/`) |
| upstream app | Spring Boot 주소 (`app:8080`) |
| Rate Limit | `limit_req_zone` — IP 기준 100 req/min |

---

## 3. 환경별 .env 파일

> `.env.*` 파일은 **모두 `.gitignore`에 추가**한다. 샘플 파일(`.env.example`)만 Git에 포함.

### 3.1 로컬 개발 (`backend/.env.local`)

```dotenv
# ── 서버 ─────────────────────────────────────────
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=local
APP_BASE_URL=http://localhost:8080
FRONTEND_URL=http://localhost:5173

# ── 데이터베이스 ──────────────────────────────────
DB_HOST=localhost
DB_PORT=3306
DB_NAME=tradev
DB_USERNAME=tradev
DB_PASSWORD=tradev_local_pw
DB_DDL_AUTO=create-drop
DB_SHOW_SQL=true

# ── Redis ────────────────────────────────────────
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

# ── JWT ──────────────────────────────────────────
# openssl rand -base64 32 로 생성
JWT_SECRET=localDevSecretKeyMustBeAtLeast32CharsLong==
JWT_ACCESS_EXPIRY=1800000
JWT_REFRESH_EXPIRY=604800000

# ── OAuth2 (Google) ──────────────────────────────
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-google-client-secret
OAUTH2_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google

# ── AWS S3 → 로컬은 MinIO 대체 ───────────────────
AWS_ACCESS_KEY_ID=minioadmin
AWS_SECRET_ACCESS_KEY=minioadmin
AWS_REGION=us-east-1
AWS_ENDPOINT_URL=http://localhost:9000
S3_BUCKET_NAME=tradev-local
S3_BASE_URL=http://localhost:9000/tradev-local
S3_PRESIGNED_EXPIRY=300

# ── 이메일 → 로컬은 Mailpit 대체 ─────────────────
MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=noreply@tradev.local
MAIL_FROM_NAME=Tradev

# ── Claude API ───────────────────────────────────
CLAUDE_API_KEY=sk-ant-api03-xxxxxxxxxxxxxxxx
CLAUDE_MODEL=claude-sonnet-4-6
CLAUDE_MAX_TOKENS=1024
CLAUDE_TIMEOUT_SECONDS=30
AI_DAILY_LIMIT_PER_USER=10

# ── 로깅 ─────────────────────────────────────────
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=DEBUG
LOG_SLOW_QUERY_MS=1000
```

### 3.2 운영 (`backend/.env.prod`)

```dotenv
# ── 서버 ─────────────────────────────────────────
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=prod
APP_BASE_URL=https://api.tradev.kr
FRONTEND_URL=https://tradev.kr

# ── 데이터베이스 ──────────────────────────────────
DB_HOST=mysql
DB_PORT=3306
DB_NAME=tradev
DB_USERNAME=tradev
DB_PASSWORD=${PROD_DB_PASSWORD}          # GitHub Secret에서 주입
DB_DDL_AUTO=validate
DB_SHOW_SQL=false

# ── Redis ────────────────────────────────────────
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=${PROD_REDIS_PASSWORD}    # GitHub Secret에서 주입

# ── JWT ──────────────────────────────────────────
JWT_SECRET=${PROD_JWT_SECRET}            # GitHub Secret에서 주입
JWT_ACCESS_EXPIRY=1800000
JWT_REFRESH_EXPIRY=604800000

# ── OAuth2 (Google) ──────────────────────────────
GOOGLE_CLIENT_ID=${PROD_GOOGLE_CLIENT_ID}
GOOGLE_CLIENT_SECRET=${PROD_GOOGLE_CLIENT_SECRET}
OAUTH2_REDIRECT_URI=https://api.tradev.kr/login/oauth2/code/google

# ── AWS S3 ───────────────────────────────────────
AWS_ACCESS_KEY_ID=${PROD_AWS_ACCESS_KEY_ID}
AWS_SECRET_ACCESS_KEY=${PROD_AWS_SECRET_ACCESS_KEY}
AWS_REGION=ap-northeast-2
S3_BUCKET_NAME=tradev-prod
S3_BASE_URL=https://tradev-prod.s3.ap-northeast-2.amazonaws.com
S3_PRESIGNED_EXPIRY=300

# ── 이메일 (AWS SES) ─────────────────────────────
MAIL_HOST=email-smtp.ap-northeast-2.amazonaws.com
MAIL_PORT=587
MAIL_USERNAME=${PROD_MAIL_USERNAME}
MAIL_PASSWORD=${PROD_MAIL_PASSWORD}
MAIL_FROM=noreply@tradev.kr
MAIL_FROM_NAME=Tradev

# ── Claude API ───────────────────────────────────
CLAUDE_API_KEY=${PROD_CLAUDE_API_KEY}
CLAUDE_MODEL=claude-sonnet-4-6
CLAUDE_MAX_TOKENS=1024
CLAUDE_TIMEOUT_SECONDS=30
AI_DAILY_LIMIT_PER_USER=10

# ── 로깅 ─────────────────────────────────────────
LOG_LEVEL_ROOT=WARN
LOG_LEVEL_APP=INFO
LOG_SLOW_QUERY_MS=1000
```

### 3.3 프론트엔드

**`.env.local` (로컬 개발):**
```dotenv
VITE_API_BASE_URL=http://localhost:8080/api
VITE_WS_BASE_URL=ws://localhost:8080/ws
VITE_GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
VITE_S3_BASE_URL=http://localhost:9000/tradev-local
VITE_APP_ENV=local
```

**`.env.production` (운영 빌드):**
```dotenv
VITE_API_BASE_URL=https://api.tradev.kr/api
VITE_WS_BASE_URL=wss://api.tradev.kr/ws
VITE_GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
VITE_S3_BASE_URL=https://tradev-prod.s3.ap-northeast-2.amazonaws.com
VITE_APP_ENV=production
```

### 3.4 `.env.example` (Git 포함 샘플)

```dotenv
# backend/.env.example
# 이 파일을 복사하여 .env.local 또는 .env.prod 로 사용하세요.
# 실제 값을 채워넣고, 절대 Git에 커밋하지 마세요.

SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=local
APP_BASE_URL=http://localhost:8080
FRONTEND_URL=http://localhost:5173

DB_HOST=localhost
DB_PORT=3306
DB_NAME=tradev
DB_USERNAME=tradev
DB_PASSWORD=CHANGE_ME

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=CHANGE_ME_OR_LEAVE_EMPTY

JWT_SECRET=CHANGE_ME_USE_openssl_rand_base64_32
JWT_ACCESS_EXPIRY=1800000
JWT_REFRESH_EXPIRY=604800000

GOOGLE_CLIENT_ID=CHANGE_ME
GOOGLE_CLIENT_SECRET=CHANGE_ME
OAUTH2_REDIRECT_URI=http://localhost:8080/login/oauth2/code/google

AWS_ACCESS_KEY_ID=CHANGE_ME
AWS_SECRET_ACCESS_KEY=CHANGE_ME
AWS_REGION=ap-northeast-2
S3_BUCKET_NAME=CHANGE_ME
S3_BASE_URL=CHANGE_ME
S3_PRESIGNED_EXPIRY=300

MAIL_HOST=localhost
MAIL_PORT=1025
MAIL_USERNAME=CHANGE_ME
MAIL_PASSWORD=CHANGE_ME
MAIL_FROM=noreply@tradev.local
MAIL_FROM_NAME=Tradev

CLAUDE_API_KEY=CHANGE_ME
CLAUDE_MODEL=claude-sonnet-4-6
CLAUDE_MAX_TOKENS=1024
CLAUDE_TIMEOUT_SECONDS=30
AI_DAILY_LIMIT_PER_USER=10

LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=DEBUG
LOG_SLOW_QUERY_MS=1000
```

---

## 4. Spring Boot application.yml

### 4.1 공통 (`application.yml`)

```yaml
server:
  port: ${SERVER_PORT:8080}

spring:
  application:
    name: tradev

  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:tradev}?useSSL=false&characterEncoding=UTF-8&serverTimezone=Asia/Seoul
    username: ${DB_USERNAME:tradev}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

  jpa:
    hibernate:
      ddl-auto: ${DB_DDL_AUTO:validate}
    show-sql: ${DB_SHOW_SQL:false}
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.MySQL8Dialect
        default_batch_fetch_size: 100

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}

  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            redirect-uri: ${OAUTH2_REDIRECT_URI}
            scope: email, profile

  mail:
    host: ${MAIL_HOST}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
      mail.smtp.starttls.required: true
    default-encoding: UTF-8

# JWT
jwt:
  secret: ${JWT_SECRET}
  access-expiry: ${JWT_ACCESS_EXPIRY:1800000}
  refresh-expiry: ${JWT_REFRESH_EXPIRY:604800000}

# AWS
cloud:
  aws:
    credentials:
      access-key: ${AWS_ACCESS_KEY_ID}
      secret-key: ${AWS_SECRET_ACCESS_KEY}
    region:
      static: ${AWS_REGION:ap-northeast-2}
    s3:
      bucket: ${S3_BUCKET_NAME}
    stack:
      auto: false   # EC2 메타데이터 자동 탐색 비활성화

app:
  base-url: ${APP_BASE_URL}
  frontend-url: ${FRONTEND_URL}
  s3:
    base-url: ${S3_BASE_URL}
    presigned-expiry: ${S3_PRESIGNED_EXPIRY:300}
  mail:
    from: ${MAIL_FROM:noreply@tradev.kr}
    from-name: ${MAIL_FROM_NAME:Tradev}

# Claude AI
claude:
  api-key: ${CLAUDE_API_KEY}
  model: ${CLAUDE_MODEL:claude-sonnet-4-6}
  max-tokens: ${CLAUDE_MAX_TOKENS:1024}
  timeout-seconds: ${CLAUDE_TIMEOUT_SECONDS:30}
  daily-limit-per-user: ${AI_DAILY_LIMIT_PER_USER:10}

# 로깅
logging:
  level:
    root: ${LOG_LEVEL_ROOT:INFO}
    com.tradev: ${LOG_LEVEL_APP:DEBUG}
    org.hibernate.SQL: ${DB_SHOW_SQL:false}
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
```

### 4.2 로컬 전용 (`application-local.yml`)

```yaml
# MinIO (S3 로컬 대체)
cloud:
  aws:
    s3:
      endpoint: ${AWS_ENDPOINT_URL:http://localhost:9000}
      path-style-access-enabled: true  # MinIO는 path-style 필수

spring:
  jpa:
    hibernate:
      ddl-auto: create-drop   # 로컬: 매 실행마다 스키마 재생성

logging:
  level:
    org.hibernate.SQL: true
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

### 4.3 운영 전용 (`application-prod.yml`)

```yaml
server:
  tomcat:
    max-threads: 200
    min-spare-threads: 20

spring:
  jpa:
    hibernate:
      ddl-auto: validate   # 운영: 스키마 변경 차단

# Actuator (헬스체크)
management:
  endpoints:
    web:
      exposure:
        include: health, info
  endpoint:
    health:
      show-details: never   # 운영: 내부 정보 미노출

logging:
  level:
    root: WARN
    com.tradev: INFO
  file:
    name: /app/logs/tradev.log
  logback:
    rollingpolicy:
      max-file-size: 100MB
      max-history: 30
```

---

## 5. Docker Compose 환경 주입

### 5.1 로컬 개발 (`docker-compose.local.yml`)

```yaml
version: "3.9"

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root_local_pw
      MYSQL_DATABASE: tradev
      MYSQL_USER: tradev
      MYSQL_PASSWORD: tradev_local_pw
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    command: redis-server   # 로컬: 비밀번호 없음
    ports:
      - "6379:6379"

  minio:
    image: minio/minio
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9000:9000"   # API
      - "9001:9001"   # Console UI
    volumes:
      - minio_data:/data

  mailpit:
    image: axllent/mailpit
    ports:
      - "1025:1025"   # SMTP
      - "8025:8025"   # Web UI (수신 메일 확인)

volumes:
  mysql_data:
  minio_data:
```

### 5.2 운영 (`docker-compose.yml`)

```yaml
version: "3.9"

services:
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - /etc/letsencrypt:/etc/letsencrypt:ro
      - ./frontend/dist:/usr/share/nginx/html:ro
    depends_on:
      - app
    restart: unless-stopped

  app:
    image: ${DOCKER_IMAGE}:${IMAGE_TAG}        # GitHub Actions에서 주입
    env_file:
      - ./backend/.env.prod                    # EC2에 존재하는 파일
    expose:
      - "8080"
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    logging:
      driver: "json-file"
      options:
        max-size: "100m"
        max-file: "3"

  mysql:
    image: mysql:8.0
    env_file:
      - ./mysql/.env.mysql                     # MYSQL_* 변수만 포함
    volumes:
      - mysql_data:/var/lib/mysql
      - ./mysql/init:/docker-entrypoint-initdb.d:ro
    expose:
      - "3306"
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    environment:
      - REDIS_PASSWORD=${REDIS_PASSWORD}
    expose:
      - "6379"
    volumes:
      - redis_data:/data
    restart: unless-stopped

volumes:
  mysql_data:
  redis_data:
```

---

## 6. GitHub Actions Secrets

> GitHub 저장소 → Settings → Secrets and variables → Actions 에서 등록.

### 6.1 배포 관련

| Secret명 | 설명 |
|---------|------|
| `EC2_HOST` | EC2 퍼블릭 IP 또는 도메인 |
| `EC2_USER` | SSH 접속 사용자명 (`ec2-user` 또는 `ubuntu`) |
| `EC2_SSH_KEY` | EC2 접속용 PEM 키 (개인키 전체 내용) |
| `DOCKER_USERNAME` | Docker Hub 계정명 |
| `DOCKER_PASSWORD` | Docker Hub 비밀번호 또는 Access Token |

### 6.2 운영 환경 변수 (EC2 .env.prod 생성용)

| Secret명 | 대응 환경 변수 |
|---------|--------------|
| `PROD_DB_PASSWORD` | `DB_PASSWORD` |
| `PROD_REDIS_PASSWORD` | `REDIS_PASSWORD` |
| `PROD_JWT_SECRET` | `JWT_SECRET` |
| `PROD_GOOGLE_CLIENT_ID` | `GOOGLE_CLIENT_ID` |
| `PROD_GOOGLE_CLIENT_SECRET` | `GOOGLE_CLIENT_SECRET` |
| `PROD_AWS_ACCESS_KEY_ID` | `AWS_ACCESS_KEY_ID` |
| `PROD_AWS_SECRET_ACCESS_KEY` | `AWS_SECRET_ACCESS_KEY` |
| `PROD_MAIL_USERNAME` | `MAIL_USERNAME` |
| `PROD_MAIL_PASSWORD` | `MAIL_PASSWORD` |
| `PROD_CLAUDE_API_KEY` | `CLAUDE_API_KEY` |

### 6.3 GitHub Actions Workflow에서 .env.prod 생성

```yaml
# .github/workflows/deploy.yml 中

- name: Create .env.prod on EC2
  uses: appleboy/ssh-action@v1
  with:
    host: ${{ secrets.EC2_HOST }}
    username: ${{ secrets.EC2_USER }}
    key: ${{ secrets.EC2_SSH_KEY }}
    script: |
      cat > ~/tradev/backend/.env.prod << 'EOF'
      SPRING_PROFILES_ACTIVE=prod
      APP_BASE_URL=https://api.tradev.kr
      FRONTEND_URL=https://tradev.kr
      DB_HOST=mysql
      DB_PORT=3306
      DB_NAME=tradev
      DB_USERNAME=tradev
      DB_PASSWORD=${{ secrets.PROD_DB_PASSWORD }}
      REDIS_HOST=redis
      REDIS_PORT=6379
      REDIS_PASSWORD=${{ secrets.PROD_REDIS_PASSWORD }}
      JWT_SECRET=${{ secrets.PROD_JWT_SECRET }}
      JWT_ACCESS_EXPIRY=1800000
      JWT_REFRESH_EXPIRY=604800000
      GOOGLE_CLIENT_ID=${{ secrets.PROD_GOOGLE_CLIENT_ID }}
      GOOGLE_CLIENT_SECRET=${{ secrets.PROD_GOOGLE_CLIENT_SECRET }}
      OAUTH2_REDIRECT_URI=https://api.tradev.kr/login/oauth2/code/google
      AWS_ACCESS_KEY_ID=${{ secrets.PROD_AWS_ACCESS_KEY_ID }}
      AWS_SECRET_ACCESS_KEY=${{ secrets.PROD_AWS_SECRET_ACCESS_KEY }}
      AWS_REGION=ap-northeast-2
      S3_BUCKET_NAME=tradev-prod
      S3_BASE_URL=https://tradev-prod.s3.ap-northeast-2.amazonaws.com
      S3_PRESIGNED_EXPIRY=300
      MAIL_HOST=email-smtp.ap-northeast-2.amazonaws.com
      MAIL_PORT=587
      MAIL_USERNAME=${{ secrets.PROD_MAIL_USERNAME }}
      MAIL_PASSWORD=${{ secrets.PROD_MAIL_PASSWORD }}
      MAIL_FROM=noreply@tradev.kr
      MAIL_FROM_NAME=Tradev
      CLAUDE_API_KEY=${{ secrets.PROD_CLAUDE_API_KEY }}
      CLAUDE_MODEL=claude-sonnet-4-6
      CLAUDE_MAX_TOKENS=1024
      CLAUDE_TIMEOUT_SECONDS=30
      AI_DAILY_LIMIT_PER_USER=10
      LOG_LEVEL_ROOT=WARN
      LOG_LEVEL_APP=INFO
      LOG_SLOW_QUERY_MS=1000
      EOF
      chmod 600 ~/tradev/backend/.env.prod
```

---

## 7. 비밀값 관리 전략

### 7.1 Git 제외 규칙 (`.gitignore`)

```gitignore
# 환경 변수 파일 — 절대 커밋 금지
.env
.env.*
!.env.example

# 인증서 / 키 파일
*.pem
*.key
*.p12
```

### 7.2 비밀값 생성 가이드

```bash
# JWT_SECRET 생성 (최소 32자 Base64)
openssl rand -base64 32

# DB_PASSWORD 생성 (16자 랜덤)
openssl rand -base64 16 | tr -dc 'a-zA-Z0-9' | head -c 16

# REDIS_PASSWORD 생성
openssl rand -base64 16 | tr -dc 'a-zA-Z0-9' | head -c 20
```

### 7.3 로컬 개발 비밀값 관리

```
원칙: 로컬용 비밀값도 실제 운영 비밀값과 달라야 한다.
     (로컬 DB에 운영 비밀번호 사용 금지)

권장 도구:
  - direnv: 디렉토리 진입 시 .env.local 자동 로드
  - 1Password CLI / Bitwarden CLI: 팀 공유 비밀값 관리
    (현재 솔로 프로젝트이므로 .env.local 파일 직접 관리)
```

### 7.4 비밀값 교체 절차

```
1. 새 비밀값 생성 (위 생성 가이드 참고)
2. GitHub Secrets 업데이트
3. CI/CD 파이프라인 재실행 (새 .env.prod 생성)
4. 도커 컨테이너 재시작 (docker-compose up -d app)
5. 구 비밀값 무효화 확인

JWT_SECRET 교체 시 주의:
  → 기존 발급된 모든 Access Token/Refresh Token 무효화됨
  → 사용자 전원 재로그인 필요 (점검 공지 후 교체 권장)
```

### 7.5 사고 발생 시 대응

```
비밀값 유출 의심 시 즉시 수행:
  1. GitHub Secret 즉시 교체
  2. 해당 서비스 비밀값 폐기 (API Key revoke, 비밀번호 변경 등)
  3. Redis에서 모든 Refresh Token 삭제 (FLUSHDB 또는 키 패턴 삭제)
  4. Git 히스토리에 포함된 경우 → git-filter-repo로 히스토리 재작성
     (이미 public repo면 Git에 커밋된 비밀값은 무효 처리 후 재발급 필수)
  5. 액세스 로그 확인하여 비정상 접근 여부 확인
```

---

## 8. 환경 변수 체크리스트

> 배포 전 반드시 확인. 미설정 시 애플리케이션 기동 실패 또는 보안 취약점 발생.

### 로컬 환경 시작 전

- [ ] `backend/.env.local` 파일 존재 (`cp backend/.env.example backend/.env.local`)
- [ ] `DB_PASSWORD` 설정 완료
- [ ] `JWT_SECRET` 설정 완료 (최소 32자)
- [ ] `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` 설정 (소셜 로그인 사용 시)
- [ ] `CLAUDE_API_KEY` 설정 (AI 기능 사용 시)
- [ ] Docker 로컬 서비스 기동 (`docker-compose -f docker-compose.local.yml up -d`)
- [ ] MinIO 버킷 생성 (`tradev-local`, public read 정책)
- [ ] `frontend/.env.local` 파일 존재

### 운영 배포 전

- [ ] GitHub Secrets 전체 등록 완료 (6.2 항목)
- [ ] EC2 보안 그룹: 포트 80/443만 외부 허용, 3306/6379 외부 차단
- [ ] S3 버킷 생성 및 CORS 설정
  ```json
  [{ "AllowedOrigins": ["https://tradev.kr"],
     "AllowedMethods": ["GET", "PUT"],
     "AllowedHeaders": ["*"],
     "MaxAgeSeconds": 3000 }]
  ```
- [ ] SES 이메일 발신자 주소 검증 완료
- [ ] Google Cloud Console에 운영 redirect URI 등록
  (`https://api.tradev.kr/login/oauth2/code/google`)
- [ ] SSL 인증서 발급 (`certbot --nginx -d tradev.kr -d api.tradev.kr`)
- [ ] `GET /actuator/health` 응답 `{"status":"UP"}` 확인

---

> **다음 단계:** `plan.md` — 구현 순서, 작업 단위 분해, 마일스톤별 태스크 목록
