package com.talktrip.talktrip.domain.chat.controller;

import com.talktrip.talktrip.domain.chat.dto.request.ChatRoomRequestDto;
import com.talktrip.talktrip.domain.chat.dto.response.*;
import com.talktrip.talktrip.domain.chat.service.ChatService;
import com.talktrip.talktrip.global.dto.SliceResponse;
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
        return chatService.getRooms(accountEmail);

    }
    @Operation(summary = "채팅방 메타 + (옵션) 첫 페이지 메시지")
    @GetMapping("/me/chatRooms/{roomId}")
    public ChatRoomWithMessagesDto getChatRoom(
            @PathVariable String roomId,
            @RequestParam(defaultValue = "false") boolean includeMessages,
            @RequestParam(required = false) Integer limit,
            Principal principal
    ) {
        String email = principal.getName();

        ChatRoomDetailDto room = chatService.getRoomDetail(roomId, email);

        if (!includeMessages) {
            return new ChatRoomWithMessagesDto(room, null);
        }

        int size = (limit == null || limit <= 0 || limit > 200) ? 50 : limit;
        var slice = chatService.getRoomChattingHistoryAndMarkAsRead(roomId, email, size, null);

        return new ChatRoomWithMessagesDto(room, slice);
    }
    @GetMapping("/me/chatRooms/{roomId}/messages")
    public SliceResponse<ChatMemberRoomWithMessageDto> getRoomMessages(
            @PathVariable String roomId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String cursor,
            Principal principal
    ) {
        return chatService.getRoomChattingHistoryAndMarkAsRead(
                roomId,
                principal.getName(), // 로그인 유저 이메일
                limit,
                cursor
        );
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
