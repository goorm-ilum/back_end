package com.talktrip.talktrip.domain.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Chat", description = "채팅 관련 API")
@RestController
@RequestMapping("/api/chat")
public class ChatApiController {

    @Operation(summary = "채팅방 접속")
    @PostMapping
    public void enterChatRoom() {}

    @Operation(summary = "채팅 입력")
    @PostMapping("/input")
    public void sendMessage() {}

    @Operation(summary = "내 채팅 목록")
    @GetMapping("/me/chats")
    public void getMyChats() {}

    @Operation(summary = "내 채팅 생성")
    @PostMapping("room")
    public void getChatRooms() {}




}
