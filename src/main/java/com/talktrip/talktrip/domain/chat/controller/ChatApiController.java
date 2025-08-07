package com.talktrip.talktrip.domain.chat.controller;

import com.talktrip.talktrip.domain.chat.dto.request.ChatRoomRequestDto;
import com.talktrip.talktrip.domain.chat.dto.response.ChatRoomResponseDto;
import com.talktrip.talktrip.domain.chat.entity.ChatMessage;
import com.talktrip.talktrip.domain.chat.entity.ChatRoom;
import com.talktrip.talktrip.domain.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Chat", description = "채팅 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatApiController {

    private final ChatService chatService;

    @Operation(summary = "채팅방 접속")
    @PostMapping
    public void enterChatRoom() {}

    @Operation(summary = "채팅 입력")
    @PostMapping("/input")
    public void sendMessage() {}

    @Operation(summary = "내 채팅 목록")
    @GetMapping("/me/chatRooms")
    public List<ChatRoom> getMyChats() {
        // 실제 데이터베이스 조회는 나중에 활성화
         List<ChatRoom> rooms = chatService.getRooms("dhrdbs");
         return rooms;
    }
    @Operation(summary = "채팅방 상세 조회")
    @GetMapping("/me/chatRooms/{roomId}")
    public List<ChatMessage> getChatRoom(@PathVariable String roomId) {
        return chatService.getRoomChattingHistoryAndMarkAsRead(roomId,"dhrdbs");
    }

    @Operation(summary = "안읽은 채팅방 갯수")
    @GetMapping("/countALLUnreadMessagesRooms")
    public int getCountALLUnreadMessagesRooms(String userId) {
        return chatService.getCountALLUnreadMessagesRooms("dhrdbs");
    }


    @Operation(summary = "안읽은 모든 채팅갯수")
    @GetMapping("/countALLUnreadMessages")
    public Map<String, Integer> getCountAllUnreadMessages(@RequestParam String userId) {
        int count = chatService.getCountAllUnreadMessages("dhrdbs");
        return Map.of("count", count);
    }
    @Operation(summary = "채팅방 입장 또는 생성")
    @PostMapping("/rooms/enter")
    public ResponseEntity<ChatRoomResponseDto> enterOrCreateRoom(@RequestBody ChatRoomRequestDto request) {
        String roomId = chatService.enterOrCreateRoom("dhrdbs", request.getSellerId());
        return ResponseEntity.ok(new ChatRoomResponseDto(roomId));
    }
}
