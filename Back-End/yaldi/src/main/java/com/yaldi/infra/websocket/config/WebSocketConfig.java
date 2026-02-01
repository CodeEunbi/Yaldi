package com.yaldi.infra.websocket.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaldi.infra.websocket.interceptor.WebSocketConnectionInterceptor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketConnectionInterceptor connectionInterceptor;

    @PostConstruct
    public void init() {
    }
    // 클라이언트가 연결할 WebSocket endpoint 설정
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // SockJS fallback
    }

    //메세지 라우팅 경로
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 서버 -> 클라이언트 구독 경로 prefix
        registry.enableSimpleBroker("/topic");
        // 클라이언트 -> 서버 publish prefix
        registry.setApplicationDestinationPrefixes("/pub");
    }

    // Interceptor 등록 (Connect/Disconnect 감지)
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(connectionInterceptor);
    }

    // Jackson 메시지 컨버터 설정 (JSON → BigDecimal 변환 처리)
    @Override
    public boolean configureMessageConverters(List<MessageConverter> converters) {
        ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json()
                // 실수도 BigDecimal로 변환 (정수는 기본적으로 변환됨)
                .featuresToEnable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .build();

        MappingJackson2MessageConverter jacksonConverter = new MappingJackson2MessageConverter();
        jacksonConverter.setObjectMapper(objectMapper);

        converters.add(jacksonConverter);
        // false: 기본 컨버터 + 추가한 컨버터 모두 사용
        return false;
    }
}
