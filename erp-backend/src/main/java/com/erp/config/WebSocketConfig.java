package com.erp.config;

import com.erp.security.CustomUserDetailsService;
import com.erp.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Kênh phát tin:
        // - /topic: Broadcast cho các phòng chat chung hoặc thông báo toàn hệ thống
        // - /queue: Tin nhắn riêng tư 1-1 giữa 2 người dùng (@SendToUser)
        config.enableSimpleBroker("/topic", "/queue");

        // Tiền tố cho các route mà Client gửi lên Server xử lý
        config.setApplicationDestinationPrefixes("/app");

        // Tiền tố cho các tin nhắn gửi đích danh tới từng User
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint kết nối WebSocket từ Client (hỗ trợ SockJS fallback)
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Cổng WebSocket tiêu chuẩn (cho các client không dùng SockJS)
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Lấy Token JWT từ Native Headers của STOMP frame CONNECT
                    String authHeader = accessor.getFirstNativeHeader("Authorization");

                    if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                        String jwt = authHeader.substring(7);

                        try {
                            if (tokenProvider.validateToken(jwt)) {
                                String username = tokenProvider.getUsernameFromJWT(jwt);
                                UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);

                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                                // Gắn Principal vào phiên làm việc WebSocket
                                accessor.setUser(authentication);
                                log.info("WebSocket STOMP: Xác thực thành công cho user: {}", username);
                            }
                        } catch (Exception ex) {
                            log.error("WebSocket STOMP: Xác thực Token thất bại: {}", ex.getMessage());
                        }
                    } else {
                        log.warn("WebSocket STOMP: Không tìm thấy header Authorization khi kết nối!");
                    }
                }

                return message;
            }
        });
    }
}

