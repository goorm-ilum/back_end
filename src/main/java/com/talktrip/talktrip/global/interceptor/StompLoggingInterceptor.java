package com.talktrip.talktrip.global.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StompLoggingInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        String sessionId = accessor.getSessionId();
        String simpDestination = accessor.getDestination(); // /topic/chat/room/ROOM1 등
        String command = accessor.getCommand() != null ? accessor.getCommand().name() : "UNKNOWN";

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            log.info("📡 SUBSCRIBE 요청: sessionId={}, destination={}, user={}", sessionId, simpDestination, accessor.getUser());
        } else if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            log.info("🔌 CONNECT 요청: sessionId={}, headers={}", sessionId, accessor.toNativeHeaderMap());
        } else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            log.info("❌ DISCONNECT 요청: sessionId={}", sessionId);
        }

        return message;
    }
}
