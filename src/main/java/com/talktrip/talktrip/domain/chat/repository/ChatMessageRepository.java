package com.talktrip.talktrip.domain.chat.repository;

import com.talktrip.talktrip.domain.chat.dto.request.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByRoomIdOrderByCreatedAt(String roomId);
}
