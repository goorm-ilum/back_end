package com.talktrip.talktrip.domain.chat.controller;

import com.talktrip.talktrip.domain.chat.dto.request.ChatRoomRequestDto;
import com.talktrip.talktrip.domain.chat.dto.response.ChatRoomDTO;
import com.talktrip.talktrip.domain.chat.dto.response.ChatRoomResponseDto;
import com.talktrip.talktrip.domain.chat.entity.ChatMessage;
import com.talktrip.talktrip.domain.chat.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
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


    @Operation(summary = "내 채팅 목록")
    @GetMapping("/me/chatRooms")
    public List<ChatRoomDTO> getMyChats(Principal principal) {
        String accountEmail = principal.getName();
        // 실제 데이터베이스 조회는 나중에 활성화
         List<ChatRoomDTO> rooms = chatService.getRooms(accountEmail);
         return rooms;
    }
    @Operation(summary = "채팅방 상세 조회")
    @GetMapping("/me/chatRooms/{roomId}")
    public List<ChatMessage> getChatRoom(@PathVariable String roomId,Principal principal) {
        String accountEmail = principal.getName();
        return chatService.getRoomChattingHistoryAndMarkAsRead(roomId,accountEmail);
    }

    @Operation(summary = "안읽은 채팅방 갯수")
    @GetMapping("/countALLUnreadMessagesRooms")
    public int getCountALLUnreadMessagesRooms(String userId,Principal principal) {
        String accountEmail = principal.getName();
        return chatService.getCountALLUnreadMessagesRooms(accountEmail);
    }


    @Operation(summary = "안읽은 모든 채팅갯수")
    @GetMapping("/countALLUnreadMessages")
    public Map<String, Integer> getCountAllUnreadMessages(Principal principal) {
        int count = chatService.getCountAllUnreadMessages(principal.getName());//실시간으로 나오게
        return Map.of("count", count);
    }
    @Operation(summary = "채팅방 입장 또는 생성")
    @PostMapping("/rooms/enter")
    public ResponseEntity<ChatRoomResponseDto> enterOrCreateRoom(Principal principal,
                                                                 @RequestBody ChatRoomRequestDto chatRoomRequestDto) {
        String roomId = chatService.enterOrCreateRoom(principal,chatRoomRequestDto);
        return ResponseEntity.ok(new ChatRoomResponseDto(roomId));
    }
    @Operation(summary = "채팅방 나가기(삭제 처리)")
    @PatchMapping("/me/chatRooms/{roomId}")
    public ResponseEntity<Void> leaveChatRoom(Principal principal,@PathVariable String roomId) {
        chatService.markChatRoomAsDeleted(principal.getName(), roomId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

}
