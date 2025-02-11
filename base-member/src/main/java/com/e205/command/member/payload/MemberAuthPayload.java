package com.e205.command.member.payload;

public record MemberAuthPayload(
    Integer id,
    Integer bagId,
    String nickname,
    String email,
    String password,
    EmailStatus status,
    MemberStatus memberStatus
)  {

}
