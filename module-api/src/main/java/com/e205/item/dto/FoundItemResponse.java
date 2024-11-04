package com.e205.item.dto;

import com.e205.FoundItemType;
import com.e205.payload.FoundItemPayload;
import lombok.Builder;

@Builder
public record FoundItemResponse(
    Integer id,
    Integer memberId,
    Point foundLocation,
    String description,
    String savePoint,
    FoundItemType type,
    String image
) {

  public static FoundItemResponse from(FoundItemPayload payload, String image) {
    return builder()
        .id(payload.id())
        .memberId(payload.memberId())
        .foundLocation(new Point(payload.lat(), payload.lon()))
        .description(payload.description())
        .savePoint(payload.savePlace())
        .type(payload.type())
        .image(image)
        .build();
  }
}
