package com.e205.service;

import com.e205.payload.ItemImagePayload;
import com.e205.payload.LostItemPayload;
import com.e205.query.LostItemListQuery;
import com.e205.query.LostItemQuery;
import java.util.List;

public interface LostItemQueryService {

  LostItemPayload find(LostItemQuery query);

  List<LostItemPayload> find(LostItemListQuery query);

  List<ItemImagePayload> findImages(Integer lostId);

  boolean isCreatable(Integer memberId);
}
