# API Spec — Tradev (중고거래 / 예약 플랫폼)

> **문서 버전:** v1.0.0  
> **작성일:** 2026-05-23  
> **연관 문서:** [PRD.md](PRD.md) · [Architecture.md](Architecture.md) · [DB Schema.md](DB%20Schema.md)

---

## 목차

1. [공통 규칙](#1-공통-규칙)
2. [인증 API](#2-인증-api)
3. [사용자 / 프로필 API](#3-사용자--프로필-api)
4. [상품 API](#4-상품-api)
5. [거래 API](#5-거래-api)
6. [예약 슬롯 API](#6-예약-슬롯-api)
7. [채팅 API](#7-채팅-api)
8. [알림 API](#8-알림-api)
9. [리뷰 API](#9-리뷰-api)
10. [신고 API](#10-신고-api)
11. [AI API](#11-ai-api)
12. [관리자 API](#12-관리자-api)
13. [실시간 통신](#13-실시간-통신)

---

## 1. 공통 규칙

### 1.1 Base URL

```
Production : https://api.tradev.kr/api
Development: http://localhost:8080/api
Admin      : https://api.tradev.kr/admin/api
```

### 1.2 인증

| 표기 | 의미 |
|------|------|
| 🔓 | 인증 불필요 (공개) |
| 🔒 | JWT Access Token 필요 |
| 👑 | Admin 권한 필요 |

```
Authorization: Bearer {accessToken}
```

### 1.3 공통 응답 형식

**성공**
```json
{
  "success": true,
  "data": { },
  "message": null
}
```

**실패**
```json
{
  "success": false,
  "data": null,
  "message": "이메일 또는 비밀번호가 올바르지 않습니다.",
  "code": "AUTH_INVALID_CREDENTIALS"
}
```

### 1.4 페이지네이션 (커서 기반)

**요청 Query Parameter:**

| 파라미터 | 타입 | 기본값 | 설명 |
|---------|------|--------|------|
| `cursor` | string | null | 이전 응답의 `nextCursor` |
| `size` | int | 20 | 페이지 크기 (최대 50) |

**응답:**
```json
{
  "success": true,
  "data": {
    "content": [ ],
    "nextCursor": "2026-05-01T10:00:00|42",
    "hasNext": true,
    "size": 20
  }
}
```

`nextCursor` 형식: `{createdAt}|{id}` (ISO-8601 + 레코드 ID)  
`hasNext: false`이면 마지막 페이지.

### 1.5 HTTP 상태 코드

| 코드 | 의미 |
|------|------|
| 200 | 성공 |
| 201 | 생성 성공 |
| 204 | 성공 (응답 바디 없음) |
| 400 | 요청 유효성 오류 |
| 401 | 인증 필요 / 토큰 만료 |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 409 | 충돌 (중복, 상태 불일치) |
| 429 | Rate Limit 초과 |
| 500 | 서버 내부 오류 |

### 1.6 공통 에러 코드

| code | HTTP | 설명 |
|------|------|------|
| `VALIDATION_ERROR` | 400 | 필드 유효성 검사 실패 |
| `AUTH_REQUIRED` | 401 | 인증 토큰 없음 |
| `AUTH_TOKEN_EXPIRED` | 401 | Access Token 만료 |
| `AUTH_TOKEN_INVALID` | 401 | 토큰 형식 오류 |
| `AUTH_REFRESH_EXPIRED` | 401 | Refresh Token 만료 |
| `FORBIDDEN` | 403 | 권한 없음 |
| `NOT_FOUND` | 404 | 리소스 없음 |
| `CONFLICT` | 409 | 중복 또는 상태 충돌 |
| `OPTIMISTIC_LOCK` | 409 | 동시 수정 충돌 |
| `RATE_LIMIT_EXCEEDED` | 429 | 요청 횟수 초과 |
| `INTERNAL_ERROR` | 500 | 서버 오류 |

---

## 2. 인증 API

### POST /auth/signup — 이메일 회원가입 🔓

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "Password1!",
  "nickname": "박지훈"
}
```

| 필드 | 타입 | 필수 | 유효성 |
|------|------|------|--------|
| email | string | Y | 이메일 형식, 최대 255자 |
| password | string | Y | 8~20자, 영문+숫자+특수문자 각 1개 이상 |
| nickname | string | Y | 2~15자, 한글/영문/숫자 |

**Response 201:**
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "message": "인증 메일이 발송되었습니다."
  }
}
```

**Error:**

| code | 설명 |
|------|------|
| `AUTH_EMAIL_DUPLICATED` | 이미 사용 중인 이메일 |
| `AUTH_NICKNAME_DUPLICATED` | 이미 사용 중인 닉네임 |

---

### POST /auth/email-verify — 이메일 인증 🔓

**Request Body:**
```json
{ "token": "uuid-verification-token" }
```

**Response 200:**
```json
{
  "success": true,
  "data": { "message": "이메일 인증이 완료되었습니다." }
}
```

**Error:**

| code | 설명 |
|------|------|
| `AUTH_VERIFY_TOKEN_INVALID` | 유효하지 않은 인증 토큰 |
| `AUTH_VERIFY_TOKEN_EXPIRED` | 만료된 인증 토큰 (24시간) |

---

### POST /auth/email-verify/resend — 인증 메일 재발송 🔓

**Request Body:**
```json
{ "email": "user@example.com" }
```

**Response 200:**
```json
{
  "success": true,
  "data": { "message": "인증 메일이 재발송되었습니다." }
}
```

> 60초 쿨다운 적용. 위반 시 `RATE_LIMIT_EXCEEDED`.

---

### POST /auth/login — 로그인 🔓

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "Password1!"
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGci...",
    "tokenType": "Bearer",
    "expiresIn": 1800
  }
}
```

> `refreshToken`은 `HttpOnly; Secure; SameSite=Strict` 쿠키로 Set-Cookie 헤더에 포함.

**Error:**

| code | 설명 |
|------|------|
| `AUTH_INVALID_CREDENTIALS` | 이메일/비밀번호 불일치 |
| `AUTH_EMAIL_NOT_VERIFIED` | 이메일 미인증 상태 |
| `AUTH_USER_SUSPENDED` | 정지된 계정 (suspendedUntil 포함) |
| `AUTH_USER_WITHDRAWN` | 탈퇴한 계정 |

---

### POST /auth/refresh — 토큰 재발급 🔓

> Request Cookie: `refreshToken`

**Response 200:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGci...",
    "expiresIn": 1800
  }
}
```

> Rotation 전략: 기존 Refresh Token 폐기 후 새 Refresh Token을 쿠키에 재발급.

---

### POST /auth/logout — 로그아웃 🔒

> Request Cookie: `refreshToken`

**Response 204** (No Content)

---

### POST /auth/password-reset/request — 비밀번호 재설정 요청 🔓

**Request Body:**
```json
{ "email": "user@example.com" }
```

**Response 200:**
```json
{
  "success": true,
  "data": { "message": "비밀번호 재설정 메일이 발송되었습니다." }
}
```

> 이메일 미존재 시에도 동일 응답 반환 (보안: 계정 존재 여부 노출 방지).

---

### POST /auth/password-reset/confirm — 비밀번호 재설정 확인 🔓

**Request Body:**
```json
{
  "token": "uuid-reset-token",
  "newPassword": "NewPassword1!"
}
```

**Response 200:**
```json
{
  "success": true,
  "data": { "message": "비밀번호가 변경되었습니다." }
}
```

---

### GET /auth/check-email — 이메일 중복 확인 🔓

**Query:** `?email=user@example.com`

**Response 200:**
```json
{
  "success": true,
  "data": { "available": true }
}
```

---

### GET /auth/check-nickname — 닉네임 중복 확인 🔓

**Query:** `?nickname=박지훈`

**Response 200:**
```json
{
  "success": true,
  "data": { "available": false }
}
```

---

## 3. 사용자 / 프로필 API

### GET /users/{userId} — 프로필 조회 🔓

**Path:** `userId` (Long)

**Response 200:**
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "nickname": "박지훈",
    "profileImageUrl": "https://s3.../profile/1.jpg",
    "bio": "교재, 전자기기 팝니다",
    "trustScore": 65,
    "grade": "FRUIT",
    "tradeCount": 12,
    "reviewCount": 10,
    "averageRating": 4.7,
    "joinedAt": "2026-01-15T09:00:00"
  }
}
```

---

### GET /users/me — 내 정보 조회 🔒

**Response 200:**
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "email": "user@example.com",
    "nickname": "박지훈",
    "profileImageUrl": "https://s3.../profile/1.jpg",
    "bio": "교재, 전자기기 팝니다",
    "trustScore": 65,
    "grade": "FRUIT",
    "role": "USER",
    "emailVerified": true,
    "createdAt": "2026-01-15T09:00:00"
  }
}
```

---

### PUT /users/me — 프로필 수정 🔒

**Request Body (multipart/form-data):**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| nickname | string | N | 2~15자 |
| bio | string | N | 최대 200자 |
| profileImage | file | N | jpg/png, 최대 5MB |

**Response 200:**
```json
{
  "success": true,
  "data": {
    "userId": 1,
    "nickname": "박지훈",
    "profileImageUrl": "https://s3.../profile/1_new.jpg",
    "bio": "수정된 소개"
  }
}
```

**Error:**

| code | 설명 |
|------|------|
| `AUTH_NICKNAME_DUPLICATED` | 닉네임 중복 |

---

### DELETE /users/me — 회원 탈퇴 🔒

**Request Body:**
```json
{ "password": "Password1!" }
```

> OAuth 가입자는 password 불필요 (null 허용).

**Response 204**

---

### GET /users/me/wishlist — 관심 상품 목록 🔒

**Query:** `cursor`, `size`

**Response 200:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "wishlistId": 10,
        "item": {
          "itemId": 55,
          "title": "아이패드 프로 11인치",
          "price": 800000,
          "thumbnailUrl": "https://s3.../items/55/0.jpg",
          "status": "SALE",
          "tradeType": "BOTH"
        },
        "createdAt": "2026-05-20T14:00:00"
      }
    ],
    "nextCursor": "2026-05-20T14:00:00|10",
    "hasNext": false,
    "size": 20
  }
}
```

---

## 4. 상품 API

### GET /items — 상품 목록 🔓

**Query Parameters:**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| cursor | string | 커서 |
| size | int | 페이지 크기 (기본 20) |
| categoryId | int | 카테고리 ID (대/중분류 모두 가능) |
| minPrice | int | 최소 가격 |
| maxPrice | int | 최대 가격 |
| tradeType | string | DIRECT / DELIVERY / BOTH |
| status | string | SALE / RESERVED / COMPLETED (기본: SALE) |
| sort | string | LATEST (기본) / POPULAR |
| keyword | string | 키워드 검색 (제목 + 설명) |

**Response 200:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "itemId": 100,
        "title": "맥북 프로 M3 14인치",
        "price": 2500000,
        "thumbnailUrl": "https://s3.../items/100/0.jpg",
        "categoryName": "노트북",
        "tradeType": "DIRECT",
        "status": "SALE",
        "wishCount": 23,
        "isWished": true,
        "seller": {
          "userId": 1,
          "nickname": "박지훈",
          "grade": "FRUIT"
        },
        "createdAt": "2026-05-20T10:00:00"
      }
    ],
    "nextCursor": "2026-05-20T10:00:00|100",
    "hasNext": true,
    "size": 20
  }
}
```

> `isWished`: 로그인 사용자만 표시. 비로그인 시 `null`.

---

### POST /items — 상품 등록 🔒

**Request Body (application/json):**
```json
{
  "title": "맥북 프로 M3 14인치",
  "description": "2024년 구매, 사용감 적음. 박스 있음.",
  "categoryId": 12,
  "price": 2500000,
  "tradeType": "DIRECT",
  "imageKeys": ["items/tmp/uuid1.jpg", "items/tmp/uuid2.jpg"]
}
```

| 필드 | 타입 | 필수 | 유효성 |
|------|------|------|--------|
| title | string | Y | 2~100자 |
| description | string | Y | 10~2000자 |
| categoryId | int | Y | 유효한 중분류 ID |
| price | int | Y | 0 이상 (무료나눔 허용) |
| tradeType | string | Y | DIRECT / DELIVERY / BOTH |
| imageKeys | string[] | Y | 1~10개, Presigned 업로드 완료된 S3 키 |

**Response 201:**
```json
{
  "success": true,
  "data": {
    "itemId": 100,
    "title": "맥북 프로 M3 14인치",
    "status": "SALE",
    "createdAt": "2026-05-23T10:00:00"
  }
}
```

**Error:**

| code | 설명 |
|------|------|
| `ITEM_IMAGE_LIMIT_EXCEEDED` | 이미지 10장 초과 |
| `ITEM_INVALID_CATEGORY` | 유효하지 않은 카테고리 |

---

### GET /items/{itemId} — 상품 상세 🔓

**Response 200:**
```json
{
  "success": true,
  "data": {
    "itemId": 100,
    "title": "맥북 프로 M3 14인치",
    "description": "2024년 구매, 사용감 적음. 박스 있음.",
    "price": 2500000,
    "tradeType": "DIRECT",
    "status": "SALE",
    "wishCount": 23,
    "viewCount": 150,
    "isWished": true,
    "isMine": false,
    "images": [
      { "imageId": 1, "imageUrl": "https://s3.../0.jpg", "isThumbnail": true },
      { "imageId": 2, "imageUrl": "https://s3.../1.jpg", "isThumbnail": false }
    ],
    "category": {
      "parentId": 1, "parentName": "전자기기",
      "categoryId": 12, "categoryName": "노트북"
    },
    "seller": {
      "userId": 1,
      "nickname": "박지훈",
      "profileImageUrl": "https://s3.../profile/1.jpg",
      "trustScore": 65,
      "grade": "FRUIT",
      "tradeCount": 12
    },
    "hasSlots": true,
    "createdAt": "2026-05-20T10:00:00",
    "updatedAt": "2026-05-21T08:00:00"
  }
}
```

> 조회 시 `viewCount` +1 처리 (비동기).  
> `isMine`: 로그인 사용자가 판매자인 경우 `true`.  
> `hasSlots`: 가용 슬롯 존재 여부.

---

### PUT /items/{itemId} — 상품 수정 🔒

> 본인 상품, `COMPLETED` 상태 아닐 때만 가능.

**Request Body:**
```json
{
  "title": "맥북 프로 M3 14인치 (박스포함)",
  "description": "수정된 설명...",
  "price": 2400000,
  "tradeType": "BOTH",
  "imageKeys": ["items/100/uuid1.jpg", "items/tmp/uuid3.jpg"]
}
```

**Response 200:**
```json
{
  "success": true,
  "data": { "itemId": 100, "updatedAt": "2026-05-23T11:00:00" }
}
```

**Error:**

| code | 설명 |
|------|------|
| `ITEM_NOT_EDITABLE` | 거래 완료 상태 |
| `FORBIDDEN` | 본인 상품 아님 |

---

### DELETE /items/{itemId} — 상품 삭제 🔒

> 거래 진행 중 (`PENDING` / `RESERVED`) 삭제 불가.

**Response 204**

**Error:**

| code | 설명 |
|------|------|
| `ITEM_IN_TRADE` | 거래 진행 중 |

---

### POST /items/{itemId}/boost — 끌어올리기 🔒

> 하루 1회 제한.

**Response 200:**
```json
{
  "success": true,
  "data": { "boostedAt": "2026-05-23T12:00:00" }
}
```

**Error:**

| code | 설명 |
|------|------|
| `ITEM_BOOST_LIMIT` | 오늘 이미 끌어올리기 사용 |

---

### PATCH /items/{itemId}/visibility — 숨기기 / 공개 🔒

**Request Body:**
```json
{ "hidden": true }
```

**Response 200:**
```json
{
  "success": true,
  "data": { "status": "HIDDEN" }
}
```

---

### POST /items/{itemId}/wishlist — 관심 등록 / 취소 토글 🔒

**Response 200:**
```json
{
  "success": true,
  "data": {
    "wished": true,
    "wishCount": 24
  }
}
```

---

### POST /items/images/presigned — Presigned URL 발급 🔒

**Request Body:**
```json
{
  "files": [
    { "fileName": "macbook.jpg", "contentType": "image/jpeg" },
    { "fileName": "box.png",     "contentType": "image/png" }
  ]
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "presignedUrls": [
      {
        "uploadUrl": "https://s3.../presigned-url-1",
        "s3Key": "items/tmp/uuid1.jpg",
        "expiresIn": 300
      },
      {
        "uploadUrl": "https://s3.../presigned-url-2",
        "s3Key": "items/tmp/uuid2.png",
        "expiresIn": 300
      }
    ]
  }
}
```

> 클라이언트: `uploadUrl`로 PUT 요청하여 이미지 업로드 후, 반환된 `s3Key` 배열을 상품 등록 API에 전달.

**Error:**

| code | 설명 |
|------|------|
| `ITEM_IMAGE_LIMIT_EXCEEDED` | 10개 초과 요청 |
| `ITEM_IMAGE_INVALID_TYPE` | 허용되지 않는 파일 형식 (jpg/png/webp만 허용) |

---

### GET /categories — 카테고리 목록 🔓

**Response 200:**
```json
{
  "success": true,
  "data": [
    {
      "categoryId": 1,
      "name": "전자기기",
      "children": [
        { "categoryId": 11, "name": "스마트폰" },
        { "categoryId": 12, "name": "노트북" }
      ]
    }
  ]
}
```

---

## 5. 거래 API

### POST /trades — 구매 요청 🔒

**Request Body:**
```json
{
  "itemId": 100,
  "message": "안녕하세요, 직거래 원합니다."
}
```

| 필드 | 타입 | 필수 | 유효성 |
|------|------|------|--------|
| itemId | long | Y | 판매중 상품, 본인 상품 아닐 것 |
| message | string | N | 최대 500자 |

**Response 201:**
```json
{
  "success": true,
  "data": {
    "tradeId": 50,
    "status": "PENDING",
    "item": { "itemId": 100, "title": "맥북 프로 M3 14인치" },
    "createdAt": "2026-05-23T10:00:00"
  }
}
```

**Error:**

| code | 설명 |
|------|------|
| `TRADE_ITEM_NOT_AVAILABLE` | 판매 중이 아닌 상품 |
| `TRADE_SELF_REQUEST` | 본인 상품 구매 요청 |
| `TRADE_ALREADY_PENDING` | 이미 진행 중인 요청 존재 |

---

### GET /trades — 거래 목록 🔒

**Query Parameters:**

| 파라미터 | 설명 |
|---------|------|
| role | BUYER / SELLER (기본: 전체) |
| status | PENDING / RESERVED / COMPLETED / CANCELLED / REJECTED |
| cursor, size | 페이지네이션 |

**Response 200:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "tradeId": 50,
        "status": "RESERVED",
        "item": {
          "itemId": 100,
          "title": "맥북 프로 M3 14인치",
          "price": 2500000,
          "thumbnailUrl": "https://s3.../0.jpg"
        },
        "counterpart": {
          "userId": 2,
          "nickname": "이소연",
          "grade": "SPROUT"
        },
        "myRole": "SELLER",
        "createdAt": "2026-05-20T10:00:00",
        "updatedAt": "2026-05-21T08:00:00"
      }
    ],
    "nextCursor": "2026-05-20T10:00:00|50",
    "hasNext": false,
    "size": 20
  }
}
```

---

### GET /trades/{tradeId} — 거래 상세 🔒

**Response 200:**
```json
{
  "success": true,
  "data": {
    "tradeId": 50,
    "status": "RESERVED",
    "item": {
      "itemId": 100,
      "title": "맥북 프로 M3 14인치",
      "price": 2500000,
      "thumbnailUrl": "https://s3.../0.jpg",
      "tradeType": "DIRECT"
    },
    "buyer": {
      "userId": 2, "nickname": "이소연", "grade": "SPROUT", "trustScore": 50
    },
    "seller": {
      "userId": 1, "nickname": "박지훈", "grade": "FRUIT", "trustScore": 65
    },
    "requestMessage": "안녕하세요, 직거래 원합니다.",
    "myRole": "SELLER",
    "buyerConfirmed": false,
    "sellerConfirmed": false,
    "reservation": {
      "reservationId": 10,
      "slotStartedAt": "2026-05-25T14:00:00",
      "slotEndedAt": "2026-05-25T15:00:00",
      "status": "CONFIRMED"
    },
    "canConfirm": true,
    "canCancel": true,
    "reviewWritten": false,
    "createdAt": "2026-05-20T10:00:00"
  }
}
```

---

### PATCH /trades/{tradeId}/accept — 거래 수락 🔒

> 판매자만 가능. 상태 `PENDING` → `RESERVED`.

**Response 200:**
```json
{
  "success": true,
  "data": { "tradeId": 50, "status": "RESERVED" }
}
```

**Error:**

| code | 설명 |
|------|------|
| `TRADE_INVALID_STATUS` | 수락 불가 상태 |
| `FORBIDDEN` | 판매자 아님 |

---

### PATCH /trades/{tradeId}/reject — 거래 거절 🔒

> 판매자만 가능. 상태 `PENDING` → `REJECTED`.

**Request Body:**
```json
{ "reason": "이미 다른 분과 거래 중입니다." }
```

**Response 200:**
```json
{
  "success": true,
  "data": { "tradeId": 50, "status": "REJECTED" }
}
```

---

### PATCH /trades/{tradeId}/confirm — 거래 완료 확인 🔒

> 구매자/판매자 모두 호출 가능. 양측 확인 시 `COMPLETED` 전이.

**Response 200:**
```json
{
  "success": true,
  "data": {
    "tradeId": 50,
    "status": "RESERVED",
    "buyerConfirmed": true,
    "sellerConfirmed": false,
    "completed": false
  }
}
```

> `completed: true`이면 양측 완료 — 신뢰 점수 +5, 리뷰 작성 유도.

---

### PATCH /trades/{tradeId}/cancel — 거래 취소 🔒

**Request Body:**
```json
{ "reason": "개인 사정으로 취소합니다." }
```

**Response 200:**
```json
{
  "success": true,
  "data": { "tradeId": 50, "status": "CANCELLED" }
}
```

**Error:**

| code | 설명 |
|------|------|
| `TRADE_INVALID_STATUS` | 취소 불가 상태 (COMPLETED 등) |

---

## 6. 예약 슬롯 API

### GET /slots — 판매자 슬롯 목록 🔓

**Query Parameters:**

| 파라미터 | 필수 | 설명 |
|---------|------|------|
| sellerId | Y | 판매자 userId |
| from | N | 조회 시작일 (yyyy-MM-dd, 기본: 오늘) |
| to | N | 조회 종료일 (기본: from + 30일) |

**Response 200:**
```json
{
  "success": true,
  "data": [
    {
      "slotId": 20,
      "startedAt": "2026-05-25T14:00:00",
      "endedAt": "2026-05-25T15:00:00",
      "status": "AVAILABLE"
    },
    {
      "slotId": 21,
      "startedAt": "2026-05-26T10:00:00",
      "endedAt": "2026-05-26T11:00:00",
      "status": "RESERVED"
    }
  ]
}
```

---

### POST /slots — 슬롯 등록 🔒

**Request Body:**
```json
{
  "startedAt": "2026-05-25T14:00:00",
  "endedAt":   "2026-05-25T15:00:00",
  "repeat": {
    "enabled": false,
    "daysOfWeek": [1, 3],
    "until": "2026-06-25"
  }
}
```

| 필드 | 타입 | 필수 | 유효성 |
|------|------|------|--------|
| startedAt | datetime | Y | 현재 이후, 30일 이내 |
| endedAt | datetime | Y | startedAt 이후 |
| repeat.enabled | boolean | N | true이면 반복 등록 |
| repeat.daysOfWeek | int[] | 반복 시 Y | 0(일)~6(토) |
| repeat.until | date | 반복 시 Y | 30일 이내 |

**Response 201:**
```json
{
  "success": true,
  "data": {
    "createdSlots": [
      { "slotId": 20, "startedAt": "2026-05-25T14:00:00", "endedAt": "2026-05-25T15:00:00" }
    ],
    "count": 1
  }
}
```

**Error:**

| code | 설명 |
|------|------|
| `SLOT_DUPLICATED` | 동일 시작 시간 슬롯 존재 |
| `SLOT_DATE_OUT_OF_RANGE` | 30일 초과 |

---

### DELETE /slots/{slotId} — 슬롯 삭제 🔒

**Response 204**

**Error:**

| code | 설명 |
|------|------|
| `SLOT_IN_USE` | 예약된 슬롯 삭제 불가 |
| `FORBIDDEN` | 본인 슬롯 아님 |

---

### POST /reservations — 예약 요청 🔒

**Request Body:**
```json
{
  "slotId": 20,
  "tradeId": 50
}
```

**Response 201:**
```json
{
  "success": true,
  "data": {
    "reservationId": 10,
    "slotId": 20,
    "startedAt": "2026-05-25T14:00:00",
    "endedAt": "2026-05-25T15:00:00",
    "status": "PENDING",
    "lockExpiresAt": "2026-05-23T10:05:00"
  }
}
```

**Error:**

| code | 설명 |
|------|------|
| `SLOT_ALREADY_LOCKED` | 다른 사용자가 예약 요청 중 |
| `SLOT_NOT_AVAILABLE` | 가용 슬롯 아님 |
| `TRADE_NOT_RESERVED` | 수락된 거래 아님 |

---

### PATCH /reservations/{reservationId}/confirm — 예약 확정 🔒

> 판매자만 가능. 5분 잠금 내 호출.

**Response 200:**
```json
{
  "success": true,
  "data": {
    "reservationId": 10,
    "status": "CONFIRMED",
    "startedAt": "2026-05-25T14:00:00"
  }
}
```

**Error:**

| code | 설명 |
|------|------|
| `RESERVATION_LOCK_EXPIRED` | 5분 잠금 만료 |

---

### PATCH /reservations/{reservationId}/cancel — 예약 취소 🔒

> 예약 24시간 전까지만 가능.

**Response 200:**
```json
{
  "success": true,
  "data": { "reservationId": 10, "status": "CANCELLED" }
}
```

**Error:**

| code | 설명 |
|------|------|
| `RESERVATION_CANCEL_TOO_LATE` | 24시간 이내 취소 불가 |

---

## 7. 채팅 API

### POST /chats — 채팅방 생성 / 조회 🔒

> 상품 + 구매자 쌍으로 유니크. 이미 존재하면 기존 채팅방 반환.

**Request Body:**
```json
{ "itemId": 100 }
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "roomId": 30,
    "item": {
      "itemId": 100,
      "title": "맥북 프로 M3 14인치",
      "thumbnailUrl": "https://s3.../0.jpg",
      "price": 2500000,
      "status": "SALE"
    },
    "counterpart": {
      "userId": 1,
      "nickname": "박지훈",
      "profileImageUrl": "https://s3.../profile/1.jpg",
      "grade": "FRUIT"
    },
    "isNew": true
  }
}
```

---

### GET /chats — 채팅 목록 🔒

**Query:** `cursor`, `size`

**Response 200:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "roomId": 30,
        "item": {
          "itemId": 100,
          "title": "맥북 프로 M3 14인치",
          "thumbnailUrl": "https://s3.../0.jpg"
        },
        "counterpart": {
          "userId": 1, "nickname": "박지훈", "grade": "FRUIT"
        },
        "lastMessage": "내일 2시에 직거래 가능한가요?",
        "lastMessageAt": "2026-05-23T09:30:00",
        "unreadCount": 2
      }
    ],
    "nextCursor": "2026-05-23T09:30:00|30",
    "hasNext": false,
    "size": 20
  }
}
```

---

### GET /chats/{roomId}/messages — 메시지 목록 🔒

**Query:** `cursor`, `size` (최신 메시지 기준 역방향 페이징)

**Response 200:**
```json
{
  "success": true,
  "data": {
    "roomId": 30,
    "content": [
      {
        "messageId": 200,
        "senderId": 2,
        "type": "TEXT",
        "content": "내일 2시에 직거래 가능한가요?",
        "metadata": null,
        "isRead": true,
        "createdAt": "2026-05-23T09:30:00"
      },
      {
        "messageId": 201,
        "senderId": 1,
        "type": "PRICE_OFFER",
        "content": "45,000원에 어떠세요?",
        "metadata": { "offeredPrice": 45000, "originalPrice": 50000 },
        "isRead": false,
        "createdAt": "2026-05-23T09:32:00"
      }
    ],
    "nextCursor": "2026-05-23T09:30:00|200",
    "hasNext": true,
    "size": 30
  }
}
```

---

### PATCH /chats/{roomId}/read — 채팅방 메시지 읽음 처리 🔒

**Response 200:**
```json
{
  "success": true,
  "data": { "readCount": 2 }
}
```

---

## 8. 알림 API

### GET /notifications/stream — SSE 연결 🔒

```
GET /api/notifications/stream
Authorization: Bearer {accessToken}
Accept: text/event-stream
```

**SSE 이벤트 형식:**
```
event: notification
data: {
  "notificationId": 100,
  "type": "NEW_CHAT_MESSAGE",
  "title": "새 메시지",
  "content": "박지훈님이 메시지를 보냈습니다.",
  "linkUrl": "/chats/30",
  "createdAt": "2026-05-23T10:00:00"
}

event: heartbeat
data: ping
```

> `heartbeat`는 30초마다 전송 (연결 유지용).

---

### GET /notifications — 알림 목록 🔒

**Query:**

| 파라미터 | 설명 |
|---------|------|
| unreadOnly | true이면 미읽음만 (기본: false) |
| cursor, size | 페이지네이션 |

**Response 200:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "notificationId": 100,
        "type": "TRADE_REQUEST_RECEIVED",
        "title": "새 구매 요청",
        "content": "이소연님이 맥북 프로 M3 14인치 구매를 요청했습니다.",
        "linkUrl": "/trades",
        "isRead": false,
        "createdAt": "2026-05-23T09:00:00"
      }
    ],
    "unreadCount": 3,
    "nextCursor": "2026-05-23T09:00:00|100",
    "hasNext": false,
    "size": 20
  }
}
```

---

### PATCH /notifications/{notificationId}/read — 단건 읽음 🔒

**Response 200:**
```json
{
  "success": true,
  "data": { "notificationId": 100, "isRead": true }
}
```

---

### PATCH /notifications/read-all — 전체 읽음 🔒

**Response 200:**
```json
{
  "success": true,
  "data": { "updatedCount": 5 }
}
```

---

## 9. 리뷰 API

### POST /reviews — 리뷰 작성 🔒

**Request Body:**
```json
{
  "tradeId": 50,
  "rating": 5,
  "content": "거래 매너가 좋으셨어요. 물건 상태도 설명과 동일했습니다."
}
```

| 필드 | 타입 | 필수 | 유효성 |
|------|------|------|--------|
| tradeId | long | Y | 완료된 거래, 7일 이내 |
| rating | int | Y | 1 ~ 5 |
| content | string | N | 최대 500자 |

**Response 201:**
```json
{
  "success": true,
  "data": {
    "reviewId": 70,
    "rating": 5,
    "content": "거래 매너가 좋으셨어요.",
    "createdAt": "2026-05-23T11:00:00"
  }
}
```

**Error:**

| code | 설명 |
|------|------|
| `REVIEW_TRADE_NOT_COMPLETED` | 완료되지 않은 거래 |
| `REVIEW_PERIOD_EXPIRED` | 7일 초과 |
| `REVIEW_ALREADY_WRITTEN` | 이미 작성한 리뷰 |

---

### GET /users/{userId}/reviews — 받은 리뷰 목록 🔓

**Query:** `cursor`, `size`

**Response 200:**
```json
{
  "success": true,
  "data": {
    "averageRating": 4.7,
    "reviewCount": 10,
    "content": [
      {
        "reviewId": 70,
        "rating": 5,
        "content": "거래 매너가 좋으셨어요.",
        "reviewer": {
          "userId": 2, "nickname": "이소연", "grade": "SPROUT"
        },
        "reply": null,
        "createdAt": "2026-05-23T11:00:00"
      }
    ],
    "nextCursor": "2026-05-23T11:00:00|70",
    "hasNext": false,
    "size": 20
  }
}
```

---

### POST /reviews/{reviewId}/reply — 답글 작성 🔒

> 리뷰 피작성자만 가능. 1회만 가능.

**Request Body:**
```json
{ "reply": "감사합니다! 좋은 거래였습니다." }
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "reviewId": 70,
    "reply": "감사합니다! 좋은 거래였습니다.",
    "repliedAt": "2026-05-24T09:00:00"
  }
}
```

**Error:**

| code | 설명 |
|------|------|
| `REVIEW_REPLY_ALREADY_EXISTS` | 이미 답글 작성 |
| `FORBIDDEN` | 피작성자 아님 |

---

## 10. 신고 API

### POST /reports — 신고 접수 🔒

**Request Body:**
```json
{
  "targetType": "ITEM",
  "targetId": 100,
  "reason": "FRAUD",
  "content": "실제 물품과 사진이 다릅니다."
}
```

| 필드 | 타입 | 필수 | 유효성 |
|------|------|------|--------|
| targetType | string | Y | ITEM / USER |
| targetId | long | Y | 유효한 상품/사용자 ID |
| reason | string | Y | ILLEGAL / FALSE_INFO / FRAUD / ETC |
| content | string | N | 최대 500자 |

**Response 201:**
```json
{
  "success": true,
  "data": {
    "reportId": 15,
    "message": "신고가 접수되었습니다. 검토 후 처리 결과를 알려드립니다."
  }
}
```

**Error:**

| code | 설명 |
|------|------|
| `REPORT_SELF_REPORT` | 본인 신고 불가 |
| `REPORT_ALREADY_SUBMITTED` | 동일 대상 중복 신고 |

---

## 11. AI API

### POST /ai/item-description — 상품 설명 자동완성 🔒

**Request Body:**
```json
{
  "title": "맥북 프로 M3 14인치",
  "categoryId": 12
}
```

**Response 200 (스트리밍: text/event-stream):**
```
data: {"chunk": "2024년에 구매한 맥북 프로 M3 14인치입니다. "}
data: {"chunk": "사용감이 적고 박스와 충전기가 포함되어 있습니다. "}
data: {"chunk": "배터리 사이클은 50회 미만입니다."}
data: {"done": true, "fullText": "2024년에 구매한 맥북 프로 M3 14인치입니다. ..."}
```

> 스트리밍 실패 시 일반 JSON 응답으로 Fallback.

**Error:**

| code | 설명 |
|------|------|
| `AI_SERVICE_UNAVAILABLE` | Claude API 호출 실패 |
| `AI_RATE_LIMIT` | AI 기능 사용 횟수 초과 (사용자당 10회/일) |

---

### POST /ai/price-recommendation — 가격 추천 🔒

**Request Body:**
```json
{
  "title": "맥북 프로 M3 14인치",
  "categoryId": 12,
  "description": "2024년 구매, 사용감 적음"
}
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "minPrice": 2200000,
    "maxPrice": 2600000,
    "recommendedPrice": 2400000,
    "basis": "최근 동일 기종 중고 거래 가격 기준 추천가입니다."
  }
}
```

---

## 12. 관리자 API

> Base: `/admin/api`  
> 모든 엔드포인트 👑 (Admin 권한 필수)

---

### GET /admin/api/dashboard — 대시보드

**Response 200:**
```json
{
  "success": true,
  "data": {
    "today": {
      "newUsers": 15,
      "newTrades": 42,
      "completedTrades": 30,
      "newReports": 3,
      "newItems": 87
    },
    "pending": {
      "reports": 5
    }
  }
}
```

---

### GET /admin/api/users — 회원 목록

**Query:**

| 파라미터 | 설명 |
|---------|------|
| keyword | 이메일 / 닉네임 검색 |
| status | ACTIVE / SUSPENDED / WITHDRAWN |
| cursor, size | 페이지네이션 |

**Response 200:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "userId": 2,
        "email": "lee@example.com",
        "nickname": "이소연",
        "trustScore": 50,
        "grade": "SPROUT",
        "status": "ACTIVE",
        "tradeCount": 3,
        "reportCount": 0,
        "createdAt": "2026-02-01T09:00:00"
      }
    ],
    "nextCursor": "...",
    "hasNext": true,
    "size": 20
  }
}
```

---

### GET /admin/api/users/{userId} — 회원 상세

**Response 200:**
```json
{
  "success": true,
  "data": {
    "userId": 2,
    "email": "lee@example.com",
    "nickname": "이소연",
    "trustScore": 50,
    "grade": "SPROUT",
    "status": "ACTIVE",
    "suspendedUntil": null,
    "emailVerified": true,
    "provider": "LOCAL",
    "tradeCount": 3,
    "reportCount": 0,
    "reportedCount": 1,
    "createdAt": "2026-02-01T09:00:00"
  }
}
```

---

### PATCH /admin/api/users/{userId}/status — 회원 상태 변경

**Request Body:**
```json
{
  "status": "SUSPENDED",
  "suspendedUntil": "2026-06-01T00:00:00",
  "reason": "사기 거래 신고 처리",
  "deductTrustScore": true
}
```

| status 값 | 설명 |
|-----------|------|
| ACTIVE | 활성화 |
| SUSPENDED | 정지 (suspendedUntil 필수) |
| WITHDRAWN | 강제 탈퇴 |

**Response 200:**
```json
{
  "success": true,
  "data": {
    "userId": 2,
    "status": "SUSPENDED",
    "suspendedUntil": "2026-06-01T00:00:00",
    "trustScore": 40
  }
}
```

---

### GET /admin/api/items — 상품 목록

**Query:** `keyword`, `status`, `categoryId`, `cursor`, `size`

**Response 200:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "itemId": 100,
        "title": "맥북 프로 M3 14인치",
        "price": 2500000,
        "status": "SALE",
        "seller": { "userId": 1, "nickname": "박지훈" },
        "reportCount": 2,
        "createdAt": "2026-05-20T10:00:00"
      }
    ],
    "nextCursor": "...",
    "hasNext": true,
    "size": 20
  }
}
```

---

### PATCH /admin/api/items/{itemId}/visibility — 강제 비공개

**Request Body:**
```json
{ "hidden": true, "reason": "불법 물품 의심" }
```

**Response 200:**
```json
{
  "success": true,
  "data": { "itemId": 100, "status": "HIDDEN" }
}
```

---

### DELETE /admin/api/items/{itemId} — 상품 강제 삭제

**Response 204**

---

### GET /admin/api/reports — 신고 목록

**Query:**

| 파라미터 | 설명 |
|---------|------|
| status | PENDING / WARNING / SUSPENDED / REJECTED |
| targetType | ITEM / USER |
| cursor, size | 페이지네이션 |

**Response 200:**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "reportId": 15,
        "targetType": "ITEM",
        "targetId": 100,
        "targetSummary": "맥북 프로 M3 14인치",
        "reporter": { "userId": 2, "nickname": "이소연" },
        "reason": "FRAUD",
        "content": "실제 물품과 사진이 다릅니다.",
        "status": "PENDING",
        "createdAt": "2026-05-23T09:00:00"
      }
    ],
    "nextCursor": "...",
    "hasNext": false,
    "size": 20
  }
}
```

---

### POST /admin/api/reports/{reportId}/process — 신고 처리

**Request Body:**
```json
{
  "action": "SUSPEND_7DAYS",
  "adminNote": "동일 피신고자 3건 이상으로 7일 정지 처리",
  "deductTrustScore": true
}
```

| action 값 | 설명 |
|-----------|------|
| `WARNING` | 경고 처리 (신뢰 점수 -10) |
| `SUSPEND_7DAYS` | 7일 정지 |
| `SUSPEND_PERMANENT` | 영구 정지 |
| `REJECT` | 신고 기각 |
| `HIDE_ITEM` | 상품 비공개 |

**Response 200:**
```json
{
  "success": true,
  "data": {
    "reportId": 15,
    "status": "SUSPENDED",
    "processedAt": "2026-05-23T12:00:00"
  }
}
```

---

### POST /admin/api/reports/summary — AI 신고 요약 👑

**Request Body:**
```json
{ "reportIds": [15, 16, 17] }
```

**Response 200:**
```json
{
  "success": true,
  "data": {
    "summary": "해당 판매자는 3건의 신고에서 공통적으로 실제 물품과 설명이 다르다는 내용이 제기되었습니다. 특히 상품 이미지와 실물의 차이가 지속적으로 언급되어 허위 정보 등록 패턴이 의심됩니다."
  }
}
```

---

### GET /admin/api/categories — 카테고리 관리 목록 👑

**Response 200:** (4. 상품 API의 `GET /categories`와 동일 형식)

---

### POST /admin/api/categories — 카테고리 추가 👑

**Request Body:**
```json
{ "parentId": 1, "name": "스마트워치", "sortOrder": 6 }
```

**Response 201:**
```json
{
  "success": true,
  "data": { "categoryId": 16, "name": "스마트워치" }
}
```

---

### PUT /admin/api/categories/{categoryId} — 카테고리 수정 👑

**Request Body:**
```json
{ "name": "스마트워치/밴드", "sortOrder": 6 }
```

**Response 200:**
```json
{
  "success": true,
  "data": { "categoryId": 16, "name": "스마트워치/밴드" }
}
```

---

### DELETE /admin/api/categories/{categoryId} — 카테고리 삭제 👑

> 해당 카테고리에 상품이 있으면 삭제 불가.

**Response 204**

**Error:**

| code | 설명 |
|------|------|
| `CATEGORY_IN_USE` | 상품이 있는 카테고리 |

---

### GET /admin/api/stats — 거래 통계 👑

**Query:**

| 파라미터 | 설명 |
|---------|------|
| from | 시작일 (yyyy-MM-dd) |
| to | 종료일 (yyyy-MM-dd) |
| groupBy | DAY / WEEK / MONTH |

**Response 200:**
```json
{
  "success": true,
  "data": {
    "period": { "from": "2026-05-01", "to": "2026-05-23" },
    "summary": {
      "totalCompleted": 320,
      "totalCancelled": 45,
      "cancelRate": 12.3,
      "avgDailyTrades": 15.6
    },
    "daily": [
      {
        "date": "2026-05-01",
        "completed": 18,
        "cancelled": 3
      }
    ],
    "byCategory": [
      { "categoryName": "전자기기", "completed": 120, "ratio": 37.5 },
      { "categoryName": "도서",     "completed": 80,  "ratio": 25.0 }
    ]
  }
}
```

---

## 13. 실시간 통신

### 13.1 WebSocket — 채팅

**연결:**
```
ws://host/ws/chat?token={accessToken}
```

**STOMP 프로토콜 사용:**

```
# 구독 (채팅방 입장)
SUBSCRIBE /topic/chat/{roomId}

# 메시지 전송
SEND /app/chat.send
{
  "roomId": 30,
  "type": "TEXT",
  "content": "안녕하세요!",
  "metadata": null
}

# 읽음 처리
SEND /app/chat.read
{ "roomId": 30 }
```

**서버 → 클라이언트 메시지 형식:**
```json
{
  "messageId": 201,
  "roomId": 30,
  "senderId": 1,
  "senderNickname": "박지훈",
  "type": "TEXT",
  "content": "안녕하세요!",
  "metadata": null,
  "createdAt": "2026-05-23T10:00:00"
}
```

**에러 메시지:**
```json
{
  "type": "ERROR",
  "code": "CHAT_ROOM_NOT_FOUND",
  "message": "존재하지 않는 채팅방입니다."
}
```

---

### 13.2 SSE — 알림

**연결:**
```
GET /api/notifications/stream
Authorization: Bearer {accessToken}
```

**이벤트 목록:**

| event 이름 | 설명 |
|-----------|------|
| `notification` | 새 알림 발생 |
| `heartbeat` | 연결 유지 핑 (30초) |
| `connect` | 연결 성공 확인 |

**connect 이벤트:**
```
event: connect
data: {"message": "connected", "userId": 1}
```

**notification 이벤트:**
```
event: notification
data: {
  "notificationId": 100,
  "type": "NEW_CHAT_MESSAGE",
  "title": "새 메시지",
  "content": "박지훈님이 메시지를 보냈습니다.",
  "linkUrl": "/chats/30",
  "createdAt": "2026-05-23T10:00:00"
}
```

---

> **다음 단계:** `UI Spec.md` — 화면 정의서, 컴포넌트 레이아웃, 인터랙션 명세
