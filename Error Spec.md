# Error Spec — Tradev (중고거래 / 예약 플랫폼)

> **문서 버전:** v1.0.0  
> **작성일:** 2026-05-23  
> **연관 문서:** [API Spec.md](API%20Spec.md) · [Architecture.md](Architecture.md) · [UI Spec.md](UI%20Spec.md)

---

## 목차

1. [에러 처리 원칙](#1-에러-처리-원칙)
2. [에러 응답 형식](#2-에러-응답-형식)
3. [에러 코드 전체 목록](#3-에러-코드-전체-목록)
   - 3.1 공통 (COMMON)
   - 3.2 인증 (AUTH)
   - 3.3 사용자 (USER)
   - 3.4 상품 (ITEM)
   - 3.5 거래 (TRADE)
   - 3.6 예약 슬롯 (SLOT / RESERVATION)
   - 3.7 채팅 (CHAT)
   - 3.8 알림 (NOTIFICATION)
   - 3.9 리뷰 (REVIEW)
   - 3.10 신고 (REPORT)
   - 3.11 AI (AI)
   - 3.12 카테고리 (CATEGORY)
   - 3.13 파일 (FILE)
4. [서버 에러 처리 전략](#4-서버-에러-처리-전략)
5. [클라이언트 에러 처리 전략](#5-클라이언트-에러-처리-전략)
6. [특수 에러 시나리오](#6-특수-에러-시나리오)
7. [사용자 노출 메시지 가이드](#7-사용자-노출-메시지-가이드)

---

## 1. 에러 처리 원칙

### 1.1 기본 원칙

1. **모든 에러는 일관된 형식**으로 응답한다. 프레임워크 기본 에러 응답(Spring의 기본 `/error`)을 클라이언트에 노출하지 않는다.
2. **에러 코드(code)는 클라이언트가 분기 처리**할 수 있도록 도메인별 의미를 담는다. HTTP 상태 코드만으로는 맥락을 알 수 없다.
3. **사용자에게 노출되는 메시지(message)** 는 기술적 세부 사항을 담지 않는다. 스택 트레이스, SQL, 내부 클래스명은 절대 노출하지 않는다.
4. **서버 내부 오류(5xx)** 는 최소한의 메시지만 노출하고, 상세 내용은 로그로만 기록한다.
5. **보안 관련 에러** 는 공격자에게 힌트를 주지 않도록 의도적으로 모호하게 처리한다. (예: 비밀번호 재설정 시 이메일 미존재 여부 미노출)
6. **유효성 검사 오류**는 어떤 필드가 왜 잘못되었는지 명확히 알려준다.

### 1.2 에러 코드 명명 규칙

```
{DOMAIN}_{ERROR_NAME}

예시:
  AUTH_TOKEN_EXPIRED      — 인증 도메인, 토큰 만료
  ITEM_NOT_FOUND          — 상품 도메인, 리소스 없음
  TRADE_INVALID_STATUS    — 거래 도메인, 상태 오류
  SLOT_ALREADY_LOCKED     — 슬롯 도메인, 잠금 충돌
```

---

## 2. 에러 응답 형식

### 2.1 기본 에러 응답

```json
{
  "success": false,
  "data": null,
  "message": "사용자에게 보여줄 메시지",
  "code": "DOMAIN_ERROR_NAME"
}
```

### 2.2 유효성 검사 에러 (400)

필드별 오류 상세를 함께 반환한다.

```json
{
  "success": false,
  "data": null,
  "message": "입력값을 확인해주세요.",
  "code": "VALIDATION_ERROR",
  "errors": [
    {
      "field": "email",
      "value": "invalid-email",
      "reason": "올바른 이메일 형식이 아닙니다."
    },
    {
      "field": "password",
      "value": null,
      "reason": "비밀번호는 8자 이상이어야 합니다."
    }
  ]
}
```

### 2.3 낙관적 잠금 충돌 에러 (409)

```json
{
  "success": false,
  "data": null,
  "message": "다른 사용자가 동시에 수정했습니다. 페이지를 새로고침 후 다시 시도해주세요.",
  "code": "OPTIMISTIC_LOCK"
}
```

### 2.4 계정 정지 에러 (401)

```json
{
  "success": false,
  "data": {
    "suspendedUntil": "2026-06-01T00:00:00"
  },
  "message": "계정이 정지되었습니다. 정지 해제일: 2026년 6월 1일",
  "code": "AUTH_USER_SUSPENDED"
}
```

---

## 3. 에러 코드 전체 목록

### 3.1 공통 (COMMON)

| code | HTTP | 상황 | 사용자 메시지 |
|------|------|------|--------------|
| `VALIDATION_ERROR` | 400 | 요청 필드 유효성 실패 | "입력값을 확인해주세요." |
| `FORBIDDEN` | 403 | 리소스 접근 권한 없음 | "접근 권한이 없습니다." |
| `NOT_FOUND` | 404 | 리소스 미존재 (일반) | "요청한 정보를 찾을 수 없습니다." |
| `METHOD_NOT_ALLOWED` | 405 | 허용되지 않은 HTTP 메서드 | "잘못된 요청입니다." |
| `CONFLICT` | 409 | 중복 또는 비즈니스 충돌 (일반) | "요청을 처리할 수 없습니다." |
| `OPTIMISTIC_LOCK` | 409 | JPA 낙관적 잠금 충돌 | "다른 사용자가 동시에 수정했습니다. 새로고침 후 다시 시도해주세요." |
| `RATE_LIMIT_EXCEEDED` | 429 | IP / 사용자 기준 요청 초과 | "요청이 너무 많습니다. 잠시 후 다시 시도해주세요." |
| `INTERNAL_ERROR` | 500 | 처리되지 않은 서버 오류 | "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요." |
| `SERVICE_UNAVAILABLE` | 503 | 외부 서비스 응답 불가 | "서비스가 일시적으로 이용 불가합니다. 잠시 후 다시 시도해주세요." |

---

### 3.2 인증 (AUTH)

| code | HTTP | 상황 | 사용자 메시지 |
|------|------|------|--------------|
| `AUTH_REQUIRED` | 401 | 인증 토큰 없음 | "로그인이 필요한 서비스입니다." |
| `AUTH_TOKEN_EXPIRED` | 401 | Access Token 만료 | (클라이언트가 자동 재발급 처리, 사용자 노출 없음) |
| `AUTH_TOKEN_INVALID` | 401 | 토큰 형식 / 서명 오류 | "인증 정보가 올바르지 않습니다. 다시 로그인해주세요." |
| `AUTH_REFRESH_EXPIRED` | 401 | Refresh Token 만료 | "로그인이 만료되었습니다. 다시 로그인해주세요." |
| `AUTH_REFRESH_INVALID` | 401 | Refresh Token 무효 (로그아웃 / 재발급 후 구 토큰) | "인증 정보가 만료되었습니다. 다시 로그인해주세요." |
| `AUTH_INVALID_CREDENTIALS` | 401 | 이메일/비밀번호 불일치 | "이메일 또는 비밀번호가 올바르지 않습니다." |
| `AUTH_EMAIL_NOT_VERIFIED` | 403 | 이메일 미인증 상태 | "이메일 인증이 필요합니다. 인증 메일을 확인해주세요." |
| `AUTH_USER_SUSPENDED` | 403 | 정지된 계정 | "계정이 정지되었습니다. 정지 해제일: {date}" |
| `AUTH_USER_WITHDRAWN` | 403 | 탈퇴한 계정 | "탈퇴한 계정입니다." |
| `AUTH_EMAIL_DUPLICATED` | 409 | 이메일 중복 | "이미 사용 중인 이메일입니다." |
| `AUTH_NICKNAME_DUPLICATED` | 409 | 닉네임 중복 | "이미 사용 중인 닉네임입니다." |
| `AUTH_VERIFY_TOKEN_INVALID` | 400 | 유효하지 않은 이메일 인증 토큰 | "유효하지 않은 인증 링크입니다." |
| `AUTH_VERIFY_TOKEN_EXPIRED` | 400 | 만료된 이메일 인증 토큰 (24시간) | "인증 링크가 만료되었습니다. 인증 메일을 재발송해주세요." |
| `AUTH_RESET_TOKEN_INVALID` | 400 | 유효하지 않은 비밀번호 재설정 토큰 | "유효하지 않은 재설정 링크입니다." |
| `AUTH_RESET_TOKEN_EXPIRED` | 400 | 만료된 비밀번호 재설정 토큰 (1시간) | "재설정 링크가 만료되었습니다. 다시 요청해주세요." |
| `AUTH_RESEND_TOO_SOON` | 429 | 재발송 60초 쿨다운 | "잠시 후 다시 시도해주세요. ({n}초 남음)" |
| `AUTH_OAUTH_FAILED` | 400 | OAuth2 인증 실패 (Google 응답 오류) | "소셜 로그인에 실패했습니다. 다시 시도해주세요." |

---

### 3.3 사용자 (USER)

| code | HTTP | 상황 | 사용자 메시지 |
|------|------|------|--------------|
| `USER_NOT_FOUND` | 404 | 존재하지 않는 사용자 | "존재하지 않는 사용자입니다." |
| `USER_ALREADY_WITHDRAWN` | 409 | 이미 탈퇴한 계정 | "이미 탈퇴한 계정입니다." |
| `USER_INVALID_PASSWORD` | 400 | 탈퇴 시 비밀번호 불일치 | "비밀번호가 올바르지 않습니다." |

---

### 3.4 상품 (ITEM)

| code | HTTP | 상황 | 사용자 메시지 |
|------|------|------|--------------|
| `ITEM_NOT_FOUND` | 404 | 존재하지 않는 상품 | "존재하지 않는 상품입니다." |
| `ITEM_DELETED` | 404 | 삭제된 상품 | "삭제된 상품입니다." |
| `ITEM_HIDDEN` | 403 | 숨김 처리된 상품 (타인 조회) | "비공개 처리된 상품입니다." |
| `ITEM_ACCESS_DENIED` | 403 | 본인 상품 아님 (수정/삭제 시도) | "해당 상품에 대한 권한이 없습니다." |
| `ITEM_NOT_EDITABLE` | 409 | 거래 완료 상품 수정 시도 | "거래가 완료된 상품은 수정할 수 없습니다." |
| `ITEM_IN_TRADE` | 409 | 거래 진행 중 삭제 시도 | "거래가 진행 중인 상품은 삭제할 수 없습니다." |
| `ITEM_BOOST_LIMIT` | 429 | 하루 1회 끌어올리기 초과 | "오늘은 이미 끌어올리기를 사용했습니다. 내일 다시 시도해주세요." |
| `ITEM_INVALID_CATEGORY` | 400 | 유효하지 않은 카테고리 ID | "유효하지 않은 카테고리입니다." |
| `ITEM_INVALID_PRICE` | 400 | 음수 가격 | "가격은 0원 이상이어야 합니다." |
| `ITEM_IMAGE_REQUIRED` | 400 | 이미지 0장 | "상품 이미지를 1장 이상 등록해주세요." |
| `ITEM_IMAGE_LIMIT_EXCEEDED` | 400 | 이미지 10장 초과 | "이미지는 최대 10장까지 등록할 수 있습니다." |

---

### 3.5 거래 (TRADE)

| code | HTTP | 상황 | 사용자 메시지 |
|------|------|------|--------------|
| `TRADE_NOT_FOUND` | 404 | 존재하지 않는 거래 | "존재하지 않는 거래입니다." |
| `TRADE_ACCESS_DENIED` | 403 | 거래 당사자 아님 | "해당 거래에 대한 권한이 없습니다." |
| `TRADE_ITEM_NOT_AVAILABLE` | 409 | 판매중 상태 아닌 상품 구매 요청 | "현재 구매 요청이 불가능한 상품입니다." |
| `TRADE_SELF_REQUEST` | 400 | 본인 상품 구매 요청 | "본인 상품에는 구매 요청을 할 수 없습니다." |
| `TRADE_ALREADY_PENDING` | 409 | 동일 상품에 진행 중인 요청 존재 | "이미 진행 중인 구매 요청이 있습니다." |
| `TRADE_INVALID_STATUS` | 409 | 현재 상태에서 허용되지 않는 전이 | "현재 상태에서는 해당 작업을 수행할 수 없습니다." |
| `TRADE_ALREADY_CONFIRMED` | 409 | 이미 거래 완료 확인한 경우 | "이미 거래 완료 확인을 하셨습니다." |
| `TRADE_SELLER_ONLY` | 403 | 판매자만 가능한 작업을 구매자가 시도 | "판매자만 수행할 수 있는 작업입니다." |
| `TRADE_BUYER_ONLY` | 403 | 구매자만 가능한 작업을 판매자가 시도 | "구매자만 수행할 수 있는 작업입니다." |

---

### 3.6 예약 슬롯 (SLOT / RESERVATION)

| code | HTTP | 상황 | 사용자 메시지 |
|------|------|------|--------------|
| `SLOT_NOT_FOUND` | 404 | 존재하지 않는 슬롯 | "존재하지 않는 슬롯입니다." |
| `SLOT_ACCESS_DENIED` | 403 | 본인 슬롯 아님 | "해당 슬롯에 대한 권한이 없습니다." |
| `SLOT_DUPLICATED` | 409 | 동일 판매자 동일 시작 시간 슬롯 존재 | "해당 시간에 이미 슬롯이 등록되어 있습니다." |
| `SLOT_DATE_OUT_OF_RANGE` | 400 | 오늘 이전 또는 30일 이후 슬롯 등록 | "슬롯은 오늘부터 30일 이내로만 등록할 수 있습니다." |
| `SLOT_INVALID_TIME` | 400 | 종료 시간 ≤ 시작 시간 | "종료 시간은 시작 시간 이후여야 합니다." |
| `SLOT_IN_USE` | 409 | 예약된 슬롯 삭제 시도 | "예약된 슬롯은 삭제할 수 없습니다." |
| `SLOT_ALREADY_LOCKED` | 409 | Redis 잠금된 슬롯 예약 요청 | "다른 사용자가 예약을 진행 중입니다. 잠시 후 다시 시도해주세요." |
| `SLOT_NOT_AVAILABLE` | 409 | AVAILABLE 상태 아닌 슬롯 예약 요청 | "예약 가능한 슬롯이 아닙니다." |
| `RESERVATION_NOT_FOUND` | 404 | 존재하지 않는 예약 | "존재하지 않는 예약입니다." |
| `RESERVATION_ACCESS_DENIED` | 403 | 예약 당사자 아님 | "해당 예약에 대한 권한이 없습니다." |
| `RESERVATION_LOCK_EXPIRED` | 409 | 5분 임시 잠금 만료 후 수락 시도 | "예약 요청이 만료되었습니다. 구매자에게 재요청을 안내해주세요." |
| `RESERVATION_CANCEL_TOO_LATE` | 409 | 예약 시간 24시간 이내 취소 시도 | "예약 취소는 24시간 전까지만 가능합니다." |
| `RESERVATION_ALREADY_EXISTS` | 409 | 동일 슬롯에 확정된 예약 이미 존재 | "이미 예약이 확정된 슬롯입니다." |

---

### 3.7 채팅 (CHAT)

| code | HTTP | 상황 | 사용자 메시지 |
|------|------|------|--------------|
| `CHAT_ROOM_NOT_FOUND` | 404 | 존재하지 않는 채팅방 | "존재하지 않는 채팅방입니다." |
| `CHAT_ACCESS_DENIED` | 403 | 채팅 당사자 아님 | "해당 채팅방에 대한 권한이 없습니다." |
| `CHAT_SELF_CHAT` | 400 | 본인 상품으로 채팅 생성 시도 | "본인 상품에는 채팅을 시작할 수 없습니다." |
| `CHAT_ITEM_UNAVAILABLE` | 409 | 거래완료/삭제 상품으로 채팅 생성 | "더 이상 거래가 불가능한 상품입니다." |
| `CHAT_MESSAGE_EMPTY` | 400 | 빈 메시지 전송 | "메시지를 입력해주세요." |
| `CHAT_MESSAGE_TOO_LONG` | 400 | 메시지 1000자 초과 | "메시지는 1,000자 이하로 입력해주세요." |

---

### 3.8 알림 (NOTIFICATION)

| code | HTTP | 상황 | 사용자 메시지 |
|------|------|------|--------------|
| `NOTIFICATION_NOT_FOUND` | 404 | 존재하지 않는 알림 | "존재하지 않는 알림입니다." |
| `NOTIFICATION_ACCESS_DENIED` | 403 | 본인 알림 아님 | "해당 알림에 대한 권한이 없습니다." |

---

### 3.9 리뷰 (REVIEW)

| code | HTTP | 상황 | 사용자 메시지 |
|------|------|------|--------------|
| `REVIEW_NOT_FOUND` | 404 | 존재하지 않는 리뷰 | "존재하지 않는 리뷰입니다." |
| `REVIEW_ACCESS_DENIED` | 403 | 해당 거래 당사자 아님 | "해당 리뷰에 대한 권한이 없습니다." |
| `REVIEW_TRADE_NOT_COMPLETED` | 409 | 완료되지 않은 거래에 리뷰 작성 | "거래가 완료된 후 리뷰를 작성할 수 있습니다." |
| `REVIEW_PERIOD_EXPIRED` | 409 | 거래 완료 후 7일 초과 | "리뷰 작성 가능 기간(7일)이 지났습니다." |
| `REVIEW_ALREADY_WRITTEN` | 409 | 동일 거래 중복 리뷰 | "이미 리뷰를 작성했습니다." |
| `REVIEW_REPLY_ALREADY_EXISTS` | 409 | 답글 중복 작성 | "이미 답글을 작성했습니다." |
| `REVIEW_REPLY_ACCESS_DENIED` | 403 | 리뷰 피작성자 아님 | "본인이 받은 리뷰에만 답글을 작성할 수 있습니다." |
| `REVIEW_INVALID_RATING` | 400 | 별점 범위 오류 (1~5 외) | "별점은 1~5 사이로 입력해주세요." |

---

### 3.10 신고 (REPORT)

| code | HTTP | 상황 | 사용자 메시지 |
|------|------|------|--------------|
| `REPORT_NOT_FOUND` | 404 | 존재하지 않는 신고 | "존재하지 않는 신고입니다." |
| `REPORT_SELF_REPORT` | 400 | 본인 신고 | "본인을 신고할 수 없습니다." |
| `REPORT_ALREADY_SUBMITTED` | 409 | 동일 대상 중복 신고 | "이미 신고한 대상입니다. 처리 결과를 기다려주세요." |
| `REPORT_TARGET_NOT_FOUND` | 404 | 신고 대상(상품/사용자) 미존재 | "신고 대상을 찾을 수 없습니다." |
| `REPORT_INVALID_TARGET_TYPE` | 400 | targetType이 ITEM/USER 외 | "잘못된 신고 대상 유형입니다." |

---

### 3.11 AI (AI)

| code | HTTP | 상황 | 사용자 메시지 |
|------|------|------|--------------|
| `AI_SERVICE_UNAVAILABLE` | 503 | Claude API 호출 실패 / 타임아웃 | "AI 기능이 일시적으로 이용 불가합니다. 직접 입력해주세요." |
| `AI_RATE_LIMIT` | 429 | 사용자당 AI 일일 한도 초과 (10회) | "오늘 AI 기능을 모두 사용했습니다. 내일 다시 이용해주세요." |
| `AI_INVALID_INPUT` | 400 | 제목 또는 카테고리 미입력 상태에서 AI 요청 | "제목과 카테고리를 먼저 입력해주세요." |
| `AI_CONTENT_POLICY` | 422 | Claude API 콘텐츠 정책 위반 감지 | "입력 내용이 정책에 위반될 수 있습니다. 내용을 수정 후 다시 시도해주세요." |

---

### 3.12 카테고리 (CATEGORY)

| code | HTTP | 상황 | 사용자 메시지 |
|------|------|------|--------------|
| `CATEGORY_NOT_FOUND` | 404 | 존재하지 않는 카테고리 | "존재하지 않는 카테고리입니다." |
| `CATEGORY_IN_USE` | 409 | 상품이 등록된 카테고리 삭제 시도 | "상품이 등록된 카테고리는 삭제할 수 없습니다." |
| `CATEGORY_INVALID_PARENT` | 400 | 중분류를 부모로 설정 (3단계 방지) | "대분류 카테고리 하위에만 중분류를 추가할 수 있습니다." |

---

### 3.13 파일 (FILE)

| code | HTTP | 상황 | 사용자 메시지 |
|------|------|------|--------------|
| `FILE_SIZE_EXCEEDED` | 400 | 업로드 파일 5MB 초과 | "파일 크기는 5MB 이하여야 합니다." |
| `FILE_INVALID_TYPE` | 400 | 허용되지 않는 파일 형식 | "JPG, PNG, WEBP 형식의 이미지만 업로드할 수 있습니다." |
| `FILE_UPLOAD_FAILED` | 500 | S3 업로드 실패 | "이미지 업로드에 실패했습니다. 다시 시도해주세요." |
| `FILE_NOT_FOUND` | 404 | Presigned URL 만료 후 접근 등 | "파일을 찾을 수 없습니다." |

---

## 4. 서버 에러 처리 전략

### 4.1 CustomException 설계

```java
// 최상위 예외 클래스
public class TradevException extends RuntimeException {
    private final ErrorCode errorCode;

    public TradevException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public TradevException(ErrorCode errorCode, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
    }
}

// ErrorCode enum
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    VALIDATION_ERROR      (400, "VALIDATION_ERROR",       "입력값을 확인해주세요."),
    FORBIDDEN             (403, "FORBIDDEN",               "접근 권한이 없습니다."),
    NOT_FOUND             (404, "NOT_FOUND",               "요청한 정보를 찾을 수 없습니다."),
    OPTIMISTIC_LOCK       (409, "OPTIMISTIC_LOCK",         "다른 사용자가 동시에 수정했습니다. 새로고침 후 다시 시도해주세요."),
    RATE_LIMIT_EXCEEDED   (429, "RATE_LIMIT_EXCEEDED",     "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
    INTERNAL_ERROR        (500, "INTERNAL_ERROR",          "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),

    // Auth
    AUTH_REQUIRED         (401, "AUTH_REQUIRED",           "로그인이 필요한 서비스입니다."),
    AUTH_TOKEN_EXPIRED    (401, "AUTH_TOKEN_EXPIRED",      "인증이 만료되었습니다."),
    AUTH_INVALID_CREDENTIALS(401, "AUTH_INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다."),
    // ... 이하 생략

    private final int httpStatus;
    private final String code;
    private final String message;
}
```

### 4.2 GlobalExceptionHandler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 1. 도메인 예외 처리
    @ExceptionHandler(TradevException.class)
    public ResponseEntity<ApiResponse<?>> handleTradevException(TradevException e) {
        ErrorCode code = e.getErrorCode();
        // WARN: 비즈니스 규칙 위반 (4xx)
        // ERROR: 예상치 못한 5xx는 여기 도달 안 함 (아래에서 처리)
        if (code.getHttpStatus() >= 500) {
            log.error("[TradevException] code={}, message={}", code, e.getMessage(), e);
        } else {
            log.warn("[TradevException] code={}, message={}", code, e.getMessage());
        }
        return ResponseEntity
            .status(code.getHttpStatus())
            .body(ApiResponse.error(code.getCode(), code.getMessage()));
    }

    // 2. 유효성 검사 예외 (@Valid 실패)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException e) {
        List<FieldError> errors = e.getBindingResult().getFieldErrors()
            .stream()
            .map(fe -> new FieldError(fe.getField(), fe.getRejectedValue(), fe.getDefaultMessage()))
            .toList();
        return ResponseEntity.badRequest()
            .body(ApiResponse.validationError(errors));
    }

    // 3. 낙관적 잠금 충돌
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<?>> handleOptimisticLock(ObjectOptimisticLockingFailureException e) {
        log.warn("[OptimisticLock] entity={}", e.getPersistentClassName());
        return ResponseEntity.status(409)
            .body(ApiResponse.error("OPTIMISTIC_LOCK",
                "다른 사용자가 동시에 수정했습니다. 새로고침 후 다시 시도해주세요."));
    }

    // 4. DB 유니크 제약 위반
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("[DataIntegrity] {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(409)
            .body(ApiResponse.error("CONFLICT", "요청을 처리할 수 없습니다."));
    }

    // 5. 인증 예외 (Spring Security)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(403)
            .body(ApiResponse.error("FORBIDDEN", "접근 권한이 없습니다."));
    }

    // 6. 처리되지 않은 모든 예외 (500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleUnexpected(Exception e) {
        // 스택 트레이스 전체 로깅 (운영 로그에서만 확인)
        log.error("[UnexpectedException] {}", e.getMessage(), e);
        return ResponseEntity.status(500)
            .body(ApiResponse.error("INTERNAL_ERROR",
                "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
    }
}
```

### 4.3 에러 로깅 전략

```
로그 레벨 기준:

ERROR — 처리되지 않은 예외 (500), 외부 서비스 장애
WARN  — 비즈니스 규칙 위반 (4xx), 낙관적 잠금 충돌, DB 제약 위반
INFO  — 일반 요청/응답 (AOP 필터)
DEBUG — 개발 환경 전용 상세 로그

에러 로그 포함 필드:
{
  "timestamp": "2026-05-23T10:00:00.000Z",
  "level": "ERROR",
  "traceId": "abc-123-def",        // MDC 기반 요청 추적 ID
  "userId": 1,                      // 인증된 사용자 (없으면 null)
  "method": "POST",
  "uri": "/api/trades",
  "errorCode": "TRADE_INVALID_STATUS",
  "message": "현재 상태에서는 해당 작업을 수행할 수 없습니다.",
  "stackTrace": "..."               // ERROR 레벨에서만 포함
}
```

### 4.4 WebSocket 에러 처리

```java
// WebSocket 세션 내 에러 메시지 전송
// STOMP 프레임으로 에러 알림

// 채팅 메시지 처리 중 에러 발생 시:
messagingTemplate.convertAndSendToUser(
    userId,
    "/queue/errors",
    new WsErrorResponse("CHAT_ROOM_NOT_FOUND", "존재하지 않는 채팅방입니다.")
);

// 연결 자체가 불가능한 경우 (토큰 오류):
// StompHeaderAccessor에서 HandshakeInterceptor로 처리 → HTTP 401 반환
```

---

## 5. 클라이언트 에러 처리 전략

### 5.1 Axios 인터셉터

```javascript
// api/axios.js

const api = axios.create({ baseURL: import.meta.env.VITE_API_URL })

// 요청 인터셉터 — Access Token 주입
api.interceptors.request.use(config => {
  const token = useAuthStore().accessToken
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 응답 인터셉터 — 에러 전역 처리
api.interceptors.response.use(
  response => response,
  async error => {
    const { response } = error
    if (!response) {
      // 네트워크 오류 (서버 응답 없음)
      useToast().show('error', '네트워크 연결을 확인해주세요.')
      return Promise.reject(error)
    }

    const { code, message } = response.data

    // Access Token 만료 → 자동 재발급
    if (code === 'AUTH_TOKEN_EXPIRED') {
      return handleTokenRefresh(error)
    }

    // Refresh Token 만료 → 로그인 페이지 이동
    if (code === 'AUTH_REFRESH_EXPIRED' || code === 'AUTH_REFRESH_INVALID') {
      useAuthStore().clearAuth()
      useToast().show('info', '로그인이 만료되었습니다. 다시 로그인해주세요.')
      router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
      return Promise.reject(error)
    }

    // 낙관적 잠금 → 페이지 새로고침 안내
    if (code === 'OPTIMISTIC_LOCK') {
      useToast().show('warning', message)
      return Promise.reject(error)
    }

    // 나머지 에러는 호출부에서 처리 (try/catch)
    // 전역 토스트는 호출부가 명시적으로 처리하지 않을 때만
    return Promise.reject(error)
  }
)

// 토큰 재발급 (동시 요청 큐잉)
let isRefreshing = false
let failedQueue = []

async function handleTokenRefresh(originalError) {
  if (isRefreshing) {
    // 재발급 중이면 큐에 추가하고 대기
    return new Promise((resolve, reject) => {
      failedQueue.push({ resolve, reject })
    }).then(token => {
      originalError.config.headers.Authorization = `Bearer ${token}`
      return api(originalError.config)
    })
  }

  isRefreshing = true
  try {
    const { data } = await api.post('/auth/refresh')
    const newToken = data.data.accessToken
    useAuthStore().setAccessToken(newToken)
    failedQueue.forEach(({ resolve }) => resolve(newToken))
    failedQueue = []
    originalError.config.headers.Authorization = `Bearer ${newToken}`
    return api(originalError.config)
  } catch (refreshError) {
    failedQueue.forEach(({ reject }) => reject(refreshError))
    failedQueue = []
    return Promise.reject(refreshError)
  } finally {
    isRefreshing = false
  }
}
```

### 5.2 에러 코드별 클라이언트 처리 분기

```javascript
// composables/useApiError.js

export function useApiError() {
  const toast = useToast()

  function handle(error, options = {}) {
    const code = error.response?.data?.code
    const message = error.response?.data?.message

    // 호출부에서 명시적으로 처리하고 싶은 코드 제외
    if (options.suppress?.includes(code)) return

    switch (code) {
      // 인증
      case 'AUTH_REQUIRED':
        // 인터셉터에서 처리됨 (로그인 페이지 이동)
        break

      // 사용자 친화적 토스트 표시
      case 'ITEM_BOOST_LIMIT':
      case 'AI_RATE_LIMIT':
      case 'AUTH_RESEND_TOO_SOON':
      case 'RESERVATION_CANCEL_TOO_LATE':
      case 'REVIEW_PERIOD_EXPIRED':
        toast.show('warning', message)
        break

      // 충돌 (의미 있는 피드백 필요)
      case 'SLOT_ALREADY_LOCKED':
        toast.show('info', message)
        break

      // 서버 오류
      case 'INTERNAL_ERROR':
      case 'SERVICE_UNAVAILABLE':
      case 'AI_SERVICE_UNAVAILABLE':
      case 'FILE_UPLOAD_FAILED':
        toast.show('error', message)
        break

      // 기본: 에러 토스트
      default:
        if (error.response?.status >= 500) {
          toast.show('error', '일시적인 오류가 발생했습니다.')
        }
        // 4xx는 개별 컴포넌트에서 인라인 처리 (폼 에러 등)
    }
  }

  return { handle }
}
```

### 5.3 폼 유효성 에러 처리

```javascript
// 컴포넌트에서 VALIDATION_ERROR 처리 예시
async function submitSignup() {
  try {
    await authApi.signup(form)
    router.push('/signup/verify')
  } catch (error) {
    if (error.response?.data?.code === 'VALIDATION_ERROR') {
      // 서버 유효성 에러 → 필드별 인라인 에러 표시
      const serverErrors = error.response.data.errors
      serverErrors.forEach(({ field, reason }) => {
        fieldErrors.value[field] = reason
      })
    } else if (error.response?.data?.code === 'AUTH_EMAIL_DUPLICATED') {
      fieldErrors.value.email = '이미 사용 중인 이메일입니다.'
    } else if (error.response?.data?.code === 'AUTH_NICKNAME_DUPLICATED') {
      fieldErrors.value.nickname = '이미 사용 중인 닉네임입니다.'
    } else {
      useApiError().handle(error)
    }
  }
}
```

### 5.4 WebSocket 에러 처리

```javascript
// composables/useWebSocket.js

stompClient.subscribe('/user/queue/errors', (frame) => {
  const { code, message } = JSON.parse(frame.body)
  useToast().show('error', message)

  // 치명적 에러 시 재연결 중단
  if (code === 'CHAT_ACCESS_DENIED') {
    stompClient.disconnect()
    router.push('/chats')
  }
})

// 연결 에러 (재연결 전략)
stompClient.onStompError = (frame) => {
  console.error('STOMP error:', frame)
  scheduleReconnect()
}

function scheduleReconnect() {
  // Exponential Backoff: 1s → 2s → 4s → 8s → 최대 30s
  const delay = Math.min(1000 * 2 ** retryCount.value, 30000)
  retryCount.value++
  setTimeout(connect, delay)
}
```

### 5.5 SSE 에러 처리

```javascript
// composables/useSse.js

let retryCount = 0

function connect() {
  const eventSource = new EventSource(
    `${BASE_URL}/notifications/stream`,
    { withCredentials: true }
  )

  eventSource.onerror = () => {
    eventSource.close()

    // 재연결 (Exponential Backoff)
    const delay = Math.min(1000 * 2 ** retryCount, 30000)
    retryCount++
    setTimeout(connect, delay)

    // 재연결 중 미수신 알림 폴링으로 보완
    fetchMissedNotifications()
  }

  eventSource.addEventListener('notification', (e) => {
    retryCount = 0  // 연결 성공 시 재시도 카운터 초기화
    const notification = JSON.parse(e.data)
    notificationStore.addNew(notification)
  })
}
```

---

## 6. 특수 에러 시나리오

### 6.1 슬롯 예약 Race Condition

```
시나리오: 구매자 A, B가 거의 동시에 동일 슬롯 예약 요청

T+0ms   A: SET slot:lock:20 userA NX EX 300 → 성공 (Redis 잠금 획득)
T+1ms   B: SET slot:lock:20 userB NX EX 300 → 실패 (키 이미 존재)

B에게 반환:
  HTTP 409 / SLOT_ALREADY_LOCKED
  "다른 사용자가 예약을 진행 중입니다. 잠시 후 다시 시도해주세요."

A의 잠금 만료 (5분) 후:
  슬롯 자동 해제
  B가 재시도하면 잠금 획득 가능

극단적 경우 (Redis 잠금 우회, DB 레벨):
  reservations.confirmed_slot_id UNIQUE 제약으로 중복 INSERT 차단
  → DataIntegrityViolationException → 409 CONFLICT 응답
```

### 6.2 거래 상태 동시 변경

```
시나리오: 구매자가 취소, 판매자가 수락을 거의 동시에 요청

T+0ms   판매자: SELECT trade WHERE id=50 (version=1, status=PENDING)
T+0ms   구매자: SELECT trade WHERE id=50 (version=1, status=PENDING)
T+1ms   판매자: UPDATE trade SET status=RESERVED, version=2 WHERE id=50 AND version=1 → 성공
T+2ms   구매자: UPDATE trade SET status=CANCELLED, version=2 WHERE id=50 AND version=1 → 0 rows affected

구매자 서비스에서 version 불일치 감지
→ ObjectOptimisticLockingFailureException
→ HTTP 409 / OPTIMISTIC_LOCK
  "다른 사용자가 동시에 수정했습니다. 페이지를 새로고침 후 다시 시도해주세요."

클라이언트: 새로고침 후 현재 상태(RESERVED) 확인
```

### 6.3 토큰 동시 재발급 (다중 탭)

```
시나리오: 사용자가 2개 탭을 열어두고 동시에 API 요청

탭 A: 401 AUTH_TOKEN_EXPIRED 수신 → /auth/refresh 요청
탭 B: 401 AUTH_TOKEN_EXPIRED 수신 → /auth/refresh 요청 (0.1초 후)

탭 A: 새 AccessToken, 새 RefreshToken 발급 → Redis에 새 RefreshToken 저장
탭 B: 구 RefreshToken으로 요청 → Redis에서 무효화된 토큰 → AUTH_REFRESH_INVALID

Rotation 정책 특성상 발생하는 정상 케이스.

처리:
  탭 B: AUTH_REFRESH_INVALID 수신
  → 로그인 페이지 이동
  → "로그인이 만료되었습니다. 다시 로그인해주세요." 토스트

예방 전략 (클라이언트):
  axios 인터셉터에서 동시 재발급 큐잉 (isRefreshing 플래그)
  → 탭 간에는 BroadcastChannel API로 새 토큰 공유 (Nice to Have)
```

### 6.4 AI 스트리밍 중 에러

```
시나리오: 설명 자동완성 스트리밍 중 Claude API 타임아웃

처리 순서:
  1. 스트리밍 시작 (텍스트 점진 렌더링 중)
  2. 30초 후 타임아웃 발생
  3. SSE 스트림 강제 종료
  4. 이미 렌더링된 텍스트 유지 + 에러 토스트
     "AI 생성이 중단되었습니다. 지금까지 생성된 내용을 확인해주세요."
  5. "다시 생성" 버튼 표시

부분 생성된 텍스트 처리:
  - 5줄 이상 생성됨 → 유지 + 에러 안내
  - 1줄 미만 생성됨 → 텍스트 초기화 + 에러 토스트
```

### 6.5 이미지 Presigned URL 만료

```
시나리오: Presigned URL 발급 후 5분 초과하여 업로드 시도

클라이언트: PUT {presignedUrl} → S3 403 AccessDenied (S3 직접 응답)

처리:
  S3 응답이 아닌 자체 에러로 변환하여 사용자에게 안내
  → "이미지 업로드 세션이 만료되었습니다. 이미지를 다시 선택해주세요."
  → 해당 이미지만 업로드 재시도 (전체 폼 재입력 불필요)
```

---

## 7. 사용자 노출 메시지 가이드

### 7.1 메시지 작성 원칙

```
✅ 좋은 메시지:
  - 무엇이 잘못되었는지 명확히 알려준다.
  - 사용자가 취할 수 있는 다음 행동을 안내한다.
  - 기술 용어를 사용하지 않는다.

❌ 나쁜 메시지:
  - "NullPointerException at TradeService:142"
  - "Error 409"
  - "Something went wrong"
  - "Database error occurred"
```

### 7.2 에러 → 안내 액션 매핑

| 에러 상황 | 메시지 | 제공할 액션 |
|----------|--------|------------|
| 로그인 필요 | "로그인이 필요한 서비스입니다." | [로그인하기] [회원가입] |
| 세션 만료 | "로그인이 만료되었습니다. 다시 로그인해주세요." | [로그인하기] |
| 슬롯 잠금 중 | "다른 사용자가 예약을 진행 중입니다. 잠시 후 다시 시도해주세요." | [새로고침] |
| 이메일 미인증 | "이메일 인증이 필요합니다." | [인증 메일 재발송] |
| 낙관적 잠금 | "다른 사용자가 동시에 수정했습니다." | [페이지 새로고침] |
| AI 서비스 불가 | "AI 기능이 일시적으로 이용 불가합니다. 직접 입력해주세요." | (없음, 텍스트 입력으로 유도) |
| 파일 크기 초과 | "파일 크기는 5MB 이하여야 합니다." | (없음) |
| 서버 오류 | "일시적인 오류가 발생했습니다. 잠시 후 다시 시도해주세요." | [다시 시도] |

### 7.3 보안상 의도적으로 모호하게 처리하는 케이스

```
비밀번호 재설정 요청:
  이메일 존재 여부와 무관하게 동일 메시지 반환
  → "비밀번호 재설정 메일이 발송되었습니다."
  (이유: 가입 여부 탐색 공격 방지)

로그인 실패:
  이메일 미존재 / 비밀번호 불일치 모두 동일 메시지
  → "이메일 또는 비밀번호가 올바르지 않습니다."
  (이유: 계정 존재 여부 탐색 방지)

계정 정지 / 탈퇴:
  이메일 단계가 아닌 로그인 시도 시점에 안내
  (이유: 이메일 단계에서 계정 상태 노출 방지)
```

---

> **다음 단계:** `Env Spec.md` — 환경 변수 정의, 비밀값 관리, 로컬/운영 환경 구성 가이드
