package com.e205.command.item.command;

import lombok.Builder;

@Builder
public record UpdateItemCommand(int memberId, int itemId, String emoticon, String name, byte colorKey) {

}
