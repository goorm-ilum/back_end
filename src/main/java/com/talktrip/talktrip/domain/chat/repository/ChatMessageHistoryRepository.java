package com.talktrip.talktrip.domain.chat.repository;

import com.talktrip.talktrip.domain.chat.entity.ChatMessageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageHistoryRepository extends JpaRepository<ChatMessageHistory, Long> {
    List<ChatMessageHistory> findByRoomIdOrderByCreatedAt(String roomId);
}
