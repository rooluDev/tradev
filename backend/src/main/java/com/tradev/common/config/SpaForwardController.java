package com.tradev.common.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Vue SPA 라우팅 지원을 위한 폴백 컨트롤러.
 * /api/, /ws/, /actuator/, /admin/ 이외의 모든 GET 요청에 대해
 * index.html을 반환하여 클라이언트 사이드 라우팅이 동작하도록 한다.
 */
@Controller
public class SpaForwardController {

    // 최상위 단일 세그먼트 경로 (예: /oauth2, /login, /trades)
    @GetMapping(value = {
            "/{path:^(?!api|ws|actuator|admin|assets).*$}"
    })
    public String forwardRoot() {
        return "forward:/index.html";
    }

    // 두 번째 세그먼트까지 (예: /oauth2/callback, /items/123)
    @GetMapping(value = {
            "/{path:^(?!api|ws|actuator|admin|assets).*$}/**"
    })
    public String forwardNested() {
        return "forward:/index.html";
    }
}
