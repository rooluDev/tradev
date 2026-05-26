package com.tradev.domain.auth.service;

import com.tradev.domain.user.entity.OAuthProvider;
import com.tradev.domain.user.entity.User;
import com.tradev.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String providerId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        User user = userRepository.findByOauthProviderAndOauthProviderId(OAuthProvider.GOOGLE, providerId)
                .orElseGet(() -> userRepository.findByEmail(email)
                        .map(existing -> {
                            // 기존 이메일 가입 사용자에 OAuth 연동
                            return existing;
                        })
                        .orElseGet(() -> {
                            // 신규 사용자 생성
                            String nickname = generateNickname(name);
                            return userRepository.save(User.builder()
                                    .email(email)
                                    .nickname(nickname)
                                    .oauthProvider(OAuthProvider.GOOGLE)
                                    .oauthProviderId(providerId)
                                    .emailVerified(true)
                                    .build());
                        })
                );

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("userId", user.getId());
        attributes.put("sub", providerId);
        attributes.put("email", email);
        attributes.put("role", user.getRole().name());

        return new DefaultOAuth2User(
                oAuth2User.getAuthorities(),
                attributes,
                "sub"
        );
    }

    private String generateNickname(String name) {
        String base = name != null ? name.replaceAll("[^가-힣a-zA-Z0-9]", "") : "user";
        if (base.length() < 2) base = "user" + base;
        String candidate = base.length() > 10 ? base.substring(0, 10) : base;

        int suffix = 1;
        while (userRepository.existsByNickname(candidate)) {
            candidate = base.substring(0, Math.min(base.length(), 8)) + suffix++;
        }
        return candidate;
    }
}
