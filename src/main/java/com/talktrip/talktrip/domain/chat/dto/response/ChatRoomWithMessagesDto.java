package com.talktrip.talktrip.domain.chat.dto.response;


import com.talktrip.talktrip.global.dto.SliceResponse;

public record ChatRoomWithMessagesDto(
        ChatRoomDetailDto room,
        SliceResponse<ChatMemberRoomWithMessageDto> messages // includeMessages=false면 null 가능
) {

}