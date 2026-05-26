package com.tradev.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradev.domain.auth.service.JwtHandshakeInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${cors.allowed-origins:http://localhost:5173}")
    private String[] allowedOrigins;

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final ObjectMapper objectMapper;

    public WebSocketConfig(JwtHandshakeInterceptor jwtHandshakeInterceptor, ObjectMapper objectMapper) {
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
        this.objectMapper = objectMapper;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 클라이언트가 구독할 prefix
        registry.enableSimpleBroker("/topic", "/queue");
        // 클라이언트가 서버로 메시지 보낼 때 prefix
        registry.setApplicationDestinationPrefixes("/app");
        // 특정 사용자에게 보낼 때 prefix
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Spring Boot auto-configured ObjectMapper(JavaTimeModule 포함)를 STOMP 메시지 컨버터에 주입.
     * 기본 컨버터의 ObjectMapper는 JavaTimeModule이 없어 LocalDateTime 직렬화 실패.
     */
    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
        resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);

        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        converter.setContentTypeResolver(resolver);
        messageConverters.add(converter);
        return true; // 기본 String/ByteArray 컨버터도 유지
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
            .setAllowedOriginPatterns("*")
            .addInterceptors(jwtHandshakeInterceptor)
            .withSockJS();
    }
}
