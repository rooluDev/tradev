package com.tradev.common.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Vue SPA 라우팅 지원을 위한 폴백 컨트롤러.
 * /api/, /ws/, /actuator/, /admin/, /assets/ 이외의 모든 GET 요청에 대해
 * index.html을 반환하여 클라이언트 사이드 라우팅이 동작하도록 한다.
 *
 * 주의: 점(.)이 포함된 경로(index.html, style.css 등)는 정적 리소스로 처리하기 위해
 * regex에서 제외 → forward 무한 루프 방지.
 */
@Controller
public class SpaForwardController {

    // 점 없는 단일 세그먼트 경로 (예: /login, /trades, /oauth2)
    @GetMapping(value = "/{path:[^\\.]*}")
    public String forwardRoot() {
        return "forward:/index.html";
    }

    // 점 없는 첫 세그먼트 + 하위 경로 (예: /oauth2/callback, /items/123/edit)
    @GetMapping(value = "/{path:[^\\.]*}/**")
    public String forwardNested() {
        return "forward:/index.html";
    }
}
