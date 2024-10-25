package com.e205.service;

import com.e205.dto.LostItemSaveCommand;
import com.e205.entity.LostItem;
import com.e205.events.EventPublisher;
import com.e205.message.LostItemSaveEvent;
import com.e205.repository.LostItemRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class DefaultLostItemService implements LostItemService {

  private static final int MAX_IMAGE_COUNT = 3;
  private static final int LOST_ITEM_COOL_TIME = 24;

  private final LostItemRepository lostItemRepository;
  private final EventPublisher eventPublisher;

  @Transactional
  @Override
  public void save(LostItemSaveCommand command) {
    if (command.images().size() > MAX_IMAGE_COUNT) {
      // TODO <fosong98> 예외 구체화 필요
      throw new RuntimeException("최대 이미지 개수를 초과했을 때");
    }

    lostItemRepository.findFirstByMemberIdOrderByCreatedAtDesc(command.lostMemberId())
        .filter(recent -> recent.getCreatedAt()
            .isAfter(LocalDateTime.now().minusHours(LOST_ITEM_COOL_TIME)))
        .ifPresent(recent -> {
          // TODO <fosong98> 예외 구체화 필요
          throw new RuntimeException("제한 시간 내에 재등록했을 때");
        });

    LostItem lostItem = command.toEntity();
    lostItemRepository.save(lostItem);
    eventPublisher.publish(new LostItemSaveEvent(lostItem, LocalDateTime.now()));
  }
}
