package com.example.task_flow_backend.realtime;

import com.example.task_flow_backend.auth.JwtService;
import com.example.task_flow_backend.auth.TokenType;
import com.example.task_flow_backend.auth.UserPrincipal;
import com.example.task_flow_backend.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Authenticates the STOMP {@code CONNECT} frame from the {@code Authorization:
 * Bearer <token>} STOMP header. An unauthenticated socket would leak every
 * board, so a missing/invalid token here fails the connection outright.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public StompAuthChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Missing bearer token on STOMP CONNECT");
        }

        try {
            Claims claims = jwtService.parse(header.substring(BEARER_PREFIX.length()));
            if (!TokenType.ACCESS.name().equals(claims.get(JwtService.CLAIM_TYPE, String.class))) {
                throw new IllegalArgumentException("STOMP CONNECT requires an access token");
            }
            UserPrincipal principal = new UserPrincipal(
                    Long.valueOf(claims.getSubject()),
                    claims.get(JwtService.CLAIM_EMAIL, String.class),
                    Role.valueOf(claims.get(JwtService.CLAIM_ROLE, String.class)));

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + principal.role().name()));
            var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
            accessor.setUser(authentication);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid STOMP credentials", ex);
        }
        return message;
    }
}
