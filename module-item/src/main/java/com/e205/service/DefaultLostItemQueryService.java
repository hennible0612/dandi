package com.e205.service;

import com.e205.entity.LostItem;
import com.e205.entity.LostItemAuth;
import com.e205.event.LostItemReadEvent;
import com.e205.message.ItemEventPublisher;
import com.e205.payload.ItemImagePayload;
import com.e205.payload.LostItemPayload;
import com.e205.query.LostItemQuery;
import com.e205.query.MembersInRouteQuery;
import com.e205.repository.ItemImageRepository;
import com.e205.repository.LostItemAuthRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional
@Service
public class DefaultLostItemQueryService implements LostItemQueryService {

  private final LostItemAuthRepository lostItemAuthRepository;
  private final RouteQueryService routeQueryService;
  private final ItemImageRepository itemImageRepository;
  private final ItemEventPublisher itemEventPublisher;

  @Override
  public LostItemPayload find(LostItemQuery query) {
    LostItemAuth lostItemAuth = getLostItemAuth(query);

    LostItem lostItem = lostItemAuth.getLostItem();

    if (lostItem.isEnded()) {
      throw new RuntimeException("종료된 분실물입니다.");
    }

    if (!lostItemAuth.isRead() && !isReadablePosition(query.memberId(), lostItem)) {
      throw new RuntimeException("분실물을 조회할 수 있는 범위를 벗어났습니다.");
    }

    if (!lostItemAuth.isRead()) {
      lostItemAuth.read();
      itemEventPublisher.publish(new LostItemReadEvent(lostItem.getId(), LocalDateTime.now()));
    }
    return lostItem.toPayload();
  }

  @Override
  public List<ItemImagePayload> findImages(Integer lostId) {
    return itemImageRepository.findAllByLostItemId(lostId).stream()
        .map(image -> new ItemImagePayload(image.getName()))
        .toList();
  }

  private LostItemAuth getLostItemAuth(LostItemQuery query) {
    return lostItemAuthRepository.findLostItemAuthByMemberIdAndLostItemId(query.memberId(),
        query.lostItemId()).orElseThrow(() -> new RuntimeException("분실물 조회 권한이 없습니다."));
  }

  private boolean isReadablePosition(Integer memberId, LostItem lostItem) {
    MembersInRouteQuery query = new MembersInRouteQuery(lostItem.getMemberId(),
        lostItem.getStartRouteId(), lostItem.getEndRouteId());

    List<Integer> memberList = routeQueryService.findUserIdsNearPath(query);
    return memberList.contains(memberId);
  }
}
